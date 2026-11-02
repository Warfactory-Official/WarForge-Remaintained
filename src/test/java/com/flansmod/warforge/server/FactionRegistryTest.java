package com.flansmod.warforge.server;

import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.util.DimBlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("needs-registries")
class FactionRegistryTest {
    private static Faction faction(String name) {
        return new Faction(FactionIds.createUUID(name), name, 0xFFFFFF, new DimBlockPos(Level.OVERWORLD, 0, 64, 0));
    }

    @Test
    void nameLookupAndUniquenessIgnoreCase() {
        FactionRegistry registry = new FactionRegistry();
        Faction faction = faction("Wolves");
        registry.register(faction);

        assertSame(faction, registry.findByName("wOLVES"));
        assertTrue(registry.isNameTaken("WOLVES", null));
        assertFalse(registry.isNameTaken("WOLVES", faction.getUuid()));
        assertFalse(registry.isNameTaken("Bears", null));
    }

    @Test
    void zonesAreNeverFoundByNameButBlockTheName() {
        FactionRegistry registry = new FactionRegistry();
        assertNull(registry.findByName("SafeZone"));
        assertTrue(registry.isNameTaken("safezone", null));
        assertTrue(registry.isNameTaken("WARZONE", null));
    }

    @Test
    void renameMovesTheNameIndex() {
        FactionRegistry registry = new FactionRegistry();
        Faction faction = faction("Old");
        registry.register(faction);
        registry.rename(faction, "New");

        assertNull(registry.findByName("old"));
        assertSame(faction, registry.findByName("NEW"));
    }

    @Test
    void playerIndexFollowsMembership() {
        FactionRegistry registry = new FactionRegistry();
        Faction faction = faction("Wolves");
        registry.register(faction);
        UUID player = UUID.randomUUID();

        faction.addMember(player);
        registry.indexMember(player, faction);
        assertSame(faction, registry.getFactionOfPlayer(player));

        faction.removeMember(player);
        registry.unindexMember(player);
        assertNull(registry.getFactionOfPlayer(player));
    }
}
