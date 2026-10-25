package com.neacommand.utils;

import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;

public final class PermissionResolver {

    private PermissionResolver() {}

    /**
     * Resolves the real permission node required for a given command.
     * Searches Bukkit CommandMap, owning plugin namespaces, registered permissions in PluginManager,
     * and plugin-specific conventions (e.g. Essentials, CMI, HuskHomes, WorldEdit, etc.).
     *
     * @param commandLine The command line or command name (e.g. "/home", "spawn", "warp shop")
     * @return The most accurate permission node (e.g. "essentials.home", "minecraft.command.gamemode")
     */
    public static String resolvePermission(String commandLine) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            return null;
        }

        String clean = commandLine.trim().toLowerCase();
        if (clean.startsWith("/")) clean = clean.substring(1).trim();
        String baseCmd = clean.split(" ")[0];

        // 1. Check if an explicit namespace was typed (e.g. "essentials:home")
        String explicitNamespace = null;
        if (baseCmd.contains(":")) {
            String[] parts = baseCmd.split(":");
            explicitNamespace = parts[0];
            baseCmd = parts[1];
        }

        // 2. Fetch the Command object from Bukkit's CommandMap if server is initialized
        Command targetCmd = null;
        if (Bukkit.getServer() != null && Bukkit.getCommandMap() != null) {
            targetCmd = Bukkit.getCommandMap().getCommand(baseCmd);
            if (targetCmd == null) {
                targetCmd = Bukkit.getCommandMap().getCommand("minecraft:" + baseCmd);
            }
        }

        // 2a. If the Command object explicitly has a valid permission set
        if (targetCmd != null) {
            String perm = targetCmd.getPermission();
            if (isValidPermission(perm)) {
                return perm;
            }
        }

        // 3. Identify the owning plugin name or namespace
        String pluginName = explicitNamespace;

        // Try getting owner from PluginCommand
        if (pluginName == null && targetCmd instanceof PluginCommand pc) {
            Plugin owner = pc.getPlugin();
            if (owner != null) {
                pluginName = owner.getName().toLowerCase();
            }
        }

        // Try getting owner from knownCommands keys (e.g. "essentials:home", "cmi:home")
        if (pluginName == null && Bukkit.getServer() != null && Bukkit.getCommandMap() != null) {
            Map<String, Command> known = Bukkit.getCommandMap().getKnownCommands();
            for (Map.Entry<String, Command> entry : known.entrySet()) {
                String key = entry.getKey().toLowerCase();
                if (key.endsWith(":" + baseCmd)) {
                    String prefix = key.substring(0, key.indexOf(':'));
                    if (!prefix.equals("minecraft") && !prefix.equals("bukkit")) {
                        pluginName = prefix;
                        String p = entry.getValue().getPermission();
                        if (isValidPermission(p)) {
                            return p;
                        }
                        break;
                    }
                }
            }
        }

        // Try detecting from class package name
        if (pluginName == null && targetCmd != null) {
            String className = targetCmd.getClass().getName().toLowerCase();
            if (className.contains("essentials")) {
                pluginName = "essentials";
            } else if (className.contains("cmi")) {
                pluginName = "cmi";
            } else if (className.contains("huskhomes")) {
                pluginName = "huskhomes";
            } else if (className.contains("worldguard")) {
                pluginName = "worldguard";
            } else if (className.contains("worldedit")) {
                pluginName = "worldedit";
            } else if (className.contains("luckperms")) {
                pluginName = "luckperms";
            } else if (className.contains("griefprevention")) {
                pluginName = "griefprevention";
            }
        }

        boolean hasPluginManager = Bukkit.getServer() != null && Bukkit.getPluginManager() != null;

        // 4. Check standard convention candidates in Bukkit PluginManager
        if (hasPluginManager && pluginName != null && !pluginName.equals("minecraft") && !pluginName.equals("bukkit")) {
            // E.g. essentials.home
            String c1 = pluginName + "." + baseCmd;
            if (Bukkit.getPluginManager().getPermission(c1) != null) return c1;

            // E.g. cmi.command.home
            String c2 = pluginName + ".command." + baseCmd;
            if (Bukkit.getPluginManager().getPermission(c2) != null) return c2;

            // E.g. plugin.use.home
            String c3 = pluginName + ".use." + baseCmd;
            if (Bukkit.getPluginManager().getPermission(c3) != null) return c3;

            // E.g. plugin.commands.home
            String c4 = pluginName + ".commands." + baseCmd;
            if (Bukkit.getPluginManager().getPermission(c4) != null) return c4;
        }

        // 5. Scan registered permissions in Bukkit PluginManager
        if (hasPluginManager) {
            Set<Permission> registered = Bukkit.getPluginManager().getPermissions();

            // 5a. If pluginName is known, look for any registered permission starting with pluginName and matching baseCmd
            if (pluginName != null) {
                for (Permission p : registered) {
                    String pName = p.getName().toLowerCase();
                    if (pName.startsWith(pluginName + ".") && (pName.endsWith("." + baseCmd) || pName.equals(pluginName + "." + baseCmd))) {
                        return p.getName();
                    }
                }
            }

            // 5b. Check any registered permission ending with ".<baseCmd>"
            for (Permission p : registered) {
                String pName = p.getName().toLowerCase();
                if (pName.endsWith("." + baseCmd) && !pName.contains("*") && !pName.startsWith("neacommand.")) {
                    return p.getName();
                }
            }

            // 6. Vanilla / Minecraft permissions
            String vanilla = "minecraft.command." + baseCmd;
            if (Bukkit.getPluginManager().getPermission(vanilla) != null) {
                return vanilla;
            }
            String bukkit = "bukkit.command." + baseCmd;
            if (Bukkit.getPluginManager().getPermission(bukkit) != null) {
                return bukkit;
            }
        }

        // 7. If pluginName is known (e.g. Essentials), use pluginName.baseCmd (e.g. essentials.home)
        if (pluginName != null && !pluginName.equals("minecraft") && !pluginName.equals("bukkit")) {
            return pluginName + "." + baseCmd;
        }

        // 8. Fallback to vanilla command format
        return "minecraft.command." + baseCmd;
    }

    private static boolean isValidPermission(String perm) {
        if (perm == null || perm.trim().isEmpty()) return false;
        String p = perm.trim().toLowerCase();
        if (p.startsWith("neacommand.")) return false;
        if (p.equals("null") || p.equals("none")) return false;
        return true;
    }
}
