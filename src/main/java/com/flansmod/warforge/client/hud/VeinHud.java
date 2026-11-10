package com.flansmod.warforge.client.hud;

import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.api.vein.VeinCodec;
import com.flansmod.warforge.client.ClientConfig;
import com.flansmod.warforge.client.ChunkVeinCache;
import com.flansmod.warforge.client.ClientProxy;
import com.flansmod.warforge.client.render.Gui2D;
import com.flansmod.warforge.client.render.HudClock;
import com.flansmod.warforge.client.util.ScreenSpaceUtil;
import com.flansmod.warforge.client.util.ScreenSpaceUtil.ScreenPos;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.item.ItemMatcher;
import com.flansmod.warforge.common.network.PacketChunkPosVeinID;
import com.flansmod.warforge.common.util.DimChunkPos;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;

public final class VeinHud {
    private static final int ICON_SIZE = 24;
    private static final int ICON_NATIVE_SIZE = 16;
    private static final int ICON_GAP = 4;
    private static final int TITLE_TOP_MARGIN = 4;
    private static final int COMPONENT_INDENT = 4;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final long REPROBE_INTERVAL_MS = 5000L;

    private static final ObjectOpenHashSet<String> REPORTED_UNRESOLVED = new ObjectOpenHashSet<>();

    private static boolean visible = true;
    private static DimChunkPos chunk;

    private static boolean built;
    private static boolean builtReceived;
    private static Pair<Vein, Quality> builtInfo;
    private static String[] lines = new String[0];
    private static ItemStack[] stacks = new ItemStack[0];
    private static int titleWidth;

    private static int iconIndex;
    private static long iconShownSinceMs = -1L;

    private static DimChunkPos probedChunk;
    private static long nextProbeMs;

    private VeinHud() {
    }

    public static boolean toggleVisible() {
        visible = !visible;
        resetCycle();
        return visible;
    }

    public static void reset() {
        visible = true;
        chunk = null;
        built = false;
        probedChunk = null;
        REPORTED_UNRESOLVED.clear();
        resetCycle();
    }

    public static void onChunkChanged(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        chunk = new DimChunkPos(dim, chunkX, chunkZ);
        built = false;
        resetCycle();
    }

    private static void resetCycle() {
        iconIndex = 0;
        iconShownSinceMs = -1L;
    }

    public static void render(Minecraft mc, Gui2D graphics) {
        // In a replay the camera's chunk is shown from the vein answers recorded with it; the inert replay server never answers a probe.
        if (!visible || chunk == null || !WarForgeConfig.ENABLE_CHUNK_VEINS || (HudClock.replay() && !ClientConfig.SHOW_VEIN_HUD_IN_REPLAYS)) {
            return;
        }
        ChunkVeinCache cache = ClientProxy.CHUNK_VEIN_CACHE;
        boolean received = cache.isReceived(chunk);
        Pair<Vein, Quality> info = cache.get(chunk);
        if (!built || received != builtReceived || info != builtInfo) {
            rebuild(mc, info, received);
        }

        boolean recognized = received && (info == null || info.getLeft() != null);
        long now = HudClock.now();
        if (!recognized && !HudClock.replay() && (probedChunk != chunk || now >= nextProbeMs)) {
            probedChunk = chunk;
            nextProbeMs = now + REPROBE_INTERVAL_MS;
            PacketChunkPosVeinID request = new PacketChunkPosVeinID();
            request.veinLocation = chunk;
            WarForgeMod.NETWORK.sendToServer(request);
        }

        advanceIcon(now);
        draw(mc, graphics);
    }

    private static void advanceIcon(long now) {
        if (stacks.length == 0) {
            return;
        }
        if (iconShownSinceMs < 0) {
            iconShownSinceMs = now;
        } else if (now - iconShownSinceMs > ClientConfig.VEIN_MEMBER_DISPLAY_TIME_MS) {
            iconShownSinceMs = now;
            iconIndex = (iconIndex + 1) % stacks.length;
        }
    }

