package com.flansmod.warforge.client.render;

import com.flansmod.warforge.common.blocks.PoleGeometry;
import com.flansmod.warforge.common.blocks.ClaimBlockEntity;
import com.flansmod.warforge.common.blocks.DummyBlockEntity;
import com.flansmod.warforge.compat.Levels;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class BeamGeometry {
    private BeamGeometry() {
    }

    public record Beam(double start, int height, int packedColor, float[] rgb) {
    }

    public static @Nullable Beam of(DummyBlockEntity te) {
        if (!te.getLaserRender()) return null;

        float[] color = te.getLaserRGB();
        int packedColor = 0xFF000000 | ((int) (color[0] * 255f) << 16) | ((int) (color[1] * 255f) << 8) | (int) (color[2] * 255f);

        double poleTop = te.getBlockPos().getY() + PoleGeometry.BASE_TRANSLATE;
        BlockPos master = te.getMasterTile();
        if (master != null) {
            float poleLength = ClaimBlockEntity.DEFAULT_POLE_LENGTH;
            if (te.getLevel().getBlockEntity(master) instanceof ClaimBlockEntity claim) {
                poleLength = claim.getPoleLength();
            }
            poleTop = master.getY() + PoleGeometry.topOffset(poleLength);
        }

        double beamStart = poleTop - te.getBlockPos().getY();
        int height = Math.max(0, Mth.ceil(Levels.maxYExclusive(te.getLevel()) - poleTop));
        return new Beam(beamStart, height, packedColor, color);
    }
}
