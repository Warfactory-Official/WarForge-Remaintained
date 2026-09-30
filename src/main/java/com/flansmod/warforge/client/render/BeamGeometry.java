package com.flansmod.warforge.client.render;

import com.flansmod.warforge.common.blocks.PoleGeometry;
import com.flansmod.warforge.common.blocks.TileEntityClaim;
import com.flansmod.warforge.common.blocks.TileEntityDummy;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/** The coloured beam above a claim statue, independent of the render API. */
public final class BeamGeometry {
    private BeamGeometry() {
    }

    /**
     * @param start       beam start relative to the dummy block entity's Y
     * @param height      beam height in blocks
     * @param packedColor ARGB colour
     * @param rgb         the same colour as red, green, blue in 0..1
     */
    public record Beam(double start, int height, int packedColor, float[] rgb) {
    }

    /** The beam of a dummy block entity, or null if it has none. */
    public static @Nullable Beam of(TileEntityDummy te) {
        if (!te.getLaserRender()) return null;

        float[] color = te.getLaserRGB();
        int packedColor = 0xFF000000 | ((int) (color[0] * 255f) << 16) | ((int) (color[1] * 255f) << 8) | (int) (color[2] * 255f);

        double poleTop = te.getBlockPos().getY() + PoleGeometry.BASE_TRANSLATE;
        BlockPos master = te.getMasterTile();
        if (master != null) {
            float poleLength = TileEntityClaim.DEFAULT_POLE_LENGTH;
            if (te.getLevel().getBlockEntity(master) instanceof TileEntityClaim claim) {
                poleLength = claim.getPoleLength();
            }
            poleTop = master.getY() + PoleGeometry.topOffset(poleLength);
        }

        double beamStart = poleTop - te.getBlockPos().getY();
        int height = Math.max(0, Mth.ceil(257.0D - poleTop));
        return new Beam(beamStart, height, packedColor, color);
    }
}
