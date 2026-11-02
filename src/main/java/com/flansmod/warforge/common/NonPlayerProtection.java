package com.flansmod.warforge.common;

import com.flansmod.warforge.api.AlterKind;
import com.flansmod.warforge.api.HarmKind;
import com.flansmod.warforge.common.WarForgeConfig.ProtectionConfig;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

/** Claim rules for effects with a responsible player and/or faction but no player event. Server thread. */
public final class NonPlayerProtection {

    private NonPlayerProtection() {}

    /** Actor faction: the player's own, else {@code faction}; neither => outsider. */
    @Nullable
    private static UUID actorFaction(@Nullable UUID player, @Nullable UUID faction) {
        if (FactionIds.isValid(player)) {
            Faction own = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(player);
            if (own != null) return own.getUuid();
        }
        return faction;
    }

    public static boolean mayAlter(DimChunkPos chunk, @Nullable UUID player, @Nullable UUID faction, AlterKind kind) {
        ProtectionConfig config = ProtectionsModule.getProtectionsForFaction(actorFaction(player, faction), chunk);
        return switch (kind) {
            case EXPLOSION -> config.EXPLOSION_DAMAGE && config.BLOCK_REMOVAL;
            case FIRE, FLUID, RADIATION -> config.BLOCK_REMOVAL;
            case TERRAFORM -> config.BREAK_BLOCKS && config.BLOCK_REMOVAL;
            case PLACEMENT -> config.PLACE_BLOCKS;
        };
    }

    /** Players only: other entities have no claim damage rule. A player source is "from player", anything else "other". */
    public static boolean mayHarm(Level level, Entity victim, @Nullable UUID player, @Nullable UUID faction, HarmKind kind) {
        if (!(victim instanceof Player)) return true;
        ProtectionConfig config = ProtectionsModule.getProtections(victim.getUUID(),
                new DimChunkPos(level.dimension(), victim.blockPosition()));
        return FactionIds.isValid(player) ? config.PLAYER_TAKE_DAMAGE_FROM_PLAYER : config.PLAYER_TAKE_DAMAGE_FROM_OTHER;
    }
}
