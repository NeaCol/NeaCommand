package com.neacommand.commands;

import com.neacommand.NeaCommand;
import com.neacommand.utils.PermissionResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final NeaCommand plugin;

    public MainCommand(NeaCommand plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!sender.hasPermission("neacommand.admin")) {
            plugin.getTranslationManager().sendMessageWithPrefix(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload" -> handleReload(sender);
            case "sync" -> handleSync(sender);
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender, args);
            case "check" -> handleCheck(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getTranslationManager().load();
        plugin.getGroupManager().loadGroups();
        plugin.getTranslationManager().sendMessageWithPrefix(sender, "config-reloaded");
    }

    private void handleSync(CommandSender sender) {
        plugin.getGroupManager().syncWithLuckPerms();
        plugin.getTranslationManager().sendMessageWithPrefix(sender, "sync-success");
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getTranslationManager().getComponent(
                "<red>Usage: /nc add <group> <command> [hidden: true/false]"
            ));
            return;
        }

        String group = args[1];
        String cmd = args[2].toLowerCase();
        boolean hidden = cmd.endsWith(":hidden") || (args.length >= 4 && args[3].equalsIgnoreCase("true"));

        plugin.getGroupManager().addCommand(group, cmd, hidden);

        plugin.getTranslationManager().sendMessageWithPrefix(
            sender,
            "command-added",
            Placeholder.parsed("command", "/" + plugin.getGroupManager().normalizeCommandString(cmd)),
            Placeholder.parsed("group", group)
        );

        suggestPermission(sender, group, plugin.getGroupManager().extractBaseCommand(cmd));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getTranslationManager().getComponent(
                "<red>Usage: /nc remove <group> <command>"
            ));
            return;
        }

        String group = args[1];
        String cmd = args[2].toLowerCase();

        if (plugin.getGroupManager().removeCommand(group, cmd)) {
            plugin.getTranslationManager().sendMessageWithPrefix(
                sender,
                "command-removed",
                Placeholder.parsed("command", "/" + plugin.getGroupManager().normalizeCommandString(cmd)),
                Placeholder.parsed("group", group)
            );
        } else {
            plugin.getTranslationManager().sendMessageWithPrefix(
                sender,
                "command-not-in-group",
                Placeholder.parsed("group", group)
            );
        }
    }

    private void handleList(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getTranslationManager().getComponent(
                "<red>Usage: /nc list <group>"
            ));
            return;
        }

        String group = args[1];
        List<String> cmds = plugin.getGroupManager().getCommands(group);

        if (cmds.isEmpty()) {
            plugin.getTranslationManager().sendMessageWithPrefix(
                sender,
                "list-empty",
                Placeholder.parsed("group", group)
            );
        } else {
            plugin.getTranslationManager().sendMessage(
                sender,
                "list-header",
                Placeholder.parsed("group", group),
                Placeholder.parsed("count", String.valueOf(cmds.size()))
            );

            String formattedList = String.join(", ", cmds);
            sender.sendMessage(plugin.getTranslationManager().getComponent(
                "<gray>" + formattedList
            ));
        }
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getTranslationManager().getComponent(
                "<red>Usage: /nc check <player>"
            ));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            plugin.getTranslationManager().sendMessageWithPrefix(
                sender,
                "player-not-found",
                Placeholder.parsed("player", args[1])
            );
            return;
        }

        String primaryGroup = plugin.getGroupManager().getPlayerPrimaryGroup(target);
        List<String> inherited = plugin.getGroupManager().getPlayerInheritedGroups(target);
        Set<String> tabAllowed = plugin.getGroupManager().getAllowedCommandsForTab(target);
        boolean bypass = plugin.getGroupManager().hasBypass(target);

        String allowedCountStr = (tabAllowed == null) ? "ALL (*)" : String.valueOf(tabAllowed.size());

        plugin.getTranslationManager().sendMessage(sender, "player-check-header", Placeholder.parsed("player", target.getName()));
        plugin.getTranslationManager().sendMessage(sender, "player-check-primary", Placeholder.parsed("group", primaryGroup));
        plugin.getTranslationManager().sendMessage(sender, "player-check-inherited", Placeholder.parsed("groups", String.join(", ", inherited)));
        plugin.getTranslationManager().sendMessage(sender, "player-check-total-commands", Placeholder.parsed("count", allowedCountStr));
        plugin.getTranslationManager().sendMessage(sender, "player-check-bypass", Placeholder.parsed("bypass", bypass ? "true" : "false"));
    }

    private void suggestPermission(CommandSender sender, String groupName, String commandName) {
        String permission = PermissionResolver.resolvePermission(commandName);
        if (permission == null || permission.isEmpty()) return;

        final String finalPerm = permission;

        if (plugin.getLuckPerms() != null) {
            Group lpGroup = plugin.getLuckPerms().getGroupManager().getGroup(groupName);
            if (lpGroup != null) {
                boolean alreadyHas = lpGroup.getCachedData().getPermissionData().checkPermission(finalPerm).asBoolean();
                if (!alreadyHas) {
                    alreadyHas = lpGroup.getNodes().stream()
                        .filter(Node::getValue)
                        .anyMatch(node -> {
                            String k = node.getKey().toLowerCase();
                            return k.equals(finalPerm.toLowerCase())
                                || k.equals("*")
                                || (finalPerm.contains(".") && k.equals(finalPerm.substring(0, finalPerm.indexOf('.')) + ".*"));
                        });
                }

                if (alreadyHas) return;
            }
        }

        Component suggestion = plugin.getTranslationManager().getComponent(
            "permission-suggestion",
            Placeholder.parsed("permission", finalPerm),
            Placeholder.parsed("group", groupName)
        );
        sender.sendMessage(suggestion);
    }

    private void sendHelp(CommandSender sender) {
        List<String> helpLines = plugin.getTranslationManager().getRawList("help");
        for (String line : helpLines) {
            sender.sendMessage(plugin.getTranslationManager().getComponent(line));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        List<String> list = new ArrayList<>();
        if (!sender.hasPermission("neacommand.admin")) return list;

        if (args.length == 1) {
            list.add("add");
            list.add("remove");
            list.add("list");
            list.add("check");
            list.add("sync");
            list.add("reload");
            list.add("help");
            return filter(list, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("add") || sub.equals("remove") || sub.equals("list")) {
                list.addAll(plugin.getGroupManager().getConfiguredGroups());
                return filter(list, args[1]);
            }
            if (sub.equals("check")) {
                Bukkit.getOnlinePlayers().forEach(p -> list.add(p.getName()));
                return filter(list, args[1]);
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("remove")) {
                list.addAll(plugin.getGroupManager().getCommands(args[1]));
            } else if (sub.equals("add")) {
                Bukkit.getCommandMap().getKnownCommands().keySet().forEach(cmd -> {
                    if (!cmd.contains(":")) list.add(cmd);
                });
            }
            return filter(list, args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("add")) {
            list.add("true");
            list.add("false");
            return filter(list, args[3]);
        }

        return list;
    }

    private List<String> filter(List<String> list, String query) {
        String lower = query.toLowerCase();
        return list.stream()
            .filter(s -> s.toLowerCase().startsWith(lower))
            .collect(Collectors.toList());
    }
}
