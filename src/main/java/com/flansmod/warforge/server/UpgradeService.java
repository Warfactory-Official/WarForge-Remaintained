package com.flansmod.warforge.server;

import com.flansmod.warforge.common.Sounds;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.EffectPackets;
import com.flansmod.warforge.common.faction.Role;
import com.flansmod.warforge.common.item.ItemMatcher;
import com.flansmod.warforge.compat.Inventories;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.flansmod.warforge.server.ServerText.t;

/** Citadel level-up. */
public final class UpgradeService {
    private static final int EFFECT_PARTICLES = 100;
    private static final int EFFECT_LIFE_TICKS = 400;
    private static final double EFFECT_SPREAD = 0.2D;
    private static final int EFFECT_RINGS = 10;

    private final FactionStorage hub;

    UpgradeService(FactionStorage hub) {
        this.hub = hub;
    }

    public boolean requestLevelUp(ServerPlayer officer, UUID factionId) {
        Faction faction = hub.registry.getFaction(factionId);
        if (faction == null) {
            officer.sendSystemMessage(t("faction.not_found"));
            return false;
        }
        if (!faction.isPlayerInFaction(officer.getUUID())) {
            officer.sendSystemMessage(t("upgrade.not_member"));
            return false;
        }
        if (!FactionAccess.requireRole(officer, faction, Role.OFFICER)) {
            return false;
        }
        Map<ItemMatcher, Integer> required = WarForgeMod.UPGRADE_HANDLER.getRequirementsFor(faction.getCitadelLevel() + 1);
        if (required == null) {
            officer.sendSystemMessage(t("upgrade.max_level"));
            return false;
        }

        List<ItemStack> inventory = Inventories.main(officer.getInventory());
        // consume against copies; commit only when every requirement is met
        ItemStack[] remaining = new ItemStack[inventory.size()];
        for (int i = 0; i < remaining.length; i++) {
            remaining[i] = inventory.get(i).copy();
        }
        for (Map.Entry<ItemMatcher, Integer> entry : required.entrySet()) {
            int needed = entry.getValue();
            for (ItemStack stack : remaining) {
                if (needed > 0 && !stack.isEmpty() && entry.getKey().matches(stack)) {
                    int consumed = Math.min(needed, stack.getCount());
                    needed -= consumed;
                    stack.shrink(consumed);
                }
            }
            if (needed > 0) {
                officer.sendSystemMessage(t("upgrade.missing_items"));
                return false;
            }
        }
        for (int i = 0; i < remaining.length; i++) {
            inventory.set(i, remaining[i]);
        }

        faction.incrementCitadelLevel();
        faction.messageAll(t("upgrade.done", officer.getName().getString(), faction.getCitadelLevel(),
                WarForgeMod.UPGRADE_HANDLER.getClaimLimitForLevel(faction.getCitadelLevel()), faction.getMaxForceLoadedChunks()));
        hub.notifier.upgraded(faction, officer.getUUID(), officer.getName().getString());

        faction.soundEffectAll(Sounds.SFX_UPGRADE.get());
        EffectPackets.upgrade(faction.getCitadelPos().dim, faction.getCitadelPos().toRegularPos().above(2),
                EFFECT_PARTICLES, EFFECT_LIFE_TICKS, 1f, EFFECT_SPREAD, faction.getColour(), EFFECT_RINGS);
        return true;
    }
}
