package com.neacommand.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Utility class to ensure scheduling compatibility across Paper, Purpur, and Folia.
 */
public final class SchedulerUtils {

    private SchedulerUtils() {}

    /**
     * Executes a task on the player's entity thread context (Folia-safe).
     */
    public static void runForPlayer(Plugin plugin, Player player, Runnable task) {
        if (player == null || !player.isOnline()) return;

        try {
            player.getScheduler().run(plugin, scheduledTask -> task.run(), null);
        } catch (NoSuchMethodError | Exception e) {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Schedules a delayed task on the player's entity thread context (Folia-safe).
     */
    public static void runForPlayerLater(Plugin plugin, Player player, Runnable task, long delayTicks) {
        if (player == null || !player.isOnline()) return;

        try {
            player.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, delayTicks);
        } catch (NoSuchMethodError | Exception e) {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * Executes a task on the global server scheduler.
     */
    public static void runGlobal(Plugin plugin, Runnable task) {
        try {
            Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
        } catch (NoSuchMethodError | Exception e) {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}
