package com.flansmod.warforge.client.ui.model;

import com.flansmod.warforge.client.ui.UiNavigation;
import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.ClientClaimChunkCache;
import com.flansmod.warforge.client.ClientSiegeState;
import com.flansmod.warforge.client.map.ChunkMapTextureDaemon;
import com.flansmod.warforge.client.map.ChunkMapUtil;
import com.flansmod.warforge.client.util.SkinUtil;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.network.ClaimChunkInfo;
import com.flansmod.warforge.client.ui.model.ClaimChunkRenderInfo;
import com.flansmod.warforge.common.network.PacketClaimChunkAction;
import com.flansmod.warforge.common.network.PacketDeclareSiege;
import com.flansmod.warforge.common.network.PacketRequestTerrainColors;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import com.flansmod.warforge.client.ui.model.SiegeCampAttackInfoRender;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.compat.Ids;
import com.flansmod.warforge.compat.Keys;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ClaimMapModel {
    public static boolean siegeTargetMode = false;
    public static DimChunkPos siegeStartPickFor = null;

    public static boolean isRemoteSiegeView() {
        return siegeStartPickFor != null;
    }

    public static void resetSiegeState() {
        siegeTargetMode = false;
        siegeStartPickFor = null;
    }

    public static final String TEXTURE_NAMESPACE = "claimmap";

    private static final ResourceLocation ATTACK_ICON = Ids.of(Tags.MODID, "gui/icon_siege_attack.png");
    private static final ClaimChunkInfo WILDERNESS = new ClaimChunkInfo();

    private ClaimMapModel() {
    }

    public static final class Modes {
        public final boolean startPick;
        public final boolean target;
        public final boolean canDeclareSiege;

        private Modes(boolean startPick, boolean target, boolean canDeclareSiege) {
            this.startPick = startPick;
            this.target = target;
            this.canDeclareSiege = canDeclareSiege;
        }
    }

    public static Modes resolveModes(DimChunkPos center) {
        DimChunkPos siegePickFor = ClaimMapModel.siegeStartPickFor;
        boolean startPickMode = siegePickFor != null && siegePickFor.dim.equals(center.dim)
                && siegePickFor.x == center.x && siegePickFor.z == center.z;
        if (siegePickFor != null && !startPickMode) {
            ClaimMapModel.siegeStartPickFor = null;
        }
        boolean targetMode = ClaimMapModel.siegeTargetMode && !startPickMode;
        boolean canDeclareSiege = WarForgeConfig.SIEGE_ALLOW_UI_DECLARE && ClientClaimChunkCache.playerHasFaction();
        return new Modes(startPickMode, targetMode, canDeclareSiege);
    }

    public static void requestTextures(DimChunkPos center, int radius) {
        Long2IntOpenHashMap tintByChunk = new Long2IntOpenHashMap();
        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                ClaimChunkInfo info = ClientClaimChunkCache.get(center.dim, x, z);
                if (info != null && !info.factionId.equals(FactionIds.NONE)) {
                    tintByChunk.put(ChunkMapUtil.key(x, z), info.colour);
                }
            }
        }
        ChunkMapTextureDaemon.requestMapUpdate(TEXTURE_NAMESPACE, center.dim, center.x, center.z, radius, tintByChunk);
    }

    public static String title(Modes modes) {
        return I18n.get(modes.startPick ? "warforge.client.map.title.siege_origin"
                : modes.target ? "warforge.client.map.title.siege_target" : "warforge.client.map.title.territory");
    }

    public static String centerLine(DimChunkPos center, int radius) {
        return I18n.get("warforge.client.map.center", center.x, center.z, radius, Keys.id(center.dim));
    }

    public static String claimsLine() {
        String claimCap = ClientClaimChunkCache.claimMax() == Short.MAX_VALUE
                ? I18n.get("warforge.client.map.unlimited") : String.valueOf(ClientClaimChunkCache.claimMax());
        return I18n.get("warforge.client.map.claims", ClientClaimChunkCache.claimCount(), claimCap,
                ClientClaimChunkCache.forceLoadedCount(), ClientClaimChunkCache.forceLoadedMax());
    }

    public static String legend(Modes modes) {
        return I18n.get(modes.startPick ? "warforge.client.map.legend.origin"
                : modes.target ? "warforge.client.map.legend.target" : "warforge.client.map.legend.manage");
    }

    public static void declareSiege(DimChunkPos center, int radius, int pageX, int pageZ) {
        ClaimMapModel.siegeTargetMode = true;
        UiNavigation.openClaimManager(center, radius, pageX, pageZ);
    }

    public static void cancelTarget(DimChunkPos center, int radius, int pageX, int pageZ) {
        ClaimMapModel.resetSiegeState();
        UiNavigation.openClaimManager(center, radius, pageX, pageZ);
    }

    public static void cancelStartPick() {
        ClaimMapModel.resetSiegeState();
        ClientCompat.setScreen(null);
    }

    public static final class Cell {
        public final int chunkX;
        public final int chunkZ;
        public final ClaimChunkInfo info;
        public final SiegeCampAttackInfoRender renderInfo;
        public final boolean[] adjacency;
        public final String textureName;

        private Modes tooltipModes;
        private List<Component> tooltip;

        private Cell(int chunkX, int chunkZ, ClaimChunkInfo info, SiegeCampAttackInfoRender renderInfo, boolean[] adjacency, String textureName) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.info = info;
            this.renderInfo = renderInfo;
            this.adjacency = adjacency;
            this.textureName = textureName;
        }
    }

    public static Cell cell(DimChunkPos center, int radius, int i, int j, Modes modes) {
        int chunkX = center.x - radius + i;
        int chunkZ = center.z - radius + j;
        ClaimChunkInfo info = liveInfo(center.dim, chunkX, chunkZ);
        SiegeCampAttackInfo baseInfo = toMapState(info, chunkX - center.x, chunkZ - center.z);
        SiegeCampAttackInfoRender renderInfo = buildRenderInfo(center, baseInfo, info, chunkX, chunkZ, modes.startPick);
        boolean[] adjacency = computeAdjacency(center.dim, center.x, center.z, radius, chunkX, chunkZ);
        String texName = ChunkMapTextureDaemon.getTextureName(TEXTURE_NAMESPACE, center.dim, chunkX, chunkZ);
        return new Cell(chunkX, chunkZ, info, renderInfo, adjacency, texName);
    }

    public static void click(DimChunkPos center, int radius, Cell cell, Modes modes, int button) {
        Minecraft mc = Minecraft.getInstance();
        if (modes.startPick) {
            handleStartPick(center, cell.chunkX, cell.chunkZ);
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

    public static List<Component> tooltip(DimChunkPos center, Cell cell, Modes modes) {
        if (cell.tooltip == null || cell.tooltipModes != modes) {
            cell.tooltip = buildTooltip(center, cell, modes);
            cell.tooltipModes = modes;
        }
        return cell.tooltip;
    }

    private static List<Component> buildTooltip(DimChunkPos center, Cell cell, Modes modes) {
        ClaimChunkInfo info = cell.info;
        List<Component> lines = new ArrayList<>();
        if (modes.startPick) {
            lines.add(Component.translatable(cell.chunkX == center.x && cell.chunkZ == center.z
                    ? "warforge.client.map.tip.siege_target" : "warforge.client.map.tip.launch_here"));
        } else if (modes.target) {
            boolean enemy = !info.factionId.equals(FactionIds.NONE)
                    && !info.factionId.equals(ClientClaimChunkCache.playerFactionId());
            lines.add(Component.translatable(enemy ? "warforge.client.map.tip.target_enemy" : "warforge.client.map.tip.not_enemy"));
        }
        if (info.factionId.equals(FactionIds.NONE)) {
            lines.add(Component.translatable("warforge.client.map.tip.wilderness"));
        } else {
            lines.add(Component.translatable("warforge.client.map.tip.faction", info.factionName));
        }
        lines.add(Component.translatable("warforge.client.map.tip.claim_type", UiText.formatClaimType(info.claimType)));
        lines.add(Component.translatable("warforge.client.map.tip.chunk", cell.chunkX, cell.chunkZ));
        if (info.vein != null) {
            lines.add(Component.translatable("warforge.client.map.tip.ore", UiText.veinLabel(info.vein, info.oreQuality)));
        } else {
            lines.add(Component.translatable("warforge.client.map.tip.no_ore"));
        }
        if (info.hasFlag(ClaimChunkInfo.FLAG_FORCE_LOADED)) {
            lines.add(Component.translatable("warforge.client.map.tip.force_loaded"));
        }
        if (info.hasFlag(ClaimChunkInfo.FLAG_HAS_COLLECTOR)) {
            lines.add(Component.translatable("warforge.client.map.tip.collector"));
        }
        if (info.hasFlag(ClaimChunkInfo.FLAG_CAN_CLAIM)) {
            lines.add(Component.translatable("warforge.client.map.tip.claim"));
        }
        if (info.hasFlag(ClaimChunkInfo.FLAG_CAN_UNCLAIM)) {
            lines.add(Component.translatable("warforge.client.map.tip.unclaim"));
        }
        if (info.hasFlag(ClaimChunkInfo.FLAG_CAN_TOGGLE_FORCELOAD)) {
            lines.add(Component.translatable(info.hasFlag(ClaimChunkInfo.FLAG_FORCE_LOADED)
                    ? "warforge.client.map.tip.unforce" : "warforge.client.map.tip.force"));
        }
        return lines;
    }

    private static SiegeCampAttackInfo toMapState(ClaimChunkInfo info, int offsetX, int offsetZ) {
        SiegeCampAttackInfo state = new SiegeCampAttackInfo();
        state.canAttack = false;
        state.offset = new Vec3i(offsetX, 0, offsetZ);
        state.factionUuid = info.factionId;
        state.factionName = info.factionName;
        state.factionColour = info.colour;
        state.warforgeVein = info.vein;
        state.oreQuality = info.oreQuality;
        return state;
    }

    private static SiegeCampAttackInfoRender buildRenderInfo(DimChunkPos center, SiegeCampAttackInfo baseInfo, ClaimChunkInfo info,
                                                             int chunkX, int chunkZ, boolean startPickMode) {
        boolean isTarget = chunkX == center.x && chunkZ == center.z;
        boolean battleZone = ClientSiegeState.inBattleZone(center.dim, chunkX, chunkZ);
        boolean highlight = startPickMode ? !isTarget : battleZone;
        ResourceLocation centerIcon;
        SiegeCampAttackInfoRender.CenterMarkType centerIconType;
        if (startPickMode) {
            centerIcon = isTarget ? ATTACK_ICON : null;
            centerIconType = SiegeCampAttackInfoRender.CenterMarkType.CUSTOM_TEXTURE;
        } else {
            Minecraft mc = Minecraft.getInstance();
            centerIcon = isTarget && mc.player != null ? SkinUtil.getPlayerFace(mc.player.getUUID()) : null;
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

    private static ClaimChunkInfo liveInfo(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        ClaimChunkInfo info = ClientClaimChunkCache.get(dim, chunkX, chunkZ);
        return info != null ? info : WILDERNESS;
    }

    private static boolean[] computeAdjacency(ResourceKey<Level> dim, int centerX, int centerZ, int radius, int chunkX, int chunkZ) {
        boolean[] adjacency = new boolean[4];
        ClaimChunkInfo current = liveInfo(dim, chunkX, chunkZ);
        if (chunkZ > centerZ - radius) {
            adjacency[0] = !current.factionId.equals(liveInfo(dim, chunkX, chunkZ - 1).factionId);
        }
        if (chunkX < centerX + radius) {
            adjacency[1] = !current.factionId.equals(liveInfo(dim, chunkX + 1, chunkZ).factionId);
        }
        if (chunkZ < centerZ + radius) {
            adjacency[2] = !current.factionId.equals(liveInfo(dim, chunkX, chunkZ + 1).factionId);
        }
        if (chunkX > centerX - radius) {
            adjacency[3] = !current.factionId.equals(liveInfo(dim, chunkX - 1, chunkZ).factionId);
        }
        return adjacency;
    }

    private static void handleTargetPick(DimChunkPos center, int radius, ClaimChunkInfo info, int chunkX, int chunkZ) {
        if (info.factionId.equals(FactionIds.NONE) || info.factionId.equals(ClientClaimChunkCache.playerFactionId())) {
            status("warforge.client.map.pick_enemy");
            return;
        }
        DimChunkPos target = new DimChunkPos(center.dim, chunkX, chunkZ);
        ClaimMapModel.siegeTargetMode = false;
        ClaimMapModel.siegeStartPickFor = target;
        int range = Math.min(WarForgeConfig.SIEGE_DECLARE_MAX_RANGE, WarForgeConfig.CLAIM_MANAGER_RADIUS);
        PacketRequestTerrainColors terrainReq = new PacketRequestTerrainColors();
        terrainReq.center = target;
        terrainReq.radius = range;
        WarForgeMod.NETWORK.sendToServer(terrainReq);
        UiNavigation.openClaimManager(target, range, -1, -1);
    }

    private static void handleStartPick(DimChunkPos center, int chunkX, int chunkZ) {
        DimChunkPos target = ClaimMapModel.siegeStartPickFor;
        if (target == null) {
            return;
        }
        if (chunkX == target.x && chunkZ == target.z && center.dim.equals(target.dim)) {
            status("warforge.client.map.pick_other");
            return;
        }
        PacketDeclareSiege packet = new PacketDeclareSiege();
        packet.targetChunk = target;
        packet.fromChunk = new DimChunkPos(center.dim, chunkX, chunkZ);
        WarForgeMod.NETWORK.sendToServer(packet);
        ClaimMapModel.resetSiegeState();
        ClientCompat.setScreen(null);
    }

    private static void status(String langKey) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            ClientCompat.overlayMessage(mc.player, Component.translatable(langKey));
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
