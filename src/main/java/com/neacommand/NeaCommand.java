package com.neacommand;

import com.neacommand.commands.MainCommand;
import com.neacommand.listeners.CommandListener;
import com.neacommand.managers.GroupManager;
import com.neacommand.managers.TranslationManager;
import net.luckperms.api.LuckPerms;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class NeaCommand extends JavaPlugin {

    private static NeaCommand instance;
    private TranslationManager translationManager;
    private GroupManager groupManager;

    // bStats metrics plugin ID
    private static final int BSTATS_PLUGIN_ID = 22100;

    @Override
    public void onEnable() {
        instance = this;

        // Print ASCII Banner
        printBanner();

        // Save default config
        saveDefaultConfig();

        // Initialize Managers
        this.translationManager = new TranslationManager(this);
        this.groupManager = new GroupManager(this);

        // Register Command
        if (getCommand("neacommand") != null) {
            MainCommand mainCommand = new MainCommand(this);
            getCommand("neacommand").setExecutor(mainCommand);
            getCommand("neacommand").setTabCompleter(mainCommand);
        }

        // Register Listeners
        getServer().getPluginManager().registerEvents(new CommandListener(this), this);

        // Initialize bStats Metrics
        if (getConfig().getBoolean("metrics", true)) {
            try {
                new Metrics(this, BSTATS_PLUGIN_ID);
                getLogger().info("bStats metrics enabled. Thank you for supporting the plugin!");
            } catch (Exception e) {
                getLogger().warning("Could not initialize bStats: " + e.getMessage());
            }
        }

        getLogger().info("NeaCommand v" + getDescription().getVersion() + " successfully enabled!");
    }

    @Override
    public void onDisable() {
        if (groupManager != null) {
            groupManager.invalidateAllCache();
        }
        getLogger().info("NeaCommand disabled successfully.");
    }

    private void printBanner() {
        getLogger().info("  _   _             _____                                          _ ");
        getLogger().info(" | \\ | |           / ____|                                        | |");
        getLogger().info(" |  \\| | ___  __ _| |     ___  _ __ ___  _ __ ___   __ _ _ __   __| |");
        getLogger().info(" | . ` |/ _ \\/ _` | |    / _ \\| '_ ` _ \\| '_ ` _ \\ / _` | '_ \\ / _` |");
        getLogger().info(" | |\\  |  __/ (_| | |___| (_) | | | | | | | | | | | (_| | | | | (_| |");
        getLogger().info(" |_| \\_|\\___|\\__,_|\\_____\\___/|_| |_| |_|_| |_| |_|\\__,_|_| |_|\\__,_|");
        getLogger().info("                                                                     ");
        getLogger().info("       Open Source Command Whitelist & Tab-Completion Blocker         ");
        getLogger().info("                     https://github.com/NeaCol/NeaCommand             ");
    }

    public static NeaCommand getInstance() {
        return instance;
    }

    public TranslationManager getTranslationManager() {
        return translationManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public LuckPerms getLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider =
            Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        return (provider != null) ? provider.getProvider() : null;
    }
}
