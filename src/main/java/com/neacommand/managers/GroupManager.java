package com.neacommand.managers;

import com.neacommand.NeaCommand;
import com.neacommand.utils.SchedulerUtils;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.group.GroupDataRecalculateEvent;
import net.luckperms.api.event.node.NodeMutateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class GroupManager {

    private final NeaCommand plugin;
    private File groupsFile;
    private FileConfiguration groupsConfig;
    private LuckPerms luckPerms;

    // GroupName -> Set of allowed command entries (e.g. "help", "warp spawn")
    private final Map<String, Set<String>> groupWhitelists = new ConcurrentHashMap<>();
    // GroupName -> Set of base commands that should be hidden from TAB
    private final Map<String, Set<String>> groupHiddenCommands = new ConcurrentHashMap<>();
    private final Set<String> globalWhitelist = ConcurrentHashMap.newKeySet();

    // Player UUID -> Cached allowed entries for execution
    private final Map<UUID, Set<String>> userCache = new ConcurrentHashMap<>();
    // Player UUID -> Cached base commands for TAB completion
    private final Map<UUID, Set<String>> userTabCache = new ConcurrentHashMap<>();

    public GroupManager(NeaCommand plugin) {
        this.plugin = plugin;
        setupLuckPerms();
        loadGroups();
        setupLuckPermsEventListener();
    }

    private void setupLuckPerms() {
        if (Bukkit.getServer() == null) return;
        var provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPerms = provider.getProvider();
        } else {
            plugin.getLogger().severe("LuckPerms API was not found! NeaCommand requires LuckPerms.");
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    private void setupLuckPermsEventListener() {
        if (Bukkit.getServer() == null || luckPerms == null) return;

        // 1. Invalidate single user cache when user data recalculates
        luckPerms.getEventBus().subscribe(plugin, UserDataRecalculateEvent.class, event -> {
            UUID uuid = event.getUser().getUniqueId();
            invalidateCache(uuid);

            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                SchedulerUtils.runForPlayerLater(plugin, player, player::updateCommands, 5L);
            }
        });

        // 2. Invalidate all users when group inheritance or data changes
        luckPerms.getEventBus().subscribe(plugin, GroupDataRecalculateEvent.class, event -> {
            invalidateAllCache();
            updateAllPlayersCommands();
        });

        // 3. Invalidate on node additions/removals
        luckPerms.getEventBus().subscribe(plugin, NodeMutateEvent.class, event -> {
            if (event.isGroup()) {
                invalidateAllCache();
                updateAllPlayersCommands();
            } else if (event.isUser() && event.getTarget() instanceof User user) {
                UUID uuid = user.getUniqueId();
                invalidateCache(uuid);
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    SchedulerUtils.runForPlayerLater(plugin, player, player::updateCommands, 5L);
                }
            }
        });
    }

    public void loadGroups() {
        if (plugin == null || plugin.getDataFolder() == null) return;
        groupsFile = new File(plugin.getDataFolder(), "groups.yml");
        if (!groupsFile.exists()) {
            plugin.saveResource("groups.yml", false);
        }
        groupsConfig = YamlConfiguration.loadConfiguration(groupsFile);

        groupWhitelists.clear();
        groupHiddenCommands.clear();
        globalWhitelist.clear();

        // 1. Load Global Whitelist
        List<String> globals = plugin.getConfig().getStringList("global-whitelist");
        for (String g : globals) {
            String clean = normalizeCommandString(g);
            if (!clean.isEmpty()) {
                globalWhitelist.add(clean);
            }
        }
        globalWhitelist.add("neacommand");
        globalWhitelist.add("nc");
        globalWhitelist.add("nea");

        // 2. Load Group Rules
        ConfigurationSection section = groupsConfig.getConfigurationSection("groups");
        if (section != null) {
            for (String groupName : section.getKeys(false)) {
                List<String> rawCmds = groupsConfig.getStringList("groups." + groupName + ".commands");
                Set<String> allowedSet = new HashSet<>();
                Set<String> hiddenSet = new HashSet<>();

                for (String raw : rawCmds) {
                    boolean isHidden = raw.toLowerCase().endsWith(":hidden");
                    String clean = normalizeCommandString(raw);
                    if (clean.isEmpty()) continue;

                    allowedSet.add(clean);
                    if (isHidden) {
                        // Mark the base command as hidden from TAB
                        hiddenSet.add(extractBaseCommand(clean));
                    }
                }
                groupWhitelists.put(groupName.toLowerCase(), Set.copyOf(allowedSet));
                groupHiddenCommands.put(groupName.toLowerCase(), Set.copyOf(hiddenSet));
            }
        }

        invalidateAllCache();
    }

    public void syncWithLuckPerms() {
        if (luckPerms == null) return;

        boolean modified = false;
        Set<Group> lpGroups = luckPerms.getGroupManager().getLoadedGroups();

        for (Group group : lpGroups) {
            String name = group.getName().toLowerCase();
            String path = "groups." + name;

            if (!groupsConfig.contains(path)) {
                groupsConfig.set(path + ".commands", new ArrayList<String>());
                modified = true;
            }

            List<String> inherits = group.getNodes().stream()
                .filter(node -> node instanceof InheritanceNode)
                .map(node -> ((InheritanceNode) node).getGroupName())
                .collect(Collectors.toList());

            groupsConfig.set(path + ".inherited-from-lp", inherits);
            modified = true;
        }

        if (modified) {
            saveGroups();
            loadGroups();
        }
    }

    public void saveGroups() {
        try {
            groupsConfig.save(groupsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save groups.yml: " + e.getMessage());
        }
    }

    public void invalidateCache(UUID uuid) {
        userCache.remove(uuid);
        userTabCache.remove(uuid);
    }

    public void invalidateAllCache() {
        userCache.clear();
        userTabCache.clear();
    }

    public boolean hasBypass(Player player) {
        if (player.hasPermission("neacommand.bypass")) return true;
        return plugin.getConfig().getBoolean("bypass-op", true) && player.isOp();
    }

    public boolean isCommandAllowed(Player player, String rawMessage) {
        if (hasBypass(player)) return true;

        String cleanLine = normalizeCommandString(rawMessage);
        if (cleanLine.isEmpty()) return false;

        String rawBase = cleanLine.split(" ")[0];
        boolean hasNamespace = rawBase.contains(":");
        String namespace = null;
        String baseCmd = rawBase;

        if (hasNamespace) {
            String[] parts = rawBase.split(":", 2);
            namespace = parts[0];
            baseCmd = parts.length > 1 ? parts[1] : "";
        }

        // 1. Check Global Whitelist
        if (globalWhitelist.contains(rawBase) || globalWhitelist.contains(cleanLine)) {
            return true;
        }
        if (!hasNamespace && globalWhitelist.contains(baseCmd)) {
            return true;
        }

        // 2. Fetch allowed entries for player
        Set<String> allowedEntries = userCache.computeIfAbsent(
            player.getUniqueId(),
            k -> calculateAllowedCommands(player, false)
        );

        if (allowedEntries.contains("*")) return true;

        // Strict namespace check
        boolean blockUnregisteredNamespaces = plugin.getConfig().getBoolean("block-unregistered-namespaces", true);
        if (hasNamespace && blockUnregisteredNamespaces) {
            // Only allow if the exact namespaced command or namespace wildcard is present
            if (allowedEntries.contains(rawBase) || allowedEntries.contains(namespace + ":*")) {
                return true;
            }
            for (String entry : allowedEntries) {
                if (entry.contains(" ") && (cleanLine.equals(entry) || cleanLine.startsWith(entry + " "))) {
                    return true;
                }
            }
            return false;
        }

        // 3. Normal base command check
        if (allowedEntries.contains(baseCmd)) {
            return true;
        }

        // 4. Check if there is an exact subcommand match (e.g., "warp spawn")
        for (String entry : allowedEntries) {
            if (entry.contains(" ")) {
                if (cleanLine.equals(entry) || cleanLine.startsWith(entry + " ")) {
                    return true;
                }
            }
        }

        return false;
    }

    public Set<String> getAllowedCommandsForTab(Player player) {
        if (hasBypass(player)) return null;

        Set<String> allowed = userTabCache.computeIfAbsent(
            player.getUniqueId(),
            k -> calculateAllowedCommands(player, true)
        );

        if (allowed != null && allowed.contains("*")) return null;

        return allowed;
    }

    /**
     * Resolves all root base command names that must be visible in the client's
     * Brigadier command tree (including root commands of allowed subcommands).
     */
    public Set<String> getAllowedBaseRootsForBrigadier(Player player) {
        Set<String> allowed = getAllowedCommandsForTab(player);
        if (allowed == null) return null;

        Set<String> roots = new HashSet<>();
        for (String cmd : allowed) {
            if (cmd.equals("*")) return null;
            String base = extractBaseCommand(cmd);
            if (!base.isEmpty()) {
                roots.add(base);
            }
        }
        return roots;
    }

    private Set<String> calculateAllowedCommands(Player player, boolean filterHidden) {
        Set<String> result = new HashSet<>(globalWhitelist);
        if (luckPerms == null) return result;

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return result;
        }

        Collection<Group> userGroups = user.getInheritedGroups(user.getQueryOptions());

        Set<String> allAllowed = new HashSet<>();
        Set<String> allHidden = new HashSet<>();

        for (Group group : userGroups) {
            String gName = group.getName().toLowerCase();

            Set<String> gAllowed = groupWhitelists.get(gName);
            if (gAllowed != null) allAllowed.addAll(gAllowed);

            Set<String> gHidden = groupHiddenCommands.get(gName);
            if (gHidden != null) allHidden.addAll(gHidden);
        }

        if (filterHidden) {
            // Remove hidden base commands from TAB suggestions
            allAllowed.removeIf(cmd -> allHidden.contains(extractBaseCommand(cmd)));
        }

        result.addAll(allAllowed);
        return result;
    }

    public boolean addCommand(String groupName, String command, boolean hidden) {
        groupName = groupName.toLowerCase();
        List<String> list = groupsConfig.getStringList("groups." + groupName + ".commands");
        boolean isHidden = hidden || command.toLowerCase().endsWith(":hidden");
        String clean = normalizeCommandString(command);

        list.removeIf(s -> normalizeCommandString(s).equalsIgnoreCase(clean));

        String entry = "/" + clean + (isHidden ? ":hidden" : "");
        list.add(entry);

        groupsConfig.set("groups." + groupName + ".commands", list);
        saveGroups();
        loadGroups();
        updateAllPlayersCommands();
        return true;
    }

    public boolean removeCommand(String groupName, String command) {
        groupName = groupName.toLowerCase();
        if (!groupsConfig.contains("groups." + groupName)) return false;

        List<String> list = groupsConfig.getStringList("groups." + groupName + ".commands");
        String clean = normalizeCommandString(command);

        boolean removed = list.removeIf(s -> normalizeCommandString(s).equalsIgnoreCase(clean));

        if (removed) {
            groupsConfig.set("groups." + groupName + ".commands", list);
            saveGroups();
            loadGroups();
            updateAllPlayersCommands();
            return true;
        }
        return false;
    }

    public void updateAllPlayersCommands() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            SchedulerUtils.runForPlayer(plugin, player, player::updateCommands);
        }
    }

    public List<String> getCommands(String groupName) {
        groupName = groupName.toLowerCase();
        return groupsConfig.getStringList("groups." + groupName + ".commands");
    }

    public List<String> getConfiguredGroups() {
        ConfigurationSection sec = groupsConfig.getConfigurationSection("groups");
        return (sec == null) ? new ArrayList<>() : new ArrayList<>(sec.getKeys(false));
    }

    public String getPlayerPrimaryGroup(Player player) {
        if (luckPerms == null) return "default";
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        return (user != null) ? user.getPrimaryGroup() : "default";
    }

    public List<String> getPlayerInheritedGroups(Player player) {
        if (luckPerms == null) return Collections.singletonList("default");
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return Collections.singletonList("default");

        return user.getInheritedGroups(user.getQueryOptions()).stream()
            .map(Group::getName)
            .collect(Collectors.toList());
    }

    public static String extractBaseCommand(String cmd) {
        if (cmd == null || cmd.isEmpty()) return "";
        String clean = normalizeCommandString(cmd);
        String base = clean.split(" ")[0];

        if (base.contains(":")) {
            String[] parts = base.split(":");
            if (parts.length > 1) base = parts[1];
        }
        return base;
    }

    public static String normalizeCommandString(String cmd) {
        if (cmd == null) return "";
        String clean = cmd.trim().toLowerCase();
        clean = clean.replace(":hidden", "").trim();
        if (clean.startsWith("/")) clean = clean.substring(1).trim();
        return clean;
    }
}
