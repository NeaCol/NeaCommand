package com.neacommand;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TranslationManagerTest {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Test
    @DisplayName("Should correctly serialize and format MiniMessage tags")
    void testMiniMessageFormatting() {
        Component component = miniMessage.deserialize("<green>Success: <white>Operation completed</white></green>");
        String plainText = PlainTextComponentSerializer.plainText().serialize(component);
        assertEquals("Success: Operation completed", plainText);
    }

    @Test
    @DisplayName("Should resolve placeholders safely using MiniMessage TagResolver")
    void testPlaceholderResolution() {
        String template = "<red>Blocked: <white><command></white> by <player>";
        Component component = miniMessage.deserialize(
            template,
            Placeholder.parsed("command", "/op"),
            Placeholder.parsed("player", "Hacker")
        );
        String plainText = PlainTextComponentSerializer.plainText().serialize(component);
        assertEquals("Blocked: /op by Hacker", plainText);
    }
}
