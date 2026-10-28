package com.neacommand.listeners;

import com.neacommand.NeaCommand;
import com.neacommand.utils.SchedulerUtils;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.TabCompleteEvent;

public class CommandListener implements Listener {

    private final NeaCommand plugin;
    // Rate limit cooldowns to prevent staff chat flooding on command spam
    private final Map<UUID, Long> adminNotificationCooldowns = new ConcurrentHashMap<>();

    public CommandListener(NeaCommand plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGroupManager().hasBypass(player)) return;

        String message = event.getMessage();

        if (!plugin.getGroupManager().isCommandAllowed(player, message)) {
            event.setCancelled(true);

            String commandName = message.split(" ")[0];
            if (plugin.getConfig().getBoolean("debug-mode", false)) {
                plugin.getLogger().info("[DEBUG] Blocked command for " + player.getName() + ": " + message);
            }

            plugin.getTranslationManager().sendMessage(
                player,
                "command-blocked",
                Placeholder.parsed("command", commandName)
            );

            // Subtle audio feedback if enabled
            if (plugin.getConfig().getBoolean("sounds.blocked.enabled", true)) {
                playBlockedSound(player);
            }

            logBlockedCommand(player, message);

            notifyAdmins(player, commandName);
        }
    }

    private void logBlockedCommand(Player player, String command) {
        if (!plugin.getConfig().getBoolean("logging.blocked-commands.enabled", true)) return;

        SchedulerUtils.runGlobal(plugin, () -> {
            try {
                java.io.File logDir = new java.io.File(plugin.getDataFolder(), "logs");
                if (!logDir.exists()) logDir.mkdirs();
                java.io.File logFile = new java.io.File(logDir, "blocked_commands.log");
                String timestamp = java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                );
                String entry = String.format("[%s] [SECURITY] Player: %s (%s) | World: %s | Command: %s%n",
                    timestamp, player.getName(), player.getUniqueId(), player.getWorld().getName(), command);
                java.nio.file.Files.writeString(
                    logFile.toPath(),
                    entry,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
                );
            } catch (Exception ignored) {}
        });
    }

    private void playBlockedSound(Player player) {
        String soundKey = plugin.getConfig().getString("sounds.blocked.sound", "ENTITY_VILLAGER_NO");
        float volume = (float) plugin.getConfig().getDouble("sounds.blocked.volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("sounds.blocked.pitch", 1.0);

        try {
            org.bukkit.Sound bukkitSound = org.bukkit.Sound.valueOf(soundKey.toUpperCase());
            player.playSound(player.getLocation(), bukkitSound, volume, pitch);
        } catch (IllegalArgumentException e) {
            try {
                String normalized = soundKey.toLowerCase().replace("_", ".");
                if (!normalized.contains(":")) {
                    normalized = "minecraft:" + normalized;
                }
                player.playSound(Sound.sound(
                    Key.key(normalized),
                    Sound.Source.MASTER,
                    volume,
                    pitch
                ));
            } catch (Exception ignored) {
                // Silently skip if invalid sound key
            }
        }
    }

    private void notifyAdmins(Player player, String command) {
        long cooldownSeconds = plugin.getConfig().getLong("admin-notify-cooldown-seconds", 3L);
        long cooldownMillis = cooldownSeconds * 1000L;
        long now = System.currentTimeMillis();

        Long lastNotified = adminNotificationCooldowns.get(player.getUniqueId());
        if (lastNotified != null && (now - lastNotified) < cooldownMillis) {
            // Rate limit active: suppress notification to prevent chat spam
            return;
        }
        adminNotificationCooldowns.put(player.getUniqueId(), now);

        String group = plugin.getGroupManager().getPlayerPrimaryGroup(player);
        Component alert = plugin.getTranslationManager().getComponent(
            "admin-notify",
            Placeholder.parsed("player", player.getName()),
            Placeholder.parsed("command", command),
            Placeholder.parsed("group", group)
        );

        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission("neacommand.admin.notify")) {
                admin.sendMessage(alert);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player player)) return;
        if (plugin.getGroupManager().hasBypass(player)) return;

        String buffer = event.getBuffer();
        if (buffer.lastIndexOf(' ') > 0) return;

        Set<String> allowedSet = plugin.getGroupManager().getAllowedCommandsForTab(player);
        if (allowedSet == null) return;

        List<String> suggestions = event.getCompletions();

        suggestions.removeIf(suggestion -> {
            String check = suggestion.startsWith("/") ? suggestion.substring(1) : suggestion;
            check = check.toLowerCase();

            // Filter out namespaces unless explicitly specified
            if (check.contains(":") && !allowedSet.contains(check)) {
                return true;
            }

            String base = check;
            if (check.contains(":")) {
                String[] parts = check.split(":");
                if (parts.length > 1) base = parts[1];
            }

            return !allowedSet.contains(base);
        });

        String search = buffer.startsWith("/") ? buffer.substring(1).toLowerCase() : buffer.toLowerCase();
        for (String cmd : allowedSet) {
            if (cmd.equals("*") || cmd.contains(":") || cmd.contains(" ")) continue;
            if (cmd.startsWith(search)) {
                String finalSuggestion = buffer.startsWith("/") ? "/" + cmd : cmd;
                if (!suggestions.contains(finalSuggestion)) {
                    suggestions.add(finalSuggestion);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGroupManager().hasBypass(player)) return;

        Set<String> allowedSet = plugin.getGroupManager().getAllowedCommandsForTab(player);
        if (allowedSet == null) return;

        Set<String> allowedRoots = plugin.getGroupManager().getAllowedBaseRootsForBrigadier(player);
        if (allowedRoots == null) return;

        Collection<String> serverCommands = event.getCommands();

        serverCommands.removeIf(command -> {
            String check = command.toLowerCase();

            // Hide namespaces (minecraft:, purpur:, bukkit:) unless explicitly listed
            if (check.contains(":")) {
                return !allowedSet.contains(check);
            }

            return !allowedRoots.contains(check);
        });

        // Ensure allowed clean base commands (including roots of subcommands) are visible in client brigadier tree
        for (String root : allowedRoots) {
            if (!serverCommands.contains(root)) {
                serverCommands.add(root);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        SchedulerUtils.runForPlayerLater(plugin, player, player::updateCommands, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getGroupManager().invalidateCache(uuid);
        adminNotificationCooldowns.remove(uuid);
    }
}
