package com.neacommand;

import com.neacommand.managers.GroupManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroupManagerTest {

    @Test
    @DisplayName("Should normalize command string removing leading slash and hidden suffix")
    void testNormalizeCommandString() {
        assertEquals("fly", GroupManager.normalizeCommandString("/fly"));
        assertEquals("fly", GroupManager.normalizeCommandString("/fly:hidden"));
        assertEquals("fly", GroupManager.normalizeCommandString("fly:hidden"));
        assertEquals("warp spawn", GroupManager.normalizeCommandString("/warp spawn"));
        assertEquals("cmi:warp spawn", GroupManager.normalizeCommandString("/cmi:warp spawn"));
        assertEquals("", GroupManager.normalizeCommandString(null));
        assertEquals("", GroupManager.normalizeCommandString(""));
        assertEquals("", GroupManager.normalizeCommandString("   "));
    }

    @Test
    @DisplayName("Should extract base command ignoring namespace and arguments")
    void testExtractBaseCommand() {
        assertEquals("warp", GroupManager.extractBaseCommand("/warp spawn"));
        assertEquals("help", GroupManager.extractBaseCommand("/minecraft:help 1"));
        assertEquals("fly", GroupManager.extractBaseCommand("essentials:fly:hidden"));
        assertEquals("ban", GroupManager.extractBaseCommand("/ban player reason"));
        assertEquals("msg", GroupManager.extractBaseCommand("/msg Notch hello there"));
        assertEquals("", GroupManager.extractBaseCommand(""));
        assertEquals("", GroupManager.extractBaseCommand(null));
        assertEquals("", GroupManager.extractBaseCommand("   "));
    }
}
