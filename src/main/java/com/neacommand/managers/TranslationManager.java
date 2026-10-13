package com.neacommand.managers;

import com.neacommand.NeaCommand;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class TranslationManager {

    private final NeaCommand plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private FileConfiguration langConfig;
    private FileConfiguration defaultEnglishConfig;

    public TranslationManager(NeaCommand plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        saveDefaultLanguageFiles();

        String lang = plugin.getConfig().getString("language", "en").toLowerCase();
        File langDir = new File(plugin.getDataFolder(), "lang");
        File langFile = new File(langDir, "messages_" + lang + ".yml");

        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file 'messages_" + lang + ".yml' not found! Falling back to English.");
            langFile = new File(langDir, "messages_en.yml");
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);

        // Fallback loader from jar
        InputStream defStream = plugin.getResource("lang/messages_en.yml");
        if (defStream != null) {
            defaultEnglishConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defStream, StandardCharsets.UTF_8)
            );
        }
    }

    private void saveDefaultLanguageFiles() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        saveResourceIfNotExists("lang/messages_en.yml");
        saveResourceIfNotExists("lang/messages_es.yml");
    }

    private void saveResourceIfNotExists(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    public String getRaw(String key) {
        String msg = langConfig.getString(key);
        if (msg == null && defaultEnglishConfig != null) {
            msg = defaultEnglishConfig.getString(key);
        }
        return (msg != null) ? msg : key;
    }

    public List<String> getRawList(String key) {
        List<String> list = langConfig.getStringList(key);
        if (list.isEmpty() && defaultEnglishConfig != null) {
            list = defaultEnglishConfig.getStringList(key);
        }
        return list;
    }

    public Component getComponent(String key, TagResolver... resolvers) {
        String raw = getRaw(key);
        return miniMessage.deserialize(raw, resolvers);
    }

    public Component getComponentWithPrefix(String key, TagResolver... resolvers) {
        String prefix = getRaw("prefix");
        String raw = getRaw(key);
        return miniMessage.deserialize(prefix + raw, resolvers);
    }

    public void sendMessage(CommandSender sender, String key, TagResolver... resolvers) {
        sender.sendMessage(getComponent(key, resolvers));
    }

    public void sendMessageWithPrefix(CommandSender sender, String key, TagResolver... resolvers) {
        sender.sendMessage(getComponentWithPrefix(key, resolvers));
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }
}