    private static void draw(Minecraft mc, Gui2D graphics) {
        ScreenPos pos = ClientConfig.POS_VEIN_INDICATOR;
        int imageSize = stacks.length > 0 ? ICON_SIZE : 0;
        int textHeight = ScreenSpaceUtil.textHeight();
        int blockHeight = Math.max(textHeight, (textHeight + imageSize) / 2);
        int titleX = ScreenSpaceUtil.getX(pos, titleWidth);
        int titleY = ScreenSpaceUtil.getY(pos, blockHeight) + TITLE_TOP_MARGIN + (blockHeight - textHeight);
        if (titleY > ClientConfig.HUD_VERT_CUTOFF_PERCENT * ScreenSpaceUtil.screenHeight()) {
            return;
        }

        graphics.text(mc.font, lines[0], titleX, titleY, TEXT_COLOR, true);
        ScreenSpaceUtil.incrementY(pos, textHeight);

        if (stacks.length > 0) {
            float scale = imageSize / (float) ICON_NATIVE_SIZE;
            graphics.push();
            graphics.translate(titleX - imageSize / 2 - ICON_GAP - imageSize / 2f, titleY + textHeight / 2f - imageSize / 2f);
            graphics.scale(scale, scale);
            graphics.item(stacks[iconIndex], 0, 0);
            graphics.pop();
        }

        for (int i = 1; i < lines.length; i++) {
            graphics.text(mc.font, lines[i], titleX + COMPONENT_INDENT, ScreenSpaceUtil.currentY(pos), TEXT_COLOR, true);
            ScreenSpaceUtil.incrementY(pos, textHeight);
        }
    }

    private static void rebuild(Minecraft mc, Pair<Vein, Quality> info, boolean received) {
        built = true;
        builtReceived = received;
        builtInfo = info;
        stacks = new ItemStack[0];

        if (!received) {
            lines = new String[]{I18n.get("warforge.info.vein.waiting")};
        } else if (info == null) {
            lines = new String[]{I18n.get("warforge.info.vein.null")};
        } else if (info.getLeft() == null || info.getRight() == null) {
            lines = new String[]{I18n.get("warforge.info.vein.unrecognized")};
        } else {
            buildVeinLines(info);
        }
        titleWidth = mc.font.width(lines[0]);
    }

    private static void buildVeinLines(Pair<Vein, Quality> info) {
        Vein vein = info.getLeft();
        Quality quality = info.getRight();
        ObjectArrayList<String> textLines = new ObjectArrayList<>(vein.compIds.size() + 1);
        ObjectArrayList<ItemStack> icons = new ObjectArrayList<>(vein.compIds.size());
        textLines.add(I18n.get(vein.translationKey, I18n.get(quality.getTranslationKey()) + " [" + quality.getMultString(vein) + "]"));

        for (ItemMatcher component : vein.compIds) {
            ItemStack stack = component.toStack();
            if (stack.isEmpty()) {
                if (REPORTED_UNRESOLVED.add(vein.translationKey)) {
                    WarForgeMod.LOGGER.error("Vein {} has a component that resolves to no item: {}", vein.translationKey, component);
                }
                continue;
            }
            icons.add(stack);
            // Hover name, not the raw description id: shaped items (e.g. "%s Ore") need their material substituted.
            textLines.add(componentLine(stack, component, info));
        }
        lines = textLines.toArray(new String[0]);
        stacks = icons.toArray(new ItemStack[0]);
    }

    private static String componentLine(ItemStack stack, ItemMatcher component, Pair<Vein, Quality> info) {
        StringBuilder line = new StringBuilder(stack.getHoverName().getString()).append(':');
        for (short[] yield : VeinCodec.getYieldInfo(component, info, chunk.dim)) {
            line.append(" <[").append(yield[1]).append("]-").append(VeinCodec.shortToPercentStr(yield[0]));
            if (yield[2] > 0) {
                line.append("; +").append(VeinCodec.shortToPercentStr(yield[2]));
            }
            line.append(">,");
        }
        line.deleteCharAt(line.length() - 1);
        return line.toString();
    }
}
