package com.flansmod.warforge.common.network;

import com.flansmod.warforge.client.util.LayeredItemIconCache;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@AllArgsConstructor
public class SiegeCampAttackInfoRender extends SiegeCampAttackInfo {
    public enum CenterMarkType {
        NONE,
        SIEGE_CAMP,
        PLAYER_FACE,
        CUSTOM_TEXTURE
    }

    @Getter
    @Setter
    @Nullable
    public ResourceLocation veinIcon = null;
    @Getter
    @Setter
    public CenterMarkType centerMarkType = CenterMarkType.NONE;
    @Getter
    @Setter
    @Nullable
    public ResourceLocation centerIcon = null;

    @OnlyIn(Dist.CLIENT)
    public SiegeCampAttackInfoRender(SiegeCampAttackInfo info) {
        super(info);
        retrieveVeinIcon();
    }

    @OnlyIn(Dist.CLIENT)
    public void retrieveVeinIcon() {
        if (mWarforgeVein == null) {
            return;
        }

        ItemStack stack = ItemStack.EMPTY;
        var compIt = mWarforgeVein.compIds.iterator();
        if (compIt.hasNext()) {
            stack = compIt.next().toStack();
        }

        if (!stack.isEmpty()) {
            veinIcon = LayeredItemIconCache.getIcon(stack);
        }
    }
}
