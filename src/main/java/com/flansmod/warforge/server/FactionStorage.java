package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarClock;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.compat.Ids;
import com.flansmod.warforge.compat.Keys;
import com.flansmod.warforge.compat.Nbt;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;

/** Entry point to the faction stores and services; owns load/save sequencing (warforgefactions.dat). */
public class FactionStorage {
    public final FactionRegistry registry = new FactionRegistry();
    public final ClaimRegistry claims = new ClaimRegistry();
    public final SiegeRegistry sieges = new SiegeRegistry(registry, claims);
    public final ConqueredChunks conquered = new ConqueredChunks();
    public final InsuranceVaults insurance = new InsuranceVaults();
    public final FactionNotifier notifier = new FactionNotifier();
    public final NamePlateSync namePlates = new NamePlateSync();
    public final CombatLogHandler combatLog = new CombatLogHandler();
    public final ClaimPolicy claimPolicy = new ClaimPolicy(this);
    public final ClaimService claimService = new ClaimService(this);
    public final IslandService islands = new IslandService(this);
    public final MembershipService membership = new MembershipService(this);
    public final FactionLifecycleService lifecycle = new FactionLifecycleService(this);
    public final UpgradeService upgrades = new UpgradeService(this);
    public final CitadelService citadels = new CitadelService(this);
    public final AllianceService alliances = new AllianceService(this);
    public final SiegeService siegeService = new SiegeService(this);
    public final SiegeSync siegeSync = new SiegeSync(this);
    public final ClaimMapWorldSync mapSync = new ClaimMapWorldSync(this);

    public void update() {
        registry.update();
        combatLog.tick();
        mapSync.tick();
    }

    public void advanceYieldDay() {
        for (Faction faction : registry.getAllFactions()) {
            islands.awardYields(faction);
        }
        if (WarForgeConfig.LEGACY_USES_YIELD_TIMER) {
            registry.increaseLegacy();
        }
    }

    /** Full recompute (startup, vein reload). */
    public void recalculateAllWealth() {
        for (Faction faction : registry.getAllFactions()) {
            claims.recalculateWealth(faction);
        }
    }

    public void updateConqueredChunks(long msUpdateTime) {
        // the difference stays far below int range (596 h in ms)
        conquered.tick((int) (msUpdateTime - WarClock.previousUpdateTimestamp));
    }

    public void readFromNBT(CompoundTag tags) {
        registry.clear();
        claims.clear();
        sieges.clear();
        WarForgeMod.FOBS.clearFobs();

        for (Tag baseTag : Nbt.getList(tags, "factions", Tag.TAG_COMPOUND)) {
            CompoundTag factionTags = (CompoundTag) baseTag;
            if (!Nbt.hasUUID(factionTags, "id")) {
                WarForgeMod.LOGGER.warn("Skipping faction entry with missing/invalid id while loading warforgefactions.dat");
                continue;
            }
            UUID id = Nbt.getUUID(factionTags, "id");
            boolean zone = FactionIds.isNeutralZone(id);
            Faction faction = zone ? registry.getFaction(id) : new Faction();
            if (zone) {
                registry.unregister(faction);
            }
            faction.readFromNBT(factionTags);
            registry.register(faction);
            WarForgeMod.FOBS.readFactionFobs(faction.getUuid(), factionTags);
        }
        claims.rebuild(registry.getAllFactions());

        WarForgeMod.FOBS.readFromNBT(tags);

        for (Tag baseTag : Nbt.getList(tags, "sieges", Tag.TAG_COMPOUND)) {
            CompoundTag siegeTags = (CompoundTag) baseTag;
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, Ids.parse(Nbt.getString(siegeTags, "dim")));
            Siege siege = new Siege();
            siege.readFromNBT(siegeTags);
            sieges.put(new DimChunkPos(dim, Nbt.getInt(siegeTags, "x"), Nbt.getInt(siegeTags, "z")), siege);
        }

        conquered.readFromNBT(tags);
        insurance.readFromNBT(tags);
        membership.readFromNBT(tags);
    }

    public void writeToNBT(CompoundTag tags) {
        WarForgeMod.FOBS.writeToNBT(tags);

        ListTag factionList = new ListTag();
        for (Faction faction : registry.getAllFactions()) {
            CompoundTag factionTags = new CompoundTag();
            Nbt.putUUID(factionTags, "id", faction.getUuid());
            faction.writeToNBT(factionTags);
            WarForgeMod.FOBS.writeFactionFobs(faction.getUuid(), factionTags);
            factionList.add(factionTags);
        }
        tags.put("factions", factionList);

        ListTag siegeList = new ListTag();
        for (Map.Entry<DimChunkPos, Siege> kvp : sieges.entries().entrySet()) {
            CompoundTag siegeTags = new CompoundTag();
            siegeTags.putString("dim", Keys.id(kvp.getKey().dim).toString());
            siegeTags.putInt("x", kvp.getKey().x);
            siegeTags.putInt("z", kvp.getKey().z);
            kvp.getValue().writeToNBT(siegeTags);
            siegeList.add(siegeTags);
        }
        tags.put("sieges", siegeList);

        conquered.writeToNBT(tags);
        insurance.writeToNBT(tags);
        membership.writeToNBT(tags);
    }
}
