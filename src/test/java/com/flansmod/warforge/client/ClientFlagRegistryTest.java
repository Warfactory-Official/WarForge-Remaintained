package com.flansmod.warforge.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ClientFlagRegistryTest {
    @Test
    void escapeKeepsSafeCharacters() {
        assertEquals("flag-1.png", ClientFlagRegistry.escape("flag-1.png"));
    }

    @Test
    void escapeSeparatesLookalikeIds() {
        assertNotEquals(ClientFlagRegistry.escape("a_b"), ClientFlagRegistry.escape("a:b"));
        assertEquals("custom_003afrance", ClientFlagRegistry.escape("custom:france"));
    }
}
