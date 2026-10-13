package com.neacommand.listeners;

import com.neacommand.NeaCommand;
import com.neacommand.utils.SchedulerUtils;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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

            notifyAdmins(player, commandName);
        }
    }

    private void notifyAdmins(Player player, String command) {
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

        Collection<String> serverCommands = event.getCommands();

        serverCommands.removeIf(command -> {
            String check = command.toLowerCase();

            // Hide namespaces (minecraft:, purpur:, bukkit:) unless explicitly listed
            if (check.contains(":")) {
                return !allowedSet.contains(check);
            }

            return !allowedSet.contains(check);
        });

        // Ensure allowed clean base commands are visible in client brigadier tree
        for (String allowed : allowedSet) {
            if (allowed.equals("*") || allowed.contains(":") || allowed.contains(" ")) continue;
            String clean = allowed.startsWith("/") ? allowed.substring(1) : allowed;
            if (!serverCommands.contains(clean)) {
                serverCommands.add(clean);
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
        plugin.getGroupManager().invalidateCache(event.getPlayer().getUniqueId());
    }
}
