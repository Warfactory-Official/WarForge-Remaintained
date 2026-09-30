package com.flansmod.warforge.client.ui.model;

import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.Tags;
import com.flansmod.warforge.api.modularui.ChunkMapTextureDaemon;
import com.flansmod.warforge.api.modularui.ChunkMapUtil;
import com.flansmod.warforge.client.ClientClaimChunkCache;
import com.flansmod.warforge.client.ClientProxy;
import com.flansmod.warforge.client.util.SkinUtil;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.factories.ClaimManagerGuiFactory;
import com.flansmod.warforge.common.network.ClaimChunkInfo;
import com.flansmod.warforge.common.network.ClaimChunkRenderInfo;
import com.flansmod.warforge.common.network.PacketClaimChunkAction;
import com.flansmod.warforge.common.network.PacketDeclareSiege;
import com.flansmod.warforge.common.network.PacketRequestTerrainColors;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import com.flansmod.warforge.common.network.SiegeCampAttackInfoRender;
import com.flansmod.warforge.common.network.SiegeCampProgressInfo;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.compat.Ids;
import com.flansmod.warforge.compat.Keys;
import com.flansmod.warforge.server.Faction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Data and actions of the claim manager (territory map) screen, independent of the UI library: the map modes,
 * the cell data, the tooltips and what a click does.
 */
@OnlyIn(Dist.CLIENT)
public final class ClaimMapModel {
    public static final String TEXTURE_NAMESPACE = "claimmap";

    private ClaimMapModel() {
    }

    /** The selection mode of the map. */
    public static final class Modes {
        /** Picking the chunk to launch a siege from (the center is the target). */
        public final boolean startPick;
        /** Picking the enemy claim to siege. */
        public final boolean target;
        /** The player may declare a siege from the map. */
        public final boolean canDeclareSiege;

        private Modes(boolean startPick, boolean target, boolean canDeclareSiege) {
            this.startPick = startPick;
            this.target = target;
            this.canDeclareSiege = canDeclareSiege;
        }
    }

    /** Resolves the mode for a map centred at center (clears a stale siege origin pick). */
    public static Modes resolveModes(DimChunkPos center) {
        DimChunkPos siegePickFor = ClaimManagerGuiFactory.siegeStartPickFor;
        boolean startPickMode = siegePickFor != null && siegePickFor.dim.equals(center.dim)
                && siegePickFor.x == center.x && siegePickFor.z == center.z;
        if (siegePickFor != null && !startPickMode) {
            ClaimManagerGuiFactory.siegeStartPickFor = null;
        }
        boolean targetMode = ClaimManagerGuiFactory.siegeTargetMode && !startPickMode;
        boolean canDeclareSiege = WarForgeConfig.SIEGE_ALLOW_UI_DECLARE
                && !ClientClaimChunkCache.playerFactionId.equals(Faction.nullUuid);
        return new Modes(startPickMode, targetMode, canDeclareSiege);
    }

