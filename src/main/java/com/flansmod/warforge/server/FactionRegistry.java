package com.flansmod.warforge.server;

import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.faction.Role;
import com.flansmod.warforge.common.util.DimBlockPos;
import lombok.Getter;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

/** Factions, the zone pseudo-factions, the lowercase-name index and the player to faction index. */
public final class FactionRegistry {
    private final HashMap<UUID, Faction> factions = new HashMap<>();
    private final HashMap<String, Faction> byLowerName = new HashMap<>();
    private final HashMap<UUID, Faction> byPlayer = new HashMap<>();
    @Getter
    private Faction safeZone;
    @Getter
    private Faction warZone;

    public FactionRegistry() {
        initNeutralZones();
    }

    public void clear() {
        factions.clear();
        byLowerName.clear();
        byPlayer.clear();
        initNeutralZones();
    }

    private void initNeutralZones() {
        safeZone = new Faction(FactionIds.SAFE_ZONE, "SafeZone", 0x00ff00, new DimBlockPos(Level.OVERWORLD, 0, 0, 0));
        warZone = new Faction(FactionIds.WAR_ZONE, "WarZone", 0xff0000, new DimBlockPos(Level.OVERWORLD, 0, 0, 0));
        register(safeZone);
        register(warZone);
    }

    /** Includes the zone pseudo-factions. */
    public Collection<Faction> getAllFactions() {
        return Collections.unmodifiableCollection(factions.values());
    }

    public boolean contains(UUID factionId) {
        return factions.containsKey(factionId);
    }

    /** @return null for null, {@link FactionIds#NONE} or unknown ids */
    public Faction getFaction(UUID factionId) {
        return factionId == null ? null : factions.get(factionId);
    }

    /** Case-insensitive; never returns a zone pseudo-faction. */
    public Faction findByName(String name) {
        if (name == null) {
            return null;
        }
        Faction faction = byLowerName.get(lower(name));
        return faction == null || FactionIds.isNeutralZone(faction.getUuid()) ? null : faction;
    }

    /** Case-insensitive; zones count as taken. */
    public boolean isNameTaken(String name, UUID exceptFactionId) {
        Faction existing = byLowerName.get(lower(name));
        return existing != null && !existing.getUuid().equals(exceptFactionId);
    }

    public Faction getFactionOfPlayer(UUID playerID) {
        return byPlayer.get(playerID);
    }

    public boolean isPlayerInFaction(UUID playerID, UUID factionID) {
        Faction faction = byPlayer.get(playerID);
        return faction != null && faction.getUuid().equals(factionID);
    }

    public boolean isPlayerRoleInFaction(UUID playerID, UUID factionID, Role role) {
        Faction faction = factions.get(factionID);
        return faction != null && faction.isPlayerRoleInFaction(playerID, role);
    }

    /** Indexes name and current members. */
    void register(Faction faction) {
        factions.put(faction.getUuid(), faction);
        byLowerName.put(lower(faction.getName()), faction);
        for (UUID member : faction.getMemberIds()) {
            byPlayer.put(member, faction);
        }
    }

    void unregister(Faction faction) {
        factions.remove(faction.getUuid());
        byLowerName.remove(lower(faction.getName()));
        for (UUID member : faction.getMemberIds()) {
            byPlayer.remove(member, faction);
        }
    }

    void rename(Faction faction, String newName) {
        byLowerName.remove(lower(faction.getName()));
        faction.setName(newName);
        byLowerName.put(lower(newName), faction);
    }

    void indexMember(UUID playerID, Faction faction) {
        byPlayer.put(playerID, faction);
    }

    void unindexMember(UUID playerID) {
        byPlayer.remove(playerID);
    }

    public void update() {
        for (Faction faction : factions.values()) {
            faction.update();
        }
    }

    public void increaseLegacy() {
        for (Faction faction : factions.values()) {
            faction.increaseLegacy();
        }
    }

    public void clearNotoriety() {
        for (Faction faction : factions.values()) {
            faction.clearNotoriety();
        }
    }

    public void clearLegacy() {
        for (Faction faction : factions.values()) {
            faction.clearLegacy();
        }
    }

    private static String lower(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
