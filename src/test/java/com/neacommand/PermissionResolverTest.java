package com.neacommand;

import com.neacommand.utils.PermissionResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermissionResolverTest {

    @Test
    @DisplayName("Should return null for null or empty input")
    void testNullOrEmpty() {
        assertNull(PermissionResolver.resolvePermission(null));
        assertNull(PermissionResolver.resolvePermission(""));
        assertNull(PermissionResolver.resolvePermission("   "));
    }

    @Test
    @DisplayName("Should format vanilla command permission with leading slash or arguments")
    void testVanillaCommand() {
        assertEquals("minecraft.command.gamemode", PermissionResolver.resolvePermission("/gamemode creative"));
        assertEquals("minecraft.command.tp", PermissionResolver.resolvePermission("tp player1 player2"));
        assertEquals("minecraft.command.help", PermissionResolver.resolvePermission("/help"));
    }

    @Test
    @DisplayName("Should resolve explicit plugin namespaces in command")
    void testExplicitNamespace() {
        assertEquals("essentials.home", PermissionResolver.resolvePermission("/essentials:home"));
        assertEquals("cmi.warp", PermissionResolver.resolvePermission("cmi:warp spawn"));
    }
}