    /** Requests the terrain textures of the map, tinted with the claim colours. */
    public static void requestTextures(DimChunkPos center, int radius) {
        HashMap<Long, Integer> tintByChunk = new HashMap<>();
        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                ClaimChunkInfo info = ClientClaimChunkCache.get(new DimChunkPos(center.dim, x, z));
                if (info != null && !info.factionId.equals(Faction.nullUuid)) {
                    tintByChunk.put(ChunkMapUtil.key(x, z), info.colour);
                }
            }
        }
        ChunkMapTextureDaemon.requestMapUpdate(TEXTURE_NAMESPACE, center.dim, center.x, center.z, radius, tintByChunk);
    }

    public static String title(Modes modes) {
        return modes.startPick ? "Choose Siege Origin" : modes.target ? "Select Siege Target" : "Territory Map";
    }

    public static String centerLine(DimChunkPos center, int radius) {
        return "Center [" + center.x + ", " + center.z + "] | Radius " + radius + " | Dim " + Keys.id(center.dim);
    }

    public static String claimsLine() {
        String claimCap = ClientClaimChunkCache.claimMax == Short.MAX_VALUE ? "INF" : String.valueOf(ClientClaimChunkCache.claimMax);
        return "Claims " + ClientClaimChunkCache.claimCount + "/" + claimCap
                + " | Loaded " + ClientClaimChunkCache.forceLoadedCount + "/" + ClientClaimChunkCache.forceLoadedMax;
    }

    public static String legend(Modes modes) {
        return modes.startPick
                ? "Click a chunk to launch the siege from. This consumes one siege camp block."
                : modes.target
                ? "Click an enemy claim to target it, then pick where to attack from."
                : "Left click claim/unclaim. Right click toggles force-loading.";
    }

    /** "Declare Siege": switches the map to target selection. */
    public static void declareSiege(DimChunkPos center, int radius, int pageX, int pageZ) {
        ClaimManagerGuiFactory.siegeTargetMode = true;
        ClaimManagerGuiFactory.INSTANCE.openClient(center, radius, pageX, pageZ);
    }

    /** "Cancel" in target selection: back to the normal map. */
    public static void cancelTarget(DimChunkPos center, int radius, int pageX, int pageZ) {
        ClaimManagerGuiFactory.resetSiegeState();
        ClaimManagerGuiFactory.INSTANCE.openClient(center, radius, pageX, pageZ);
    }

    /** "Cancel" in origin selection: closes the map. */
    public static void cancelStartPick() {
        ClaimManagerGuiFactory.resetSiegeState();
        ClientCompat.setScreen(null);
    }

    /** One chunk of the map. */
    public static final class Cell {
        public final int chunkX;
        public final int chunkZ;
        public final ClaimChunkInfo info;
        public final SiegeCampAttackInfoRender renderInfo;
        public final boolean[] adjacency;
        public final String textureName;

        private Cell(int chunkX, int chunkZ, ClaimChunkInfo info, SiegeCampAttackInfoRender renderInfo, boolean[] adjacency, String textureName) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.info = info;
            this.renderInfo = renderInfo;
            this.adjacency = adjacency;
            this.textureName = textureName;
        }
    }

    /** The cell at grid position (i, j) of a map of the given radius around center. */
    public static Cell cell(DimChunkPos center, int radius, int i, int j, Modes modes) {
        Minecraft mc = Minecraft.getInstance();
        int chunkX = center.x - radius + i;
        int chunkZ = center.z - radius + j;
        ClaimChunkInfo info = getLiveInfo(center.dim, chunkX, chunkZ);
        SiegeCampAttackInfo baseInfo = toMapState(info, center.x, center.z);
        SiegeCampAttackInfoRender renderInfo = buildRenderInfo(center, baseInfo, info, chunkX, chunkZ, modes.startPick, mc);
        boolean[] adjacency = computeAdjacency(center.dim, center.x, center.z, radius, chunkX, chunkZ);
        String texName = ChunkMapTextureDaemon.getTextureName(TEXTURE_NAMESPACE, center.dim, chunkX, chunkZ);
        return new Cell(chunkX, chunkZ, info, renderInfo, adjacency, texName);
    }

    /** A click with the mouse button on a cell. */
    public static void click(DimChunkPos center, int radius, Cell cell, Modes modes, int button) {
        Minecraft mc = Minecraft.getInstance();
        if (modes.startPick) {
            handleStartPick(center, cell.chunkX, cell.chunkZ, mc);
            return;
        }
        if (modes.target) {
            handleTargetPick(center, radius, cell.info, cell.chunkX, cell.chunkZ);
            return;
        }
        byte action = determineAction(cell.info, button);
        if (action == -1) {
            return;
        }
        PacketClaimChunkAction packet = new PacketClaimChunkAction();
        packet.action = action;
        packet.chunk = new DimChunkPos(center.dim, cell.chunkX, cell.chunkZ);
        packet.center = center;
        packet.radius = radius;
        WarForgeMod.NETWORK.sendToServer(packet);
    }

    /** The hover tooltip of a cell. */
    public static List<Component> tooltip(DimChunkPos center, Cell cell, Modes modes) {
        ClaimChunkInfo info = cell.info;
        int chunkX = cell.chunkX;
        int chunkZ = cell.chunkZ;
        List<Component> lines = new ArrayList<>();
        if (modes.startPick) {
            if (chunkX == center.x && chunkZ == center.z) {
                lines.add(Component.literal("Siege target"));
            } else {
                lines.add(Component.literal("Click to launch the siege from here"));
            }
        } else if (modes.target) {
            boolean enemy = !info.factionId.equals(Faction.nullUuid)
                    && !info.factionId.equals(ClientClaimChunkCache.playerFactionId);
            lines.add(Component.literal(enemy ? "Click to target this claim for a siege" : "Not a siege-able enemy claim"));
        }
        if (info.factionId.equals(Faction.nullUuid)) {
            lines.add(Component.literal("Wilderness"));
        } else {
            lines.add(Component.literal("Faction: " + info.factionName));
        }
        lines.add(Component.literal("Claim Type: " + UiText.formatClaimType(info.claimType)));
        lines.add(Component.literal("Chunk: [" + chunkX + ", " + chunkZ + "]"));
        if (info.vein != null) {
            if (info.oreQuality != null) {
                lines.add(Component.literal("Ore: " + UiText.translateVeinName(info.vein.translationKey,
                        I18n.get(info.oreQuality.getTranslationKey()) + " [" +
                                info.oreQuality.getMultString(info.vein) + "]")));
            } else {
                lines.add(Component.literal("Ore: " + UiText.translateVeinName(info.vein.translationKey)));
            }
        } else {
            lines.add(Component.literal("No ores in this chunk"));
        }
        if (info.hasFlag(ClaimChunkInfo.FLAG_FORCE_LOADED)) {
            lines.add(Component.literal("Force-loaded"));
        }
        if (info.hasFlag(ClaimChunkInfo.FLAG_HAS_COLLECTOR)) {
            lines.add(Component.literal("Has Faction Yield Storage"));
        }
        if (info.hasFlag(ClaimChunkInfo.FLAG_CAN_CLAIM)) {
            lines.add(Component.literal("Left click to claim"));
        }
        if (info.hasFlag(ClaimChunkInfo.FLAG_CAN_UNCLAIM)) {
            lines.add(Component.literal("Left click to unclaim"));
        }
        if (info.hasFlag(ClaimChunkInfo.FLAG_CAN_TOGGLE_FORCELOAD)) {
            lines.add(Component.literal(info.hasFlag(ClaimChunkInfo.FLAG_FORCE_LOADED)
                    ? "Right click to unforce-load" : "Right click to force-load"));
        }
        return lines;
    }

    private static SiegeCampAttackInfo toMapState(ClaimChunkInfo info, int centerX, int centerZ) {
        SiegeCampAttackInfo state = new SiegeCampAttackInfo();
        state.canAttack = false;
        state.mOffset = new Vec3i(info.x - centerX, 0, info.z - centerZ);
        state.mFactionUUID = info.factionId;
        state.mFactionName = info.factionName;
        state.mFactionColour = info.colour;
        state.mWarforgeVein = info.vein;
        state.mOreQuality = info.oreQuality;
        return state;
    }

    private static SiegeCampAttackInfoRender buildRenderInfo(DimChunkPos center, SiegeCampAttackInfo baseInfo, ClaimChunkInfo info, int chunkX, int chunkZ, boolean startPickMode, Minecraft mc) {
        boolean isTarget = chunkX == center.x && chunkZ == center.z;
        boolean battleZone = isBattleZoneChunk(center.dim, chunkX, chunkZ);
        boolean highlight = startPickMode ? !isTarget : battleZone;
        ResourceLocation centerIcon;
        SiegeCampAttackInfoRender.CenterMarkType centerIconType;
        if (startPickMode) {
            centerIcon = isTarget ? Ids.of(Tags.MODID, "gui/icon_siege_attack.png") : null;
            centerIconType = SiegeCampAttackInfoRender.CenterMarkType.CUSTOM_TEXTURE;
        } else {
            centerIcon = (isTarget && mc.player != null) ? SkinUtil.getPlayerFace(mc.player.getUUID()) : null;
            centerIconType = SiegeCampAttackInfoRender.CenterMarkType.PLAYER_FACE;
        }
        return new ClaimChunkRenderInfo(
                baseInfo,
                info.claimType,
                info.hasFlag(ClaimChunkInfo.FLAG_FORCE_LOADED),
                info.outlineStyle == ClaimChunkInfo.OUTLINE_CONQUERED,
                highlight,
                centerIcon,
                centerIconType
        );
    }

    private static boolean isBattleZoneChunk(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        ChunkPos target = new ChunkPos(chunkX, chunkZ);
        for (SiegeCampProgressInfo siegeInfo : ClientProxy.sSiegeInfo.values()) {
            if (siegeInfo == null || siegeInfo.attackingPos == null || !siegeInfo.attackingPos.dim.equals(dim) || siegeInfo.warzoneChunks == null) {
                continue;
            }
            if (siegeInfo.warzoneChunks.contains(target)) {
                return true;
            }
        }
        return false;
    }

    private static ClaimChunkInfo getLiveInfo(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        ClaimChunkInfo info = ClientClaimChunkCache.get(new DimChunkPos(dim, chunkX, chunkZ));
        if (info != null) {
            return info;
        }
        ClaimChunkInfo fallback = new ClaimChunkInfo();
        fallback.x = chunkX;
        fallback.z = chunkZ;
        return fallback;
    }

    private static boolean[] computeAdjacency(ResourceKey<Level> dim, int centerX, int centerZ, int radius, int chunkX, int chunkZ) {
        boolean[] adjacency = new boolean[4];
        ClaimChunkInfo current = getLiveInfo(dim, chunkX, chunkZ);
        if (chunkZ > centerZ - radius) {
            adjacency[0] = !current.factionId.equals(getLiveInfo(dim, chunkX, chunkZ - 1).factionId);
        }
        if (chunkX < centerX + radius) {
            adjacency[1] = !current.factionId.equals(getLiveInfo(dim, chunkX + 1, chunkZ).factionId);
        }
        if (chunkZ < centerZ + radius) {
            adjacency[2] = !current.factionId.equals(getLiveInfo(dim, chunkX, chunkZ + 1).factionId);
        }
        if (chunkX > centerX - radius) {
            adjacency[3] = !current.factionId.equals(getLiveInfo(dim, chunkX - 1, chunkZ).factionId);
        }
        return adjacency;
    }

    private static void handleTargetPick(DimChunkPos center, int radius, ClaimChunkInfo info, int chunkX, int chunkZ) {
        if (info.factionId.equals(Faction.nullUuid) || info.factionId.equals(ClientClaimChunkCache.playerFactionId)) {
            status("Pick an enemy faction claim to siege", Minecraft.getInstance());
            return;
        }
        DimChunkPos target = new DimChunkPos(center.dim, chunkX, chunkZ);
        ClaimManagerGuiFactory.siegeTargetMode = false;
        ClaimManagerGuiFactory.siegeStartPickFor = target;
        int range = Math.min(WarForgeConfig.SIEGE_DECLARE_MAX_RANGE, WarForgeConfig.CLAIM_MANAGER_RADIUS);
        PacketRequestTerrainColors terrainReq = new PacketRequestTerrainColors();
        terrainReq.center = target;
        terrainReq.radius = range;
        WarForgeMod.NETWORK.sendToServer(terrainReq);
        ClaimManagerGuiFactory.INSTANCE.openClient(target, range, -1, -1);
    }

    private static void handleStartPick(DimChunkPos center, int chunkX, int chunkZ, Minecraft mc) {
        DimChunkPos target = ClaimManagerGuiFactory.siegeStartPickFor;
        if (target == null) {
            return;
        }
        if (chunkX == target.x && chunkZ == target.z && center.dim.equals(target.dim)) {
            status("Pick a chunk other than the target to launch from", mc);
            return;
        }
        PacketDeclareSiege packet = new PacketDeclareSiege();
        packet.targetChunk = target;
        packet.fromChunk = new DimChunkPos(center.dim, chunkX, chunkZ);
        WarForgeMod.NETWORK.sendToServer(packet);
        ClaimManagerGuiFactory.resetSiegeState();
        ClientCompat.setScreen(null);
    }

    private static void status(String message, Minecraft mc) {
        if (mc.player != null) {
            ClientCompat.overlayMessage(mc.player, Component.literal(message));
        }
    }

    private static byte determineAction(ClaimChunkInfo info, int mouseButton) {
        if (mouseButton == 1 && info.hasFlag(ClaimChunkInfo.FLAG_CAN_TOGGLE_FORCELOAD)) {
            return PacketClaimChunkAction.ACTION_TOGGLE_FORCELOAD;
        }
        if (info.hasFlag(ClaimChunkInfo.FLAG_CAN_CLAIM)) {
            return PacketClaimChunkAction.ACTION_CLAIM;
        }
        if (info.hasFlag(ClaimChunkInfo.FLAG_CAN_UNCLAIM)) {
            return PacketClaimChunkAction.ACTION_UNCLAIM;
        }
        if (info.hasFlag(ClaimChunkInfo.FLAG_CAN_TOGGLE_FORCELOAD)) {
            return PacketClaimChunkAction.ACTION_TOGGLE_FORCELOAD;
        }
        return -1;
    }
}
