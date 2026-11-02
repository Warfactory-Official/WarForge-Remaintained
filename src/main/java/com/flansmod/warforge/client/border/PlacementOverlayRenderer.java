package com.flansmod.warforge.client.border;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.ClientBorderCache;
import com.flansmod.warforge.client.render.BorderGpu;
import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.network.ClaimChunkInfo;
import com.flansmod.warforge.compat.Ids;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;

public final class PlacementOverlayRenderer {
    private static final ResourceLocation OVERLAY = Ids.of(Tags.MODID, "world/overlay.png");
    private static final int VALID = 0xFF00FF00;
    private static final int INVALID = 0xFFFF0000;
    private static final float PLANE_HEIGHT_ABOVE_FEET = 1.5f;
    private static final float CHUNK = 16f;

    private static final Matrix4f SCRATCH_MODEL_VIEW = new Matrix4f();
    private static BorderGpu.Mesh validMesh;
    private static BorderGpu.Mesh invalidMesh;

    private PlacementOverlayRenderer() {
    }

    public static void reset() {
        if (validMesh != null) {
            BorderGpu.close(validMesh);
            validMesh = null;
        }
        if (invalidMesh != null) {
            BorderGpu.close(invalidMesh);
            invalidMesh = null;
        }
    }

    public static void render(Minecraft mc, Player player, Matrix4f viewMatrix, double camX, double camY, double camZ) {
        if (!(player.getMainHandItem().getItem() instanceof BlockItem blockItem)) {
            return;
        }
        Block held = blockItem.getBlock();
        boolean citadel = held == Content.citadelBlock;
        if (!citadel && held != Content.basicClaimBlock && held != Content.reinforcedClaimBlock) {
            return;
        }

        int chunkX = player.blockPosition().getX() >> 4;
        int chunkZ = player.blockPosition().getZ() >> 4;
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = blockHit.getBlockPos();
            Direction face = blockHit.getDirection();
            chunkX = hitPos.getX() + face.getStepX() >> 4;
            chunkZ = hitPos.getZ() + face.getStepZ() >> 4;
        }

        ResourceKey<Level> dim = player.level().dimension();
        ClaimChunkInfo info = ClientBorderCache.get(dim, chunkX, chunkZ);
        boolean canPlace = citadel
                ? info == null || !FactionIds.isValid(info.factionId)
                : info != null && info.hasFlag(ClaimChunkInfo.FLAG_CAN_CLAIM);

        BorderGpu.Mesh mesh;
        if (canPlace) {
            mesh = validMesh != null ? validMesh : (validMesh = buildQuad(VALID));
        } else {
            mesh = invalidMesh != null ? invalidMesh : (invalidMesh = buildQuad(INVALID));
        }
        if (mesh == null) {
            return;
        }

        float planeY = (int) player.getY() + PLANE_HEIGHT_ABOVE_FEET;
        SCRATCH_MODEL_VIEW.set(viewMatrix)
                .translate((float) (chunkX * 16 - camX), (float) (planeY - camY), (float) (chunkZ * 16 - camZ));
        BorderGpu.draw(mesh, OVERLAY, SCRATCH_MODEL_VIEW);
    }

    private static BorderGpu.Mesh buildQuad(int argb) {
        return BorderGpu.build(null, sink -> {
            Matrix4f identity = new Matrix4f();
            sink.vertex(identity, 0f, 0f, 0f, argb, 0f, 0f);
            sink.vertex(identity, CHUNK, 0f, 0f, argb, CHUNK, 0f);
            sink.vertex(identity, CHUNK, 0f, CHUNK, argb, CHUNK, CHUNK);
            sink.vertex(identity, 0f, 0f, CHUNK, argb, 0f, CHUNK);
        });
    }
}
