package com.flansmod.warforge.client.ui.model;

import static com.flansmod.warforge.common.ui.UiLang.tr;

import com.flansmod.warforge.common.util.DimChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@OnlyIn(Dist.CLIENT)
public final class ClaimManagerView {
    public interface Opener {
        void open(DimChunkPos center, int radius, int pageX, int pageZ);
    }

    private static @Nullable Object screen;
    private static @Nullable DimChunkPos center;
    private static int radius;
    private static int pageX;
    private static int pageZ;
    private static int signature;

    private ClaimManagerView() {
    }

    public static int signature(DimChunkPos center, int radius, int pageX, int pageZ, ClaimMapModel.Modes modes,
                                ClaimMapModel.Cell[] cells) {
        int h = center.hashCode();
        h = 31 * h + radius;
        h = 31 * h + pageX;
        h = 31 * h + pageZ;
        h = 31 * h + (modes.startPick ? 1 : 0) + (modes.target ? 2 : 0) + (modes.canDeclareSiege ? 4 : 0);
        for (ClaimMapModel.Cell cell : cells) {
            h = 31 * h + (cell.info == null ? 0 : cell.info.signature());
            h = 31 * h + Arrays.hashCode(cell.adjacency);
            h = 31 * h + (cell.renderInfo.canAttack ? 1 : 0);
        }
        return h;
    }

    public static boolean unchanged(@Nullable Object currentScreen, int newSignature) {
        return isOpen(currentScreen) && newSignature == signature;
    }

    public static void shown(Object shownScreen, int shownSignature, DimChunkPos shownCenter, int shownRadius, int shownPageX, int shownPageZ) {
        signature = shownSignature;
        screen = shownScreen;
        center = shownCenter;
        radius = shownRadius;
        pageX = shownPageX;
        pageZ = shownPageZ;
    }

    public static boolean isOpen(@Nullable Object currentScreen) {
        if (screen != null && screen != currentScreen) {
            reset();
        }
        return screen != null;
    }

    public static void refreshIfOpen(@Nullable Object currentScreen, Opener opener) {
        if (isOpen(currentScreen)) {
            opener.open(center, radius, pageX, pageZ);
        }
    }

    public static void reset() {
        screen = null;
        center = null;
    }

    public static String pagingLine(int visibleSize, int size) {
        return visibleSize < size
                ? tr("warforge.ui.paged_view_dx_d_across_the_selected_radius", visibleSize, visibleSize)
                : tr("warforge.ui.showing_the_full_selected_radius");
    }
}
