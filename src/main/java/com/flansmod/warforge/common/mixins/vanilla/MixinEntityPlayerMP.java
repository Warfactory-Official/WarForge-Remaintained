package com.flansmod.warforge.common.mixins.vanilla;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.FactionDisplay;
import com.flansmod.warforge.server.Faction;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces a player's tab-list display name with their faction-prefixed name when the
 * {@code Faction Prefix In Tab List} setting is enabled. Returning {@code null} (the vanilla default)
 * leaves the plain name in place.
 */
@Mixin(EntityPlayerMP.class)
public class MixinEntityPlayerMP {
    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
    private void warforge$factionTabPrefix(CallbackInfoReturnable<ITextComponent> cir) {
        if (!WarForgeConfig.FACTION_PREFIX_IN_TABLIST || WarForgeMod.FACTIONS == null) {
            return;
        }
        EntityPlayerMP self = (EntityPlayerMP) (Object) this;
        Faction faction = WarForgeMod.FACTIONS.getFactionOfPlayer(self.getUniqueID());
        if (faction == null) {
            return;
        }
        ITextComponent name = FactionDisplay.tabName(faction, self.getName());
        if (name != null) {
            cir.setReturnValue(name);
        }
    }
}
