package com.flansmod.warforge.server;

import com.flansmod.warforge.api.ObjectIntPair;
import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.common.Sounds;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.*;
import com.flansmod.warforge.common.effect.EffectDisband;
import com.flansmod.warforge.common.effect.EffectUpgrade;
import com.flansmod.warforge.common.network.*;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.common.util.TimeHelper;
import com.flansmod.warforge.server.Faction.PlayerData;
import com.flansmod.warforge.server.Faction.Role;
import com.mojang.authlib.GameProfile;
import lombok.Getter;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

import static com.flansmod.warforge.common.WarForgeMod.*;

public class FactionStorage {
    // SafeZone and WarZone
    public static UUID SAFE_ZONE_ID = Faction.createUUID("safezone");
    public static UUID WAR_ZONE_ID = Faction.createUUID("conquered zone");
    public static Faction SAFE_ZONE = null;
    public static Faction WAR_ZONE = null;
    private final HashMap<UUID, Faction> mFactions = new HashMap<>();
    // This map contains every single claim, including siege camps.
    // So if you take one of these and try to look it up in the faction, check their active sieges too
    private final HashMap<DimChunkPos, UUID> mClaims = new HashMap<>();
    // This is all the currently active sieges, keyed by the defending position
    @Getter
    private final HashMap<DimChunkPos, Siege> sieges = new HashMap<>();
    private final Queue<DimChunkPos> finishedSiegeQueue = new ArrayDeque<>(4); //Array queue used due to small amount of data
    //This is all chunks that are under the "Grace" period
    public HashMap<DimChunkPos, ObjectIntPair<UUID>> conqueredChunks = new HashMap<>();
    private final HashMap<UUID, ArrayList<ItemStack>> redeemableInsuranceVaults = new HashMap<UUID, ArrayList<ItemStack>>();

    public FactionStorage() {
        InitNeutralZones();
    }

    public static boolean IsNeutralZone(UUID factionID) {
        return factionID.equals(SAFE_ZONE_ID) || factionID.equals(WAR_ZONE_ID);
    }

    // copy UUID's are used in the case that the UUID referred to are nulled at some point, which is not ideal for chunk protection
    public static UUID copyUUID(final UUID source) {
        return new UUID(source.getMostSignificantBits(), source.getLeastSignificantBits());
    }

    public static boolean isValidFaction(Faction faction) {
        return faction != null && isValidFaction(faction.uuid);
    }

    public static boolean isValidFaction(UUID factionID) {
        return factionID != null && !factionID.equals(Faction.nullUuid);
    }

    // returns big endian (decreasing sig/ biggest sig first) array
    public static int[] UUIDToBEIntArray(UUID uniqueID) {
        return new int[]{
                (int) (uniqueID.getMostSignificantBits() >>> 32),
                (int) uniqueID.getMostSignificantBits(),
                (int) (uniqueID.getLeastSignificantBits() >>> 32),
                (int) uniqueID.getLeastSignificantBits()
        };
    }

    public static UUID BEIntArrayToUUID(int[] bigEndianArray) {
        // FUCKING SIGN EXTENSION
        return new UUID(
                ((bigEndianArray[0] & 0xFFFF_FFFFL) << 32) | (bigEndianArray[1] & 0xFFFF_FFFFL),
                ((bigEndianArray[2] & 0xFFFF_FFFFL) << 32) | (bigEndianArray[3] & 0xFFFF_FFFFL)
        );
    }

    private void InitNeutralZones() {
        SAFE_ZONE = new Faction();
        SAFE_ZONE.citadelPos = new DimBlockPos(0, 0, 0, 0); // Overworld origin
        SAFE_ZONE.colour = 0x00ff00;
        SAFE_ZONE.name = "SafeZone";
        SAFE_ZONE.uuid = SAFE_ZONE_ID;
        mFactions.put(SAFE_ZONE_ID, SAFE_ZONE);

        WAR_ZONE = new Faction();
        WAR_ZONE.citadelPos = new DimBlockPos(0, 0, 0, 0); // Overworld origin
        WAR_ZONE.colour = 0xff0000;
        WAR_ZONE.name = "WarZone";
        WAR_ZONE.uuid = WAR_ZONE_ID;
        mFactions.put(WAR_ZONE_ID, WAR_ZONE);
        // Note: We specifically do not put this data in the leaderboard
    }

    public boolean IsPlayerInFaction(UUID playerID, UUID factionID) {
        if (mFactions.containsKey(factionID))
            return mFactions.get(factionID).isPlayerInFaction(playerID);
        return false;
    }

    public boolean isPlayerDefending(UUID playerID) {
        Faction faction = getFactionOfPlayer(playerID);
        List<Siege> Sieges = new ArrayList<>(sieges.values());
        for (Siege siege : Sieges) {
            if (siege.defendingFaction == faction.uuid) {
                return true;
            }
        }

        return false;
    }

    public long getPlayerCooldown(UUID playerID) {
        return mFactions.get(playerID).members.get(playerID).moveFlagCooldown;
    }

    public HashMap<DimChunkPos, UUID> getClaims() {
        return mClaims;
    }

    public Collection<Faction> getAllFactions() {
        return mFactions.values();
    }

    public boolean IsPlayerRoleInFaction(UUID playerID, UUID factionID, Faction.Role role) {
        if (mFactions.containsKey(factionID))
            return mFactions.get(factionID).isPlayerRoleInFaction(playerID, role);
        return false;
    }

    public Faction getFaction(UUID factionID) {
        if (factionID == null) {
            return null;
        }  // fixes NPE when clicking on random chunk to begin siege

        if (factionID.equals(Faction.nullUuid))
            return null;

        if (mFactions.containsKey(factionID))
            return mFactions.get(factionID);

        LOGGER.error("Could not find a faction with UUID " + factionID);
        return null;
    }

    public Faction getFaction(String name) {
        for (HashMap.Entry<UUID, Faction> entry : mFactions.entrySet()) {
            if (entry.getValue().name.equals(name))
                return entry.getValue();
        }
        return null;
    }

    public Faction GetFactionWithOpenInviteTo(UUID playerID) {
        for (HashMap.Entry<UUID, Faction> entry : mFactions.entrySet()) {
            if (entry.getValue().isInvitingPlayer(playerID))
                return entry.getValue();
        }
        return null;
    }

    public String[] GetFactionNames() {
        String[] names = new String[mFactions.size()];
        int i = 0;
        for (HashMap.Entry<UUID, Faction> entry : mFactions.entrySet()) {
            names[i] = entry.getValue().name;
            i++;
        }
        return names;
    }

    private boolean validateFactionName(String factionName, ICommandSender sender, UUID existingFactionId) {
        if (factionName == null || factionName.isEmpty()) {
            sender.sendMessage(new TextComponentString("You can't create or rename a faction with no name"));
            return false;
        }

        if (factionName.length() > WarForgeConfig.FACTION_NAME_LENGTH_MAX) {
            sender.sendMessage(new TextComponentString("Name is too long, must be at most " + WarForgeConfig.FACTION_NAME_LENGTH_MAX + " characters"));
            return false;
        }

        for (int i = 0; i < factionName.length(); i++) {
            char c = factionName.charAt(i);
            if ('0' <= c && c <= '9') continue;
            if ('a' <= c && c <= 'z') continue;
            if ('A' <= c && c <= 'Z') continue;

            sender.sendMessage(new TextComponentString("Invalid character [" + c + "] in faction name"));
            return false;
        }

        String lowerName = factionName.toLowerCase(Locale.ROOT);
        for (String banned : WarForgeConfig.FACTION_NAME_BANLIST) {
            if (banned != null && !banned.trim().isEmpty() && lowerName.contains(banned.trim().toLowerCase(Locale.ROOT))) {
                sender.sendMessage(new TextComponentString("That faction name is not allowed"));
                return false;
            }
        }

        Faction existing = getFaction(factionName);
        if (existing != null && (existingFactionId == null || !existing.uuid.equals(existingFactionId))) {
            sender.sendMessage(new TextComponentString("A faction with the name " + factionName + " already exists"));
            return false;
        }

        return true;
    }

    // This is called for any non-citadel claim. Citadels can be factionless, so this makes no sense
    public void onNonCitadelClaimPlaced(IClaim claim, EntityLivingBase placer) {
        onNonCitadelClaimPlaced(claim, getFactionOfPlayer(placer.getUniqueID()));
    }

    public void onNonCitadelClaimPlaced(IClaim claim, Faction faction) {
        if (faction != null) {
            TileEntity tileEntity = claim.getAsTileEntity();
            boolean dataOnlyClaim = tileEntity instanceof TileEntityBasicClaim || tileEntity instanceof TileEntityReinforcedClaim;
            mClaims.put(claim.getClaimPos().toChunkPos(), faction.uuid);

            faction.messageAll(new TextComponentString("Claimed the chunk [" + claim.getClaimPos().toChunkPos().x + ", " + claim.getClaimPos().toChunkPos().z + "] around " + claim.getClaimPos().toFancyString()));

            if (!dataOnlyClaim) {
                claim.onServerSetFaction(faction);
            }
            faction.onClaimPlaced(claim);

            // Basic / reinforced claim blocks become data-only claims after placement.
            if (dataOnlyClaim) {
                World world = tileEntity.getWorld();
                DimBlockPos claimPos = claim.getClaimPos();
                world.setBlockToAir(claimPos.toRegularPos());
                world.setBlockToAir(claimPos.toRegularPos().up());
                world.setBlockToAir(claimPos.toRegularPos().up(2));
            }
        } else
            LOGGER.error("Invalid placer placed a claim at " + claim.getClaimPos());
    }

    public boolean requestClaimChunkNoTile(EntityPlayerMP player, DimChunkPos chunkPos) {
        Faction faction = getFactionOfPlayer(player.getUniqueID());
        if (faction == null) {
            player.sendMessage(new TextComponentString("You aren't in a faction"));
            return false;
        }
        if (!canClaimChunkNoTile(player, faction, chunkPos, true)) {
            return false;
        }

        ClaimedBlockSelection claimBlock = findFirstClaimBlock(player.inventory);
        if (claimBlock == null) {
            player.sendMessage(new TextComponentString("You need a basic or reinforced claim block in your inventory"));
            return false;
        }

        mClaims.put(chunkPos, faction.uuid);
        faction.claimNoTileEntity(chunkPos, player.getPosition().getY(), claimBlock.claimType);
        claimBlock.consume(player.inventory);
        faction.messageAll(new TextComponentString("Claimed the chunk [" + chunkPos.x + ", " + chunkPos.z + "]"));
        return true;
    }

    private boolean canClaimChunkNoTile(EntityPlayerMP player, Faction faction, DimChunkPos chunkPos, boolean notify) {
        if (!isOp(player) && !faction.isPlayerRoleInFaction(player.getUniqueID(), Faction.Role.OFFICER)) {
            if (notify) {
                player.sendMessage(new TextComponentString("You are not an officer of your faction"));
            }
            return false;
        }
        if (!getClaim(chunkPos).equals(Faction.nullUuid)) {
            if (notify) {
                player.sendMessage(new TextComponentString("This chunk already has a claim"));
            }
            return false;
        }
        if (!containsInt(WarForgeConfig.CLAIM_DIM_WHITELIST, chunkPos.dim)) {
            if (notify) {
                player.sendMessage(new TextComponentString("You cannot claim chunks in this dimension"));
            }
            return false;
        }
        if (WarForgeConfig.ENABLE_CITADEL_UPGRADES && !faction.canPlaceClaim()) {
            if (notify) {
                player.sendMessage(new TextComponentString("Your faction reached it's level's claim limit, upgrade the level to incrase the limit"));
            }
            return false;
        }
        if (!WarForgeConfig.ENABLE_ISOLATED_CLAIMS && BlockBasicClaim.hasAdjacent(chunkPos, faction) == null) {
            if (notify) {
                player.sendMessage(new TextComponentString("Isolated claims are disabled; you cannot put a claim here with no adjacent claims"));
            }
            return false;
        }

        // Prevent joining two collector-bearing islands into one.
        int collectorsInConnectedIslands = 0;
        HashSet<DimChunkPos> seen = new HashSet<DimChunkPos>();
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            DimChunkPos neighbor = chunkPos.Offset(facing, 1);
            if (!faction.uuid.equals(getClaim(neighbor)) || seen.contains(neighbor)) {
                continue;
            }

            Set<DimChunkPos> island = collectFactionIsland(faction.uuid, neighbor);
            seen.addAll(island);
            for (DimBlockPos collectorPos : faction.islandCollectors) {
                if (island.contains(collectorPos.toChunkPos())) {
                    collectorsInConnectedIslands++;
                    break;
                }
            }
        }
        if (collectorsInConnectedIslands > 1) {
            if (notify) {
                player.sendMessage(new TextComponentString("This claim would merge multiple islands that each already have a collector"));
            }
            return false;
        }

        ObjectIntPair<UUID> conqueredChunkInfo = conqueredChunks.get(chunkPos);
        if (conqueredChunkInfo != null && !Objects.equals(conqueredChunkInfo.getObj(), faction.uuid)) {
            if (notify) {
                Faction owner = getFaction(conqueredChunkInfo.getObj());
                String ownerName = owner == null ? "Unknown" : owner.name;
                player.sendMessage(new TextComponentTranslation("warforge.info.chunk_is_conquered", ownerName, TimeHelper.formatTime(conqueredChunkInfo.getInteger())));
            }
            return false;
        }

        return true;
    }

    public UUID getClaim(DimBlockPos pos) {
        return getClaim(pos.toChunkPos());
    }

    public UUID getClaim(DimChunkPos pos) {
        if (mClaims.containsKey(pos))
            return mClaims.get(pos);
        return Faction.nullUuid;
    }

    public Faction getFactionOfPlayer(UUID playerID) {
        for (HashMap.Entry<UUID, Faction> entry : mFactions.entrySet()) {
            if (entry.getValue().isPlayerInFaction(playerID))
                return entry.getValue();
        }
        return null;
    }

    public void update() {
        for (HashMap.Entry<UUID, Faction> entry : mFactions.entrySet()) {
            entry.getValue().update();
        }
    }

    public void updateConqueredChunks(long msUpdateTime) {
        int msPassed = (int) (msUpdateTime - previousUpdateTimestamp); // the difference is likely less than 596h (max time storage of int using ms)

        try {
            // concurrent modification exception can occur when conquered chunks is checked by other methods, such as pre block place
            for (DimChunkPos chunkPosKey : conqueredChunks.keySet()) {
                ObjectIntPair<UUID> chunkEntry = conqueredChunks.get(chunkPosKey);

                if (chunkEntry.getInteger() < msPassed) conqueredChunks.remove(chunkPosKey);
                else chunkEntry.setInteger(chunkEntry.getInteger() - msPassed);
            }
        } catch (Exception e) {
            LOGGER.atError().log("Error when updating conquered chunk of type " + e + ", with stacktrace:");
            e.printStackTrace();
        }
    }

    public void advanceSiegeDay() {
        for (HashMap.Entry<DimChunkPos, Siege> kvp : sieges.entrySet()) {
            kvp.getValue().AdvanceDay();
            if (kvp.getValue().isCompleted())
                finishedSiegeQueue.add(kvp.getKey());
        }

        processCompleteSieges();

        if (!WarForgeConfig.LEGACY_USES_YIELD_TIMER) {
            for (HashMap.Entry<UUID, Faction> entry : mFactions.entrySet()) {
                entry.getValue().increaseLegacy();
            }
        }
    }

    public void updateSiegeTimers() {
        for (HashMap.Entry<DimChunkPos, Siege> kvp : sieges.entrySet()) {
            kvp.getValue().updateSiegeTimer();
            if (kvp.getValue().isCompleted())
                finishedSiegeQueue.add(kvp.getKey());
        }
        processCompleteSieges();
    }

    public void advanceYieldDay() {
        for (HashMap.Entry<UUID, Faction> entry : mFactions.entrySet()) {
            entry.getValue().awardYields();
        }

        if (WarForgeConfig.LEGACY_USES_YIELD_TIMER) {
            for (HashMap.Entry<UUID, Faction> entry : mFactions.entrySet()) {
                entry.getValue().increaseLegacy();
            }
        }
    }

    public void playerDied(EntityPlayerMP playerWhoDied, DamageSource source) {
        if (source.getTrueSource() instanceof EntityPlayerMP killer) {
            Faction killedFac = getFactionOfPlayer(playerWhoDied.getUniqueID());
            Faction killerFac = getFactionOfPlayer(killer.getUniqueID());

            if (killedFac != null && killerFac != null) {
                for (HashMap.Entry<DimChunkPos, Siege> kvp : sieges.entrySet()) {
                    kvp.getValue().onPVPKill(killer, playerWhoDied);
                    if (kvp.getValue().isCompleted())
                        finishedSiegeQueue.add(kvp.getKey());
                }

                processCompleteSieges();
            }

            if (killerFac != null) {
                int numTimesKilled = 0;
                if (killerFac.killCounter.containsKey(playerWhoDied.getUniqueID())) {
                    numTimesKilled = killerFac.killCounter.get(playerWhoDied.getUniqueID()) + 1;
                    killerFac.killCounter.replace(playerWhoDied.getUniqueID(), numTimesKilled);
                } else {
                    numTimesKilled = 1;
                    killerFac.killCounter.put(playerWhoDied.getUniqueID(), numTimesKilled);
                }

                if (numTimesKilled <= WarForgeConfig.NOTORIETY_KILL_CAP_PER_PLAYER) {
                    if (killerFac != killedFac) {
                        source.getTrueSource().sendMessage(new TextComponentString("Killing " + playerWhoDied.getName() + " earned your faction " + WarForgeConfig.NOTORIETY_PER_PLAYER_KILL + " notoriety"));
                        killerFac.notoriety += WarForgeConfig.NOTORIETY_PER_PLAYER_KILL;
                    }
                } else {
                    source.getTrueSource().sendMessage(new TextComponentString("Your faction has already killed " + playerWhoDied.getName() + " " + numTimesKilled + " times. You will not become more notorious."));
                }
            }
        }
    }

    public void clearNotoriety() {
        for (HashMap.Entry<UUID, Faction> entry : mFactions.entrySet()) {
            entry.getValue().notoriety = 0;
        }
    }

    public void clearLegacy() {
        for (HashMap.Entry<UUID, Faction> entry : mFactions.entrySet()) {
            entry.getValue().legacy = 0;
        }
    }

    public synchronized void processCompleteSieges() {
        while (!finishedSiegeQueue.isEmpty()) {
            var siege = finishedSiegeQueue.poll();
            sieges.get(siege).finished = true;
            handleCompletedSiege(siege);
        }
    }

    // cleaner separation between action to be done on completed sieges and the determination of these sieges
    public void handleCompletedSiege(DimChunkPos chunkPos) {
        handleCompletedSiege(chunkPos, true);
    }

    // Forceful siege termination, called via privileged user
    public void handleCompletedSiege(DimChunkPos chunkPos, siegeTermination termType) {
        Siege siege = sieges.get(chunkPos);
        if (siege == null) {
            LOGGER.warn("Attempted to complete non-existent siege at {}", chunkPos);
            return;
        }

        Faction attackers = getFaction(siege.attackingFaction);
        Faction defenders = getFaction(siege.defendingFaction);

        if (attackers == null || defenders == null) {
            LOGGER.error("Invalid factions in completed siege. Nothing will happen.");
            return;
        }

        DimBlockPos blockPos = defenders.getSpecificPosForClaim(chunkPos);

        switch (termType) {
            case WIN -> {
                if (WarForgeConfig.ATTACKER_CONQUERED_CHUNK_PERIOD > 0) {
                    conqueredChunks.put(chunkPos, new ObjectIntPair<>(copyUUID(attackers.uuid), WarForgeConfig.ATTACKER_CONQUERED_CHUNK_PERIOD));
                    for (DimBlockPos siegeCampPos : siege.attackingCamps) {
                        if (siegeCampPos != null)
                            conqueredChunks.put(siegeCampPos.toChunkPos(), new ObjectIntPair<>(copyUUID(attackers.uuid), WarForgeConfig.ATTACKER_CONQUERED_CHUNK_PERIOD));
                    }
                }

                defenders.onClaimLost(blockPos, true);
                mClaims.remove(blockPos.toChunkPos());

                attackers.messageAll(new TextComponentTranslation("warforge.info.siege_won_attackers", attackers.name, blockPos.toFancyString()));
                defenders.messageAll(new TextComponentTranslation("warforge.info.siege_lost_defenders", defenders.name, blockPos.toFancyString()));
                attackers.notoriety += WarForgeConfig.NOTORIETY_PER_SIEGE_ATTACK_SUCCESS;

                if (WarForgeConfig.SIEGE_CAPTURE) {
                    MC_SERVER.getWorld(blockPos.dim).setBlockState(blockPos.toRegularPos(), Content.basicClaimBlock.getDefaultState());
                    TileEntity te = MC_SERVER.getWorld(blockPos.dim).getTileEntity(blockPos.toRegularPos());
                    onNonCitadelClaimPlaced((IClaim) te, attackers);
                }

                attackers.increaseSiegeMomentum(true);
                siege.onCompleted(true);
            }

            case LOSE -> {
                if (WarForgeConfig.DEFENDER_CONQUERED_CHUNK_PERIOD > 0) {
                    conqueredChunks.put(chunkPos, new ObjectIntPair<>(copyUUID(defenders.uuid), WarForgeConfig.DEFENDER_CONQUERED_CHUNK_PERIOD));
                    for (DimBlockPos siegeCampPos : siege.attackingCamps) {
                        if (siegeCampPos != null)
                            conqueredChunks.put(siegeCampPos.toChunkPos(), new ObjectIntPair<>(copyUUID(defenders.uuid), WarForgeConfig.DEFENDER_CONQUERED_CHUNK_PERIOD));
                    }
                }

                attackers.messageAll(new TextComponentTranslation("warforge.info.siege_lost_attackers", attackers.name, blockPos.toFancyString()));
                defenders.messageAll(new TextComponentTranslation("warforge.info.siege_won_defenders", defenders.name, blockPos.toFancyString()));
                defenders.notoriety += WarForgeConfig.NOTORIETY_PER_SIEGE_DEFEND_SUCCESS;

                attackers.stopMomentum(true);
                attackers.messageAll(new TextComponentTranslation("warforge.info_momentum_lost"));

                siege.onCompleted(false);
            }

            case NEUTRAL -> {
                attackers.messageAll(new TextComponentTranslation("warforge.info.siege_cancelled_attackers", attackers.name, blockPos.toFancyString()));
                defenders.messageAll(new TextComponentTranslation("warforge.info.siege_cancelled_defenders", defenders.name, blockPos.toFancyString()));
                siege.onCompleted(false); // tbh Idk what to put here
            }
        }

        WarForgeMod.FACTIONS.sendSiegeInfoToNearby(siege.defendingClaim.toChunkPos());
        sieges.remove(chunkPos);
    }

    // cleanup is done by failing, passing, or cancelling siege through the TE class. If called without boolean, it is assumed to not be from inside TE
    public void handleCompletedSiege(DimChunkPos chunkPos, boolean doCleanup) {
        Siege siege = sieges.get(chunkPos);

        Faction attackers = getFaction(siege.attackingFaction);
        Faction defenders = getFaction(siege.defendingFaction);
        if (attackers == null || defenders == null) {
            LOGGER.error("Invalid factions in completed siege. Nothing will happen.");
            return;
        }

        DimBlockPos blockPos = defenders.getSpecificPosForClaim(chunkPos);
        boolean successful = siege.WasSuccessful();
        if (successful) {
            if (WarForgeConfig.ATTACKER_CONQUERED_CHUNK_PERIOD > 0) {
                conqueredChunks.put(chunkPos, new ObjectIntPair<>(copyUUID(attackers.uuid), WarForgeConfig.ATTACKER_CONQUERED_CHUNK_PERIOD));
                for (DimBlockPos siegeCampPos : siege.attackingCamps)
                    if (siegeCampPos != null)
                        conqueredChunks.put(siegeCampPos.toChunkPos(), new ObjectIntPair<>(copyUUID(attackers.uuid), WarForgeConfig.ATTACKER_CONQUERED_CHUNK_PERIOD));
            }

            defenders.onClaimLost(blockPos, true); // drops block if SIEGE_CAPTURE is off (claim is not overriden), or drops nothing if it is on (claim effectively removed and instantly replaced)
            mClaims.remove(blockPos.toChunkPos());
            attackers.messageAll(new TextComponentTranslation("warforge.info.siege_won_attackers", attackers.name, blockPos.toFancyString()));
            defenders.messageAll(new TextComponentTranslation("warforge.info.siege_lost_defenders", defenders.name, blockPos.toFancyString()));
            attackers.notoriety += WarForgeConfig.NOTORIETY_PER_SIEGE_ATTACK_SUCCESS;
            if (WarForgeConfig.SIEGE_CAPTURE) {
                MC_SERVER.getWorld(blockPos.dim).setBlockState(blockPos.toRegularPos(), Content.basicClaimBlock.getDefaultState());
                TileEntity te = MC_SERVER.getWorld(blockPos.dim).getTileEntity(blockPos.toRegularPos());
                onNonCitadelClaimPlaced((IClaim) te, attackers);
            }
            attackers.increaseSiegeMomentum(true);
        } else {
            if (WarForgeConfig.DEFENDER_CONQUERED_CHUNK_PERIOD > 0) {
                conqueredChunks.put(chunkPos, new ObjectIntPair<>(copyUUID(defenders.uuid), WarForgeConfig.DEFENDER_CONQUERED_CHUNK_PERIOD)); // defenders get won claims defended
                for (DimBlockPos siegeCampPos : siege.attackingCamps)
                    if (siegeCampPos != null)
                        conqueredChunks.put(siegeCampPos.toChunkPos(), new ObjectIntPair<>(copyUUID(defenders.uuid), WarForgeConfig.DEFENDER_CONQUERED_CHUNK_PERIOD));
            }
            attackers.messageAll(new TextComponentTranslation("warforge.info.siege_lost_attackers", attackers.name, blockPos.toFancyString()));
            defenders.messageAll(new TextComponentTranslation("warforge.info.siege_won_defenders", defenders.name, blockPos.toFancyString()));
            defenders.notoriety += WarForgeConfig.NOTORIETY_PER_SIEGE_DEFEND_SUCCESS;
            attackers.stopMomentum(true);
        }

        if (doCleanup) siege.onCompleted(successful);

        // Then remove the siege
        sieges.remove(chunkPos);

        //Remove defending status from the faction
        for (Siege activeSiege : sieges.values()) {
            if (activeSiege.defendingFaction.equals(defenders.uuid))
                return;
        }
        defenders.isCurrentlyDefending = false;
    }

    public boolean requestCreateFaction(TileEntityCitadel citadel, EntityPlayer player, String factionName, int colour) {
        if (citadel == null) {
            player.sendMessage(new TextComponentString("You can't create a faction without a citadel"));
            return false;
        }

        if (!validateFactionName(factionName, player, null)) {
            return false;
        }

        Faction existingFaction = getFactionOfPlayer(player.getUniqueID());
        if (existingFaction != null) {
            player.sendMessage(new TextComponentString("You are already in a faction"));
            return false;
        }

        invalidateRedeemableInsuranceVault(player.getUniqueID(), player);

        UUID proposedID = Faction.createUUID(factionName);
        while (mFactions.containsKey(proposedID)) {
            proposedID = UUID.randomUUID();
        }

        if (colour == 0xffffff) {
            colour = Color.HSBtoRGB(rand.nextFloat(), rand.nextFloat() * 0.5f + 0.5f, 1.0f);
        }

        // All checks passed, create a faction
        Faction faction = new Faction();
        faction.onlinePlayerCount = 1;
        faction.uuid = proposedID;
        faction.name = factionName;
        faction.citadelPos = new DimBlockPos(citadel);
        faction.claims.put(faction.citadelPos, 0);
        faction.claimTypes.put(faction.citadelPos, Faction.ClaimType.CITADEL);
        faction.colour = colour;
        faction.notoriety = 0;
        faction.legacy = 0;
        faction.wealth = 0;

        mFactions.put(proposedID, faction);
        citadel.onServerCreateFaction(faction);
        mClaims.put(citadel.getClaimPos().toChunkPos(), proposedID);
        LEADERBOARD.RegisterFaction(faction);

        INSTANCE.messageAll(new TextComponentString(player.getName() + " created the faction " + factionName), true);

        faction.addPlayer(player.getUniqueID());
        faction.setLeader(player.getUniqueID());
        PacketNamePlateChange packet = new PacketNamePlateChange();
        packet.name = player.getName();
        packet.faction = factionName;
        packet.color = faction.colour;
        NETWORK.sendToAllAround(packet, player.posX, player.posY, player.posZ, 100, player.dimension);
        return true;
    }

    public boolean requestRenameFaction(ICommandSender sender, UUID factionId, String newName) {
        Faction faction = getFaction(factionId);
        if (faction == null) {
            sender.sendMessage(new TextComponentString("That faction doesn't exist"));
            return false;
        }
        if (!validateFactionName(newName, sender, faction.uuid)) {
            return false;
        }

        String oldName = faction.name;
        faction.name = newName;

        for (World world : MC_SERVER.worlds) {
            for (TileEntity te : world.loadedTileEntityList) {
                if (te instanceof TileEntityClaim claim && claim.getFaction().equals(faction.uuid)) {
                    claim.updateFactionName(newName);
                }
            }
        }

        ArrayList<EntityPlayer> onlinePlayers = faction.getOnlinePlayers(entityPlayer -> entityPlayer != null && entityPlayer.isEntityAlive());
        onlinePlayers.forEach(entityPlayer -> {
            PacketNamePlateChange packet = new PacketNamePlateChange();
            packet.name = entityPlayer.getName();
            packet.faction = newName;
            packet.color = faction.colour;
            NETWORK.sendToAllAround(packet, entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, 100, entityPlayer.dimension);
        });

        INSTANCE.messageAll(new TextComponentString("Faction " + oldName + " was renamed to " + newName), true);
        return true;
    }

    public boolean requestLevelUp(EntityPlayerMP officer, UUID factionID) {
        Faction faction = getFaction(factionID);
        if (faction == null) {
            officer.sendMessage(new TextComponentString("That faction doesn't exist"));
            return false;
        }

        if (!faction.isPlayerInFaction(officer.getUniqueID())) {
            officer.sendMessage(new TextComponentString("You are not in that faction"));
            return false;
        }
        if (!faction.isPlayerRoleInFaction(officer.getUniqueID(), Role.OFFICER) && !faction.isPlayerRoleInFaction(officer.getUniqueID(), Role.LEADER)) {
            officer.sendMessage(new TextComponentString("You must be officer or higher to upgrade citadel"));
            return false;
        }

        Map<StackComparable, Integer> requiredItems = (Map<StackComparable, Integer>) UPGRADE_HANDLER.getRequirementsFor(faction.citadelLevel + 1).clone();

        if (requiredItems == null) {
            officer.sendMessage(new TextComponentString("You cannot level up fruther"));
            return false;
        }

        List<ItemStack> invCopy = officer.inventory.mainInventory.stream()
                .map(ItemStack::copy)
                .collect(Collectors.toList());
        //Copy for safety
        boolean passed = true;

        outer:
        for (Map.Entry<StackComparable, Integer> entry : requiredItems.entrySet()) {
            StackComparable sc = entry.getKey();
            int required = entry.getValue();

            for (ItemStack stack : invCopy) {
                if (!stack.isEmpty() && sc.equals(stack)) {
                    int consumed = Math.min(required, stack.getCount());
                    required -= consumed;
                    stack.shrink(consumed);
                }
                if (required == 0) break;
            }

            if (required > 0) {
                passed = false;
                break;
            }
        }


        if (!passed || invCopy.size() != 36) { //Schizophrenia
            officer.sendMessage(new TextComponentString("Could not upgrade: you don't have the required items"));
            return false;
        }

        for (int i = 0; i < officer.inventory.mainInventory.size(); i++) {
            officer.inventory.mainInventory.set(i, invCopy.get(i));
        }

        faction.citadelLevel++;
        faction.messageAll(new TextComponentString(
                officer.getName() + " has upgraded your facition to level " +
                        faction.citadelLevel + "! You can now claim " +
                        UPGRADE_HANDLER.getClaimLimitForLevel(faction.citadelLevel) +
                        " chunks and force-load up to " + faction.getMaxForceLoadedChunks() + " chunks."));


        faction.soundEffectAll(Sounds.sfxUpgrade);
        EffectUpgrade.composeEffect(faction.citadelPos.dim, faction.citadelPos.toRegularPos().up(2), 100, 400, 1f, 0.2D, faction.colour, 10);
        return true;
    }

    public boolean requestChooseFactionFlag(EntityPlayerMP player, UUID factionID, String flagId) {
        Faction faction = getFaction(factionID);
        if (faction == null) {
            player.sendMessage(new TextComponentString("That faction doesn't exist"));
            return false;
        }
        if (!WarForgeMod.isOp(player) && !faction.isPlayerRoleInFaction(player.getUniqueID(), Role.LEADER)) {
            player.sendMessage(new TextComponentString("You are not the faction leader"));
            return false;
        }
        if (!faction.flagId.isEmpty()) {
            player.sendMessage(new TextComponentString("This faction has already chosen its flag"));
            return false;
        }
        if (!WarForgeMod.FLAG_REGISTRY.isAvailable(flagId)) {
            player.sendMessage(new TextComponentString("That flag is not available"));
            return false;
        }

        faction.flagId = flagId;
        for (World world : MC_SERVER.worlds) {
            for (TileEntity te : world.loadedTileEntityList) {
                if (te instanceof TileEntityClaim claim && claim.getFaction().equals(faction.uuid)) {
                    claim.updateFactionFlag(flagId);
                }
            }
        }
        faction.messageAll(new TextComponentString("Your faction selected its flag: " + flagId));
        return true;
    }

    public boolean requestRemovePlayerFromFaction(ICommandSender remover, UUID factionID, UUID toRemove) {
        Faction faction = getFaction(factionID);
        if (faction == null) {
            remover.sendMessage(new TextComponentString("That faction doesn't exist"));
            return false;
        }

        if (!faction.isPlayerInFaction(toRemove)) {
            remover.sendMessage(new TextComponentString("That player is not in that faction"));
            return false;
        }

        boolean canRemove = isOp(remover);
        boolean removingSelf = false;
        if (remover instanceof EntityPlayer) {
            UUID removerID = ((EntityPlayer) remover).getUniqueID();
            if (removerID.equals(toRemove)) // remove self
            {
                canRemove = true;
                removingSelf = true;
            }

            if (faction.isPlayerOutrankingOfficer(removerID, toRemove))
                canRemove = true;
        }

        if (!canRemove) {
            remover.sendMessage(new TextComponentString("You don't have permission to remove that player"));
            return false;
        }

        GameProfile userProfile = MC_SERVER.getPlayerProfileCache().getProfileByUUID(toRemove);
        if (userProfile != null) {
            if (removingSelf) {
                faction.messageAll(new TextComponentString(userProfile.getName() + " left " + faction.name));
                if (faction.getMemberCount() <= 1)
                    faction.messageAll(new TextComponentString(faction.name + "was abandoned and disbanded."));
            } else
                faction.messageAll(new TextComponentString(userProfile.getName() + " was kicked from " + faction.name));
        } else {
            remover.sendMessage(new TextComponentString("Error: Could not get user profile"));
        }

        faction.removePlayer(toRemove);

        if (faction.getMemberCount() < 1)
            disbandAndCleanup(faction);

        sendAllSiegeInfoToNearby();

        return true;
    }

    public boolean requestInvitePlayerToMyFaction(EntityPlayer factionOfficer, UUID invitee) {
        Faction myFaction = getFactionOfPlayer(factionOfficer.getUniqueID());
        if (myFaction != null)
            return RequestInvitePlayerToFaction(factionOfficer, myFaction.uuid, invitee);
        return false;
    }

    public boolean RequestInvitePlayerToFaction(ICommandSender factionOfficer, UUID factionID, UUID invitee) {
        Faction faction = getFaction(factionID);
        if (faction == null) {
            factionOfficer.sendMessage(new TextComponentString("That faction doesn't exist"));
            return false;
        }

        if (!isOp(factionOfficer) && !faction.isPlayerRoleInFaction(getUUID(factionOfficer), Faction.Role.OFFICER)) {
            factionOfficer.sendMessage(new TextComponentString("You are not an officer of this faction"));
            return false;
        }

        Faction existingFaction = getFactionOfPlayer(invitee);
        if (existingFaction != null) {
            factionOfficer.sendMessage(new TextComponentString("That player is already in a faction"));
            return false;
        }

        // TODO: Faction player limit - grows with claims?

        faction.invitePlayer(invitee);
        MC_SERVER.getPlayerList().getPlayerByUUID(invitee).sendMessage(new TextComponentString("You have received an invite to " + faction.name + ". Type /f accept to join"));

        return true;
    }

    public void RequestAcceptInvite(EntityPlayer player) {
        Faction inviter = GetFactionWithOpenInviteTo(player.getUniqueID());
        if (inviter != null) {
            invalidateRedeemableInsuranceVault(player.getUniqueID(), player);
            inviter.addPlayer(player.getUniqueID());
        } else
            player.sendMessage(new TextComponentString("You have no open invite to accept"));
    }

    public boolean RequestTransferLeadership(EntityPlayer factionLeader, UUID factionID, UUID newLeaderID) {
        Faction faction = getFaction(factionID);
        if (faction == null) {
            factionLeader.sendMessage(new TextComponentString("That faction does not exist"));
            return false;
        }

        if (!isOp(factionLeader) && !faction.isPlayerRoleInFaction(factionLeader.getUniqueID(), Faction.Role.LEADER)) {
            factionLeader.sendMessage(new TextComponentString("You are not the leader of this faction"));
            return false;
        }

        if (!faction.isPlayerInFaction(newLeaderID)) {
            factionLeader.sendMessage(new TextComponentString("That player is not in your faction"));
            return false;
        }

        // Do the set
        if (!faction.setLeader(newLeaderID)) {
            factionLeader.sendMessage(new TextComponentString("Failed to set leader"));
            return false;
        }

        factionLeader.sendMessage(new TextComponentString("Successfully set leader"));
        return true;
    }

    public boolean requestPromote(EntityPlayer factionLeader, EntityPlayerMP target) {
        Faction faction = getFactionOfPlayer(factionLeader.getUniqueID());
        if (faction == null) {
            factionLeader.sendMessage(new TextComponentString("You are not in a faction"));
            return false;
        }
        if (!faction.isPlayerRoleInFaction(factionLeader.getUniqueID(), Role.LEADER)) {
            factionLeader.sendMessage(new TextComponentString("You are not the leader of this faction"));
            return false;
        }
        if (!faction.isPlayerRoleInFaction(target.getUniqueID(), Role.MEMBER)) {
            factionLeader.sendMessage(new TextComponentString("This player cannot be promoted"));
            return false;
        }

        faction.promote(target.getUniqueID());
        return true;
    }

    public boolean requestDemote(EntityPlayer factionLeader, EntityPlayerMP target) {
        Faction faction = getFactionOfPlayer(factionLeader.getUniqueID());
        if (faction == null) {
            factionLeader.sendMessage(new TextComponentString("You are not in a faction"));
            return false;
        }
        if (!faction.isPlayerRoleInFaction(factionLeader.getUniqueID(), Role.LEADER)) {
            factionLeader.sendMessage(new TextComponentString("You are not the leader of this faction"));
            return false;
        }
        if (!faction.isPlayerRoleInFaction(target.getUniqueID(), Role.OFFICER)) {
            factionLeader.sendMessage(new TextComponentString("This player cannot be demoted"));
            return false;
        }

        faction.demote(target.getUniqueID());
        return true;
    }

    public boolean requestDisbandFaction(EntityPlayer factionLeader, UUID factionID) {
        if (factionID.equals(Faction.nullUuid)) {
            Faction faction = getFactionOfPlayer(factionLeader.getUniqueID());
            if (faction != null)
                factionID = faction.uuid;
        }

        if (!IsPlayerRoleInFaction(factionLeader.getUniqueID(), factionID, Faction.Role.LEADER)) {
            factionLeader.sendMessage(new TextComponentString("You are not the leader of this faction"));
            return false;
        }
        disbandAndCleanup(factionID, false);
        return true;
    }

    // Used to remove and clean up faction;
    private void disbandAndCleanup(UUID factionID) {
        disbandAndCleanup(mFactions.get(factionID), false);
    }

    private void disbandAndCleanup(UUID factionID, boolean unlockInsurance) {
        disbandAndCleanup(mFactions.get(factionID), unlockInsurance);
    }

    private void disbandAndCleanup(Faction faction) {
        disbandAndCleanup(faction, false);
    }

    private void disbandAndCleanup(Faction faction, boolean unlockInsurance) {
        if (faction == null) {
            return;
        }
        if (unlockInsurance) {
            unlockInsuranceVault(faction);
        }
        EffectDisband.composeEffect(faction.citadelPos.dim, faction.citadelPos.toRegularPos(), 100);
        for (Map.Entry<DimBlockPos, Integer> kvp : faction.claims.entrySet()) {
            mClaims.remove(kvp.getKey().toChunkPos());
        }
        ArrayList<EntityPlayer> onlinePlayers = faction.getOnlinePlayers(
                player -> player != null);

        onlinePlayers.forEach(player -> {
            PacketNamePlateChange packet = new PacketNamePlateChange();
            packet.name = player.getName();
            packet.isRemove = true;
            NETWORK.sendToAllAround(packet, player.posX, player.posY, player.posZ, 100, player.dimension);
        });
        faction.disband();
        mFactions.remove(faction.uuid);
        WarForgeMod.CHUNK_LOADING_MANAGER.releaseFaction(faction.uuid);
        LEADERBOARD.UnregisterFaction(faction);
    }

    //Use disbandAndCleanup
    public void FactionDefeated(Faction faction) {
        disbandAndCleanup(faction, true);

    }

    public boolean requestRedeemInsuranceVault(EntityPlayerMP player) {
        ArrayList<ItemStack> items = redeemableInsuranceVaults.get(player.getUniqueID());
        if (items == null || items.isEmpty()) {
            player.sendMessage(new TextComponentString("You do not have an unlocked insurance stash to redeem"));
            return false;
        }

        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ItemStack redeemStack = stack.copy();
            boolean inserted = player.inventory.addItemStackToInventory(redeemStack);
            if (!inserted && !redeemStack.isEmpty()) {
                player.dropItem(redeemStack, false);
            }
        }

        redeemableInsuranceVaults.remove(player.getUniqueID());
        player.sendMessage(new TextComponentString("Redeemed your faction insurance stash"));
        return true;
    }

    private void unlockInsuranceVault(Faction faction) {
        if (!faction.hasInsuranceContents()) {
            return;
        }

        UUID leaderId = faction.getLeaderId();
        if (leaderId.equals(Faction.nullUuid)) {
            LOGGER.warn("Could not unlock insurance stash for faction {} because no leader was found", faction.name);
            return;
        }

        ArrayList<ItemStack> payout = redeemableInsuranceVaults.computeIfAbsent(leaderId, ignored -> new ArrayList<ItemStack>());
        payout.addAll(faction.drainInsuranceContents());

        EntityPlayerMP leader = MC_SERVER.getPlayerList().getPlayerByUUID(leaderId);
        if (leader != null) {
            leader.sendMessage(new TextComponentString("Your faction insurance stash has been unlocked. Use /f vault redeem to withdraw it."));
        }
    }

    private void invalidateRedeemableInsuranceVault(UUID playerId, EntityPlayer player) {
        ArrayList<ItemStack> removed = redeemableInsuranceVaults.remove(playerId);
        if (removed != null && !removed.isEmpty() && player != null) {
            player.sendMessage(new TextComponentString("Your unlocked insurance stash was voided because you entered a new faction before redeeming it."));
        }
    }

    public boolean IsSiegeInProgress(DimChunkPos chunkPos) {

        for (Map.Entry<DimChunkPos, Siege> kvp : sieges.entrySet()) {
            if (kvp.getKey().equals(chunkPos))
                return true;

            for (DimBlockPos attackerPos : kvp.getValue().attackingCamps) {
                if (attackerPos.toChunkPos().equals(chunkPos))
                    return true;
            }
        }
        return false;
    }

    public void onFactionMemberLoggedIn(UUID playerID) {
        Faction faction = getFactionOfPlayer(playerID);
        if (!isValidFaction(faction)) {
            return;
        }
        faction.onlinePlayerCount += 1;
        faction.offlineRaidProtectionUntil = 0L;
    }

    public void onFactionMemberLoggedOut(UUID playerID) {
        Faction faction = getFactionOfPlayer(playerID);
        if (!isValidFaction(faction)) {
            return;
        }
        faction.onlinePlayerCount = Math.max(0, faction.onlinePlayerCount - 1);
        if (faction.onlinePlayerCount == 0 && WarForgeConfig.ENABLE_OFFLINE_RAID_PROTECTION && !faction.offlineRaidProtectionDisabled) {
            faction.offlineRaidProtectionUntil = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(WarForgeConfig.OFFLINE_RAID_PROTECTION_HOURS);
        }
    }

    public boolean isOfflineRaidProtected(Faction faction) {
        if (!WarForgeConfig.ENABLE_OFFLINE_RAID_PROTECTION || faction == null || faction.offlineRaidProtectionDisabled) {
            return false;
        }
        return faction.onlinePlayerCount <= 0 && System.currentTimeMillis() < faction.offlineRaidProtectionUntil;
    }

    // runs on the server only
    public void requestStartSiege(EntityPlayer factionOfficer, DimBlockPos siegeCampPos, Vec3i direction) {
        Faction attacking = getFactionOfPlayer(factionOfficer.getUniqueID());
        if (attacking == null) {
            factionOfficer.sendMessage(new TextComponentString("You are not in a faction"));
            return;
        }
        long currentTimeStamp = System.currentTimeMillis();

        // for some reason, server tick is in number of ticks and last siege timestamp is in ms, while siege cooldown is in mins (according to description), though through calculations looks like hours? it should be in ms
        if (attacking.getSiegeMomentum() == 0 && attacking.lastSiegeTimestamp + WarForgeConfig.SIEGE_COOLDOWN_FAIL > currentTimeStamp) {
            factionOfficer.sendMessage(new TextComponentString("Your faction is on cooldown on starting a new siege"));


            factionOfficer.sendMessage(new TextComponentString("Cooldown remaining:" + TimeHelper.formatTime(attacking.lastSiegeTimestamp + WarForgeConfig.SIEGE_COOLDOWN_FAIL - currentTimeStamp)));
            return;
        }


        if (!attacking.isPlayerRoleInFaction(factionOfficer.getUniqueID(), Faction.Role.OFFICER)) {
            factionOfficer.sendMessage(new TextComponentString("You are not an officer of this faction"));
            return;
        }

        // TODO: Verify there aren't existing alliances

        if (direction.getZ() == 0 && direction.getX() == 0) {
            factionOfficer.sendMessage(new TextComponentString("You can't siege the siege block!"));
            return;
        }

        TileEntitySiegeCamp siegeTE = (TileEntitySiegeCamp) MC_SERVER.getWorld(siegeCampPos.dim).getTileEntity(siegeCampPos.toRegularPos());
        if (siegeTE == null) {
            factionOfficer.sendMessage(new TextComponentString("Could not find that siege camp"));
            return;
        }
        if (!attacking.uuid.equals(siegeTE.getFaction())) {
            factionOfficer.sendMessage(new TextComponentString("Your faction doesn't own this block!"));
            return;
        }

        DimChunkPos defendingChunk = siegeCampPos.toChunkPos().Offset(direction);
        UUID defendingFactionID = mClaims.get(defendingChunk);
        Faction defending = getFaction(defendingFactionID);

        if (defending == null) {
            factionOfficer.sendMessage(new TextComponentString("Could not find a target faction at that position"));
            return;
        }

        if (isOfflineRaidProtected(defending)) {
            factionOfficer.sendMessage(new TextComponentString("That faction is offline and protected until " + TimeHelper.formatTime(defending.offlineRaidProtectionUntil - System.currentTimeMillis())));
            return;
        }

        DimBlockPos defendingPos = defending.getSpecificPosForClaim(defendingChunk);
        if (defendingPos == null) {
            factionOfficer.sendMessage(new TextComponentString("Could not find a valid defending claim in that chunk"));
            return;
        }

        if (IsSiegeInProgress(defendingPos.toChunkPos())) {
            factionOfficer.sendMessage(new TextComponentString("That position is already under siege"));
            return;
        }

        if (conqueredChunks.get(defendingPos.toChunkPos()) != null) {
            factionOfficer.sendMessage(new TextComponentTranslation("warforge.info.chunk_is_conquered",
                    defending.name, TimeHelper.formatTime(conqueredChunks.get(defendingPos.toChunkPos()).getInteger())));
            return;
        }

        long maxTime = WarForgeConfig.SIEGE_MOMENTUM_TIME.get(attacking.getSiegeMomentum()) * 1000L;

        Siege siege = new Siege(attacking.uuid, defendingFactionID, defendingPos, maxTime);
        siege.attackingCamps.add(siegeCampPos);

        sieges.put(defendingChunk, siege);
        siegeTE.setSiegeTarget(defendingPos);
        siege.start();

        attacking.lastSiegeTimestamp = currentTimeStamp;
    }

    public void endSiege(DimBlockPos getPos) {
        Siege siege = sieges.get(getPos.toChunkPos());
        if (siege != null) {
            siege.onCancelled();
            sieges.remove(getPos.toChunkPos());
        }
    }

    public void requestOpClaim(EntityPlayer op, DimChunkPos pos, UUID factionID) {
        Faction zone = getFaction(factionID);
        if (zone == null) {
            op.sendMessage(new TextComponentString("Could not find that faction"));
            return;
        }

        UUID existingClaim = getClaim(pos);
        if (!existingClaim.equals(Faction.nullUuid)) {
            op.sendMessage(new TextComponentString("There is already a claim here"));
            return;
        }

        // Place a bedrock tile entity at 0,0,0 chunk coords
        // This might look a bit dodge in End. It's only for admin claims though
        DimBlockPos tePos = new DimBlockPos(pos.dim, pos.getXStart(), 0, pos.getZStart());
        op.world.setBlockState(tePos.toRegularPos(), Content.adminClaimBlock.getDefaultState());
        TileEntity te = op.world.getTileEntity(tePos.toRegularPos());
        if (te == null || !(te instanceof IClaim)) {
            op.sendMessage(new TextComponentString("Placing admin claim block failed"));
            return;
        }

        onNonCitadelClaimPlaced((IClaim) te, zone);

        op.sendMessage(new TextComponentString("Claimed " + pos + " for faction " + zone.name));

    }

    public void sendSiegeInfoToNearby(DimChunkPos siegePos) {
        Siege siege = sieges.get(siegePos);
        if (siege != null) {
            SiegeCampProgressInfo info = siege.GetSiegeInfo();
            if (info != null) {
                PacketSiegeCampProgressUpdate packet = new PacketSiegeCampProgressUpdate();
                packet.info = info;
                NETWORK.sendToAllAround(packet, siegePos.x * 16, 128d, siegePos.z * 16, WarForgeConfig.SIEGE_INFO_RADIUS + 128f, siegePos.dim);
            }
        }
    }

    public void sendAllSiegeInfoToNearby() {
        for (HashMap.Entry<DimChunkPos, Siege> kvp : FACTIONS.sieges.entrySet()) {
            kvp.getValue().calculateBasePower();

            sendSiegeInfoToNearby(kvp.getKey());
        }

    }

    // Returns arraylist with nulls for invalid claim directions, in horizontal + diagonal ordering
    public int getAdjacentClaims(UUID excludingFaction, DimBlockPos pos, ArrayList<DimChunkPos> positions) {
        // Ensure list has 8 entries: [N, S, W, E, NW, NE, SW, SE]
        if (positions.size() < 8)
            positions = new ArrayList<>(Arrays.asList(new DimChunkPos[8]));
        int numValidTargets = 0;

        // Cardinal directions (indices 0–3)
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            DimChunkPos targetChunkPos = pos.toChunkPos().Offset(facing, 1);
            if (!isClaimed(excludingFaction, targetChunkPos)) continue;
            UUID targetID = getClaim(targetChunkPos);
            int targetY = getFaction(targetID).getSpecificPosForClaim(targetChunkPos).getY();
            if (Math.abs(pos.getY() - targetY) > WarForgeConfig.VERTICAL_SIEGE_DIST) continue;

            positions.set(facing.getHorizontalIndex(), targetChunkPos);
            ++numValidTargets;
        }

        // Diagonal directions (indices 4–7): NW, NE, SW, SE
        EnumFacing[][] diagonalPairs = {
                {EnumFacing.NORTH, EnumFacing.WEST},  // NW
                {EnumFacing.NORTH, EnumFacing.EAST},  // NE
                {EnumFacing.SOUTH, EnumFacing.WEST},  // SW
                {EnumFacing.SOUTH, EnumFacing.EAST}   // SE
        };

        for (int i = 0; i < diagonalPairs.length; i++) {
            EnumFacing f1 = diagonalPairs[i][0];
            EnumFacing f2 = diagonalPairs[i][1];
            DimChunkPos diagonalPos = pos.toChunkPos().Offset(f1, 1).Offset(f2, 1);
            if (!isClaimed(excludingFaction, diagonalPos)) continue;
            UUID targetID = getClaim(diagonalPos);
            int targetY = getFaction(targetID).getSpecificPosForClaim(diagonalPos).getY();
            if (Math.abs(pos.getY() - targetY) > WarForgeConfig.VERTICAL_SIEGE_DIST) continue;

            positions.set(4 + i, diagonalPos);
            ++numValidTargets;
        }

        return numValidTargets;
    }


    public LinkedHashMap<DimChunkPos, Boolean> getClaimRadiusAround(UUID excludedFaction, DimBlockPos originPos, int radius) {
        LinkedHashMap<DimChunkPos, Boolean> posMap = new LinkedHashMap<>((int) Math.pow(radius * 2, 2) + 1, 0.75f, true);
        DimChunkPos centerChunk = originPos.toChunkPos();
        int centerX = centerChunk.x;
        int centerZ = centerChunk.z;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                boolean canSiege = true;
                DimChunkPos targetChunkPos = new DimChunkPos(centerChunk.dim, x, z);
                if (!isClaimed(excludedFaction, targetChunkPos)) canSiege = false; // also screens for dimension
                UUID chunkClaim = getClaim(targetChunkPos);
                if (chunkClaim.equals(excludedFaction) || chunkClaim.equals(Faction.nullUuid)) canSiege = false;
                else {
                    int targetY = getFaction(chunkClaim).getSpecificPosForClaim(targetChunkPos).getY();
                    if (originPos.getY() < targetY - WarForgeConfig.VERTICAL_SIEGE_DIST || originPos.getY() > targetY + WarForgeConfig.VERTICAL_SIEGE_DIST)
                        canSiege = false;
                }
                posMap.put(targetChunkPos, canSiege);

            }
        }
        return posMap;
    }

    public boolean isClaimed(UUID excludingFaction, DimChunkPos pos) {
        UUID factionID = getClaim(pos);
        return factionID != null && !factionID.equals(excludingFaction) && !factionID.equals(Faction.nullUuid);
    }

    public Set<DimChunkPos> collectFactionIsland(UUID factionID, DimChunkPos start) {
        HashSet<DimChunkPos> visited = new HashSet<DimChunkPos>();
        if (!factionID.equals(getClaim(start))) {
            return visited;
        }

        ArrayDeque<DimChunkPos> queue = new ArrayDeque<DimChunkPos>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            DimChunkPos current = queue.poll();
            for (EnumFacing facing : EnumFacing.HORIZONTALS) {
                DimChunkPos neighbor = current.Offset(facing, 1);
                if (!visited.contains(neighbor) && factionID.equals(getClaim(neighbor))) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return visited;
    }

    public boolean registerCollector(EntityPlayerMP player, DimBlockPos collectorPos) {
        Faction faction = getFactionOfPlayer(player.getUniqueID());
        if (faction == null) {
            player.sendMessage(new TextComponentString("You are not in a faction"));
            return false;
        }
        if (!isOp(player) && !faction.isPlayerRoleInFaction(player.getUniqueID(), Role.OFFICER)) {
            player.sendMessage(new TextComponentString("You are not an officer of your faction"));
            return false;
        }

        DimChunkPos collectorChunk = collectorPos.toChunkPos();
        if (!faction.uuid.equals(getClaim(collectorChunk))) {
            player.sendMessage(new TextComponentString("Collector can only be placed in your faction land"));
            return false;
        }

        Set<DimChunkPos> island = collectFactionIsland(faction.uuid, collectorChunk);
        ArrayList<DimBlockPos> staleCollectors = new ArrayList<DimBlockPos>();
        for (DimBlockPos existingCollector : faction.islandCollectors) {
            World collectorWorld = MC_SERVER.getWorld(existingCollector.dim);
            if (collectorWorld == null || !(collectorWorld.getTileEntity(existingCollector.toRegularPos()) instanceof TileEntityIslandCollector)) {
                staleCollectors.add(existingCollector);
                continue;
            }
            if (island.contains(existingCollector.toChunkPos())) {
                player.sendMessage(new TextComponentString("There is already a collector on this faction island"));
                return false;
            }
        }
        if (!staleCollectors.isEmpty()) {
            faction.islandCollectors.removeAll(staleCollectors);
            WarForgeMod.CHUNK_LOADING_MANAGER.refreshFactionChunks(faction);
        }

        TileEntity te = player.world.getTileEntity(collectorPos.toRegularPos());
        if (!(te instanceof TileEntityIslandCollector collector)) {
            return false;
        }

        collector.onServerSetFaction(faction);
        faction.islandCollectors.add(collectorPos);
        WarForgeMod.CHUNK_LOADING_MANAGER.refreshFactionChunks(faction);
        return true;
    }

    public void unregisterCollector(DimBlockPos collectorPos) {
        for (Faction faction : mFactions.values()) {
            if (faction.islandCollectors.remove(collectorPos)) {
                WarForgeMod.CHUNK_LOADING_MANAGER.refreshFactionChunks(faction);
                return;
            }
        }
    }

    public boolean requestToggleForceLoad(EntityPlayerMP player, DimChunkPos chunkPos) {
        Faction faction = getFactionOfPlayer(player.getUniqueID());
        if (faction == null) {
            player.sendMessage(new TextComponentString("You are not in a faction"));
            return false;
        }
        if (!isOp(player) && !faction.isPlayerRoleInFaction(player.getUniqueID(), Role.OFFICER)) {
            player.sendMessage(new TextComponentString("You are not an officer of your faction"));
            return false;
        }
        if (!faction.uuid.equals(getClaim(chunkPos))) {
            player.sendMessage(new TextComponentString("You can only force-load your faction chunks"));
            return false;
        }

        if (faction.forcedChunks.contains(chunkPos)) {
            faction.forcedChunks.remove(chunkPos);
            player.sendMessage(new TextComponentString("Removed force-load from chunk [" + chunkPos.x + ", " + chunkPos.z + "]"));
        } else {
            if (!faction.canForceLoadMore()) {
                player.sendMessage(new TextComponentString("Force-load chunk limit reached (" + faction.getMaxForceLoadedChunks() + ")"));
                return false;
            }
            faction.forcedChunks.add(chunkPos);
            player.sendMessage(new TextComponentString("Force-loaded chunk [" + chunkPos.x + ", " + chunkPos.z + "]"));
        }

        WarForgeMod.CHUNK_LOADING_MANAGER.refreshFactionChunks(faction);
        return true;
    }

    public PacketClaimChunksData createClaimChunksData(EntityPlayerMP player, DimChunkPos center, int radius) {
        int clampedRadius = Math.max(1, Math.min(radius, WarForgeConfig.CLAIM_MANAGER_RADIUS));
        if (center.dim != player.dimension) {
            center = new DimChunkPos(player.dimension, player.getPosition());
        }

        Faction faction = getFactionOfPlayer(player.getUniqueID());
        boolean canManage = faction != null && (isOp(player) || faction.isPlayerRoleInFaction(player.getUniqueID(), Role.OFFICER));

        PacketClaimChunksData packet = new PacketClaimChunksData();
        packet.dim = center.dim;
        packet.centerX = center.x;
        packet.centerZ = center.z;
        packet.radius = clampedRadius;
        packet.playerFactionId = faction == null ? Faction.nullUuid : faction.uuid;
        packet.forceLoadedCount = faction == null ? 0 : faction.forcedChunks.size();
        packet.forceLoadedMax = faction == null ? 0 : faction.getMaxForceLoadedChunks();
        packet.claimCount = faction == null ? 0 : faction.claims.size();
        packet.claimMax = faction == null ? 0 : WarForgeMod.UPGRADE_HANDLER.getClaimLimitForLevel(faction.citadelLevel);
        if (packet.claimMax < 0) {
            packet.claimMax = Short.MAX_VALUE;
        }

        for (int x = center.x - clampedRadius; x <= center.x + clampedRadius; x++) {
            for (int z = center.z - clampedRadius; z <= center.z + clampedRadius; z++) {
                DimChunkPos chunk = new DimChunkPos(center.dim, x, z);
                UUID ownerId = getClaim(chunk);
                Faction ownerFaction = getFaction(ownerId);
                if (ownerFaction != null && ownerFaction.getSpecificPosForClaim(chunk) == null) {
                    mClaims.remove(chunk);
                    ownerFaction = null;
                    ownerId = Faction.nullUuid;
                }

                ClaimChunkInfo info = new ClaimChunkInfo();
                info.x = x;
                info.z = z;
                if (ownerFaction != null) {
                    info.factionId = ownerFaction.uuid;
                    info.factionName = ownerFaction.name;
                    info.flagId = ownerFaction.flagId;
                    info.colour = ownerFaction.colour;
                    info.claimType = ownerFaction.getClaimType(chunk);
                }

                var veinInfo = VEIN_HANDLER.getVein(chunk.dim, chunk.x, chunk.z, MC_SERVER.worlds[0].getSeed());
                if (veinInfo != null) {
                    info.vein = veinInfo.getLeft();
                    info.oreQuality = veinInfo.getRight();
                }

                if (faction != null && ownerFaction != null && ownerFaction.uuid.equals(faction.uuid)) {
                    info.flags |= ClaimChunkInfo.FLAG_OWNED_BY_PLAYER;
                    if (faction.forcedChunks.contains(chunk)) {
                        info.flags |= ClaimChunkInfo.FLAG_FORCE_LOADED;
                    }
                    for (DimBlockPos collectorPos : faction.islandCollectors) {
                        if (collectorPos.toChunkPos().equals(chunk)) {
                            info.flags |= ClaimChunkInfo.FLAG_HAS_COLLECTOR;
                            break;
                        }
                    }
                }

                if (canManage) {
                    if (ownerFaction == null) {
                        if (canClaimChunkNoTile(player, faction, chunk, false)) {
                            info.flags |= ClaimChunkInfo.FLAG_CAN_CLAIM;
                        }
                    } else if (ownerFaction.uuid.equals(faction.uuid)) {
                        DimBlockPos claimPos = faction.getSpecificPosForClaim(chunk);
                        if (claimPos != null && !claimPos.equals(faction.citadelPos) && !IsSiegeInProgress(chunk)) {
                            info.flags |= ClaimChunkInfo.FLAG_CAN_UNCLAIM;
                        }
                        info.flags |= ClaimChunkInfo.FLAG_CAN_TOGGLE_FORCELOAD;
                    }
                }

                packet.chunks.add(info);
            }
        }

        return packet;
    }

    public void sendClaimChunks(EntityPlayerMP player, DimChunkPos center, int radius) {
        PacketClaimChunksData packet = createClaimChunksData(player, center, radius);
        WarForgeMod.NETWORK.sendTo(packet, player);
    }

    private ClaimedBlockSelection findFirstClaimBlock(InventoryPlayer inventory) {
        for (int slot = 0; slot < inventory.mainInventory.size(); slot++) {
            ItemStack stack = inventory.mainInventory.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() == Content.basicClaimBlockItem) {
                return new ClaimedBlockSelection(slot, Faction.ClaimType.BASIC);
            }
            if (stack.getItem() == Content.reinforcedClaimBlockItem) {
                return new ClaimedBlockSelection(slot, Faction.ClaimType.REINFORCED);
            }
        }
        return null;
    }

    private ItemStack claimTypeToStack(Faction.ClaimType claimType) {
        return switch (claimType) {
            case BASIC -> new ItemStack(Content.basicClaimBlockItem);
            case REINFORCED -> new ItemStack(Content.reinforcedClaimBlockItem);
            default -> ItemStack.EMPTY;
        };
    }

    public boolean requestRemoveClaim(EntityPlayerMP player, DimBlockPos pos) {
        DimChunkPos targetChunk = pos.toChunkPos();
        UUID factionID = getClaim(targetChunk);
        Faction faction = getFaction(factionID);
        if (factionID.equals(Faction.nullUuid) || faction == null) {
            player.sendMessage(new TextComponentString("Could not find a claim in that location"));
            return false;
        }

        DimBlockPos claimPos = faction.getSpecificPosForClaim(targetChunk);
        if (claimPos != null) {
            pos = claimPos;
        }

        if (pos.equals(faction.citadelPos)) {
            player.sendMessage(new TextComponentString("Can't remove the citadel without disbanding the faction"));
            return false;
        }

        if (!isOp(player) && !faction.isPlayerRoleInFaction(player.getUniqueID(), Role.OFFICER)) {
            player.sendMessage(new TextComponentString("You are not an officer of the faction"));
            return false;
        }

        for (HashMap.Entry<DimChunkPos, Siege> siege : sieges.entrySet()) {
            if (siege.getKey().equals(targetChunk)) {
                player.sendMessage(new TextComponentString("This claim is currently under siege"));
                return false;
            }

            if (siege.getValue().attackingCamps.contains(pos)) {
                player.sendMessage(new TextComponentString("This siege camp is currently in a siege"));
                return false;
            }
        }

        Faction.ClaimType claimType = faction.getClaimType(targetChunk);
        World claimWorld = MC_SERVER.getWorld(pos.dim);
        boolean dataOnlyClaim = claimWorld != null && !WarForgeMod.isClaim(
                claimWorld.getBlockState(pos.toRegularPos()).getBlock(),
                Content.statue,
                Content.dummyTranslusent
        );

        faction.onClaimLost(pos);
        mClaims.remove(targetChunk);
        faction.forcedChunks.remove(targetChunk);
        ArrayList<DimBlockPos> removedCollectors = new ArrayList<DimBlockPos>();
        for (DimBlockPos collectorPos : faction.islandCollectors) {
            if (collectorPos.toChunkPos().equals(targetChunk)) {
                removedCollectors.add(collectorPos);
                World world = MC_SERVER.getWorld(collectorPos.dim);
                if (world != null) {
                    world.setBlockToAir(collectorPos.toRegularPos());
                }
            }
        }
        faction.islandCollectors.removeAll(removedCollectors);
        WarForgeMod.CHUNK_LOADING_MANAGER.refreshFactionChunks(faction);
        if (dataOnlyClaim) {
            ItemStack refund = claimTypeToStack(claimType);
            if (!refund.isEmpty()) {
                boolean inserted = player.inventory.addItemStackToInventory(refund);
                if (!inserted && !refund.isEmpty()) {
                    player.dropItem(refund, false);
                } else {
                    player.inventoryContainer.detectAndSendChanges();
                }
            }
        }
        faction.messageAll(new TextComponentString(player.getName() + " unclaimed " + pos.toFancyString()));

        return true;
    }

    public boolean requestRemoveClaimByChunk(EntityPlayerMP player, DimChunkPos chunkPos) {
        Faction faction = getFaction(getClaim(chunkPos));
        if (faction == null) {
            player.sendMessage(new TextComponentString("Could not find a claim in that location"));
            return false;
        }

        DimBlockPos claimPos = faction.getSpecificPosForClaim(chunkPos);
        if (claimPos == null) {
            player.sendMessage(new TextComponentString("Could not find a claim in that location"));
            return false;
        }

        return requestRemoveClaim(player, claimPos);
    }

    public boolean requestRemoveClaimServer(DimBlockPos pos) {
        DimChunkPos targetChunk = pos.toChunkPos();
        UUID factionID = getClaim(targetChunk);
        Faction faction = getFaction(factionID);
        if (factionID.equals(Faction.nullUuid) || faction == null) {
            return false;
        }

        DimBlockPos claimPos = faction.getSpecificPosForClaim(targetChunk);
        if (claimPos != null) {
            pos = claimPos;
        }

        if (pos.equals(faction.citadelPos)) {
            return false;
        }

        for (HashMap.Entry<DimChunkPos, Siege> siege : sieges.entrySet()) {
            if (siege.getKey().equals(targetChunk)) {
                return false;
            }

            if (siege.getValue().attackingCamps.contains(pos)) {
                return false;
            }
        }

        faction.onClaimLost(pos);
        mClaims.remove(targetChunk);
        faction.forcedChunks.remove(targetChunk);
        ArrayList<DimBlockPos> removedCollectors = new ArrayList<DimBlockPos>();
        for (DimBlockPos collectorPos : faction.islandCollectors) {
            if (collectorPos.toChunkPos().equals(targetChunk)) {
                removedCollectors.add(collectorPos);
                World world = MC_SERVER.getWorld(collectorPos.dim);
                if (world != null) {
                    world.setBlockToAir(collectorPos.toRegularPos());
                }
            }
        }
        faction.islandCollectors.removeAll(removedCollectors);
        WarForgeMod.CHUNK_LOADING_MANAGER.refreshFactionChunks(faction);
        return true;
    }

    public boolean requestSetFactionColour(EntityPlayerMP player, int colour) {
        Faction faction = getFactionOfPlayer(player.getUniqueID());
        if (faction == null) {
            player.sendMessage(new TextComponentString("You are not in a faction"));
            return false;
        }

        if (!faction.isPlayerRoleInFaction(player.getUniqueID(), Role.LEADER)) {
            player.sendMessage(new TextComponentString("You are not the faction leader"));
            return false;
        }

        faction.setColour(colour);
        for (World world : MC_SERVER.worlds) {
            for (TileEntity te : world.loadedTileEntityList) {
                if (te instanceof IClaim) {
                    if (((IClaim) te).getFaction().equals(faction.uuid)) {
                        ((IClaim) te).updateColour(colour);
                    }
                }
            }
        }

        ArrayList<EntityPlayer> onlinePlayers = faction.getOnlinePlayers(
                entityPlayer -> entityPlayer != null && entityPlayer.isEntityAlive());

        onlinePlayers.forEach(entityPlayer -> {
            PacketNamePlateChange packet = new PacketNamePlateChange();
            packet.name = entityPlayer.getName();
            packet.faction = faction.name;
            packet.color = colour;
            NETWORK.sendToAllAround(packet, entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, 100, player.dimension);
        });


        return true;

    }

    public boolean requestMoveCitadel(EntityPlayerMP player) {
        Faction faction = getFactionOfPlayer(player.getUniqueID());
        if (faction == null) {
            player.sendMessage(new TextComponentString("You are not in a faction"));
            return false;
        }

        if (!faction.isPlayerRoleInFaction(player.getUniqueID(), Role.LEADER)) {
            player.sendMessage(new TextComponentString("You are not the faction leader"));
            return false;
        }

        for (HashMap.Entry<DimChunkPos, Siege> kvp : sieges.entrySet()) {
            if (kvp.getValue().defendingFaction.equals(faction.uuid)) {
                player.sendMessage(new TextComponentString("There is an ongoing siege against your faction"));
                return false;
            }
        }
        RayTraceResult hit = player.rayTrace(player.interactionManager.getBlockReachDistance(), 1.0F);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || hit.sideHit == null) {
            player.sendMessage(new TextComponentString("Look at the block face where you want to place the citadel"));
            return false;
        }

        BlockPos targetPos = hit.getBlockPos().offset(hit.sideHit);
        DimBlockPos pos = new DimBlockPos(player.dimension, targetPos);
        if (pos.equals(faction.citadelPos)) {
            player.sendMessage(new TextComponentString("The citadel is already there"));
            return false;
        }

        DimChunkPos targetChunk = pos.toChunkPos();
        UUID chunkOwner = getClaim(targetChunk);
        if (!faction.uuid.equals(chunkOwner)) {
            player.sendMessage(new TextComponentString("You must look at a position inside your claimed land"));
            return false;
        }

        if (!canPlaceMovedCitadelAt(player.world, pos, faction.citadelPos)) {
            player.sendMessage(new TextComponentString("Not enough free space to place the 3-block citadel pillar there"));
            return false;
        }

        DimChunkPos oldChunk = faction.citadelPos.toChunkPos();
        DimBlockPos replacedClaimPos = targetChunk.equals(oldChunk) ? faction.citadelPos : faction.getSpecificPosForClaim(targetChunk);
        if (replacedClaimPos == null) {
            player.sendMessage(new TextComponentString("That claimed chunk has no stored claim position to replace"));
            return false;
        }

        TileEntityCitadel oldCitadel = (TileEntityCitadel) MC_SERVER.getWorld(faction.citadelPos.dim).getTileEntity(faction.citadelPos.toRegularPos());
        if (oldCitadel == null) {
            player.sendMessage(new TextComponentString("Could not find the current citadel tile entity"));
            return false;
        }

        Integer oldPending = faction.claims.remove(faction.citadelPos);
        if (oldPending == null) {
            oldPending = 0;
        }
        Integer targetPending = replacedClaimPos.equals(faction.citadelPos) ? oldPending : faction.claims.remove(replacedClaimPos);
        Faction.ClaimType targetClaimType = faction.claimTypes.getOrDefault(replacedClaimPos, Faction.ClaimType.BASIC);
        if (targetPending == null) {
            targetPending = 0;
        }
        if (!replacedClaimPos.equals(faction.citadelPos)) {
            faction.claimTypes.remove(replacedClaimPos);
        }

        // Set new citadel
        MC_SERVER.getWorld(pos.dim).setBlockState(pos.toRegularPos(), Content.citadelBlock.getDefaultState());
        TileEntityCitadel newCitadel = (TileEntityCitadel) MC_SERVER.getWorld(pos.dim).getTileEntity(pos.toRegularPos());
        if (newCitadel == null) {
            player.sendMessage(new TextComponentString("Failed to create the new citadel"));
            return false;
        }
        newCitadel.copyStorageFrom(oldCitadel);

        // Old citadel remains a normal claimed chunk but no longer has a physical claim block.
        DimBlockPos oldCitadelPos = faction.citadelPos;
        World oldCitadelWorld = MC_SERVER.getWorld(faction.citadelPos.dim);
        oldCitadelWorld.setBlockToAir(faction.citadelPos.toRegularPos());
        oldCitadelWorld.setBlockToAir(faction.citadelPos.toRegularPos().up());
        oldCitadelWorld.setBlockToAir(faction.citadelPos.toRegularPos().up(2));
        if (!targetChunk.equals(oldChunk)) {
            faction.claims.put(oldCitadelPos, oldPending);
            faction.claimTypes.put(oldCitadelPos, targetClaimType);
        }

        // Update pos
        faction.citadelPos = pos;
        faction.claims.put(pos, targetPending);
        faction.claimTypes.put(pos, Faction.ClaimType.CITADEL);
        newCitadel.onServerSetFaction(faction);

        INSTANCE.messageAll(new TextComponentString(faction.name + " moved their citadel"), true);
        return true;
    }

    private boolean canPlaceMovedCitadelAt(World world, DimBlockPos pos, DimBlockPos oldCitadelPos) {
        if (!world.getBlockState(pos.down()).isSideSolid(world, pos.down(), EnumFacing.UP)) {
            return false;
        }
        return isCitadelSpaceFree(world, pos, oldCitadelPos)
                && isCitadelSpaceFree(world, pos.up(), oldCitadelPos)
                && isCitadelSpaceFree(world, pos.up(2), oldCitadelPos);
    }

    private boolean isCitadelSpaceFree(World world, BlockPos pos, DimBlockPos oldCitadelPos) {
        if (oldCitadelPos != null) {
            if (pos.equals(oldCitadelPos.toRegularPos()) || pos.equals(oldCitadelPos.toRegularPos().up()) || pos.equals(oldCitadelPos.toRegularPos().up(2))) {
                return true;
            }
        }
        return world.isAirBlock(pos) || world.getBlockState(pos).getBlock().isReplaceable(world, pos);
    }

    public void readFromNBT(NBTTagCompound tags) {
        mFactions.clear();
        mClaims.clear();
        sieges.clear();
        redeemableInsuranceVaults.clear();

        InitNeutralZones();

        NBTTagList list = tags.getTagList("factions", 10); // Compound Tag
        for (NBTBase baseTag : list) {
            NBTTagCompound factionTags = ((NBTTagCompound) baseTag);
            UUID uuid = factionTags.getUniqueId("id");
            Faction faction;


            assert uuid != null;
            if (uuid.equals(SAFE_ZONE_ID)) {
                faction = SAFE_ZONE;
            } else if (uuid.equals(WAR_ZONE_ID)) {
                faction = WAR_ZONE;
            } else {
                faction = new Faction();
                faction.uuid = uuid;
                mFactions.put(uuid, faction);
                LEADERBOARD.RegisterFaction(faction);
            }

            faction.readFromNBT(factionTags);

            // Also populate the DimChunkPos lookup table
            for (DimBlockPos blockPos : faction.claims.keySet()) {
                mClaims.put(blockPos.toChunkPos(), uuid);
            }
        }

        list = tags.getTagList("sieges", 10); // Compound Tag
        for (NBTBase baseTag : list) {
            NBTTagCompound siegeTags = ((NBTTagCompound) baseTag);
            int dim = siegeTags.getInteger("dim");
            int x = siegeTags.getInteger("x");
            int z = siegeTags.getInteger("z");

            Siege siege = new Siege();
            siege.ReadFromNBT(siegeTags);

            sieges.put(new DimChunkPos(dim, x, z), siege);
        }

        readConqueredChunks(tags);
        readRedeemableInsurance(tags);
    }

    private void readConqueredChunks(NBTTagCompound tags) {
        conqueredChunks = new HashMap<>();
        NBTTagCompound conqueredChunksDataList = tags.getCompoundTag("conqueredChunks");

        // 11 is type id for int array
        int index = 0;
        while (true) {
            NBTTagList keyValPair = conqueredChunksDataList.getTagList("conqueredChunk_" + index, 11);
            if (keyValPair.isEmpty())
                break; // exit once invalid (empty, since getTagList never returns null) is found, as it is assumed this is the first non-existent/ invalid index
            int[] dimInfo = keyValPair.getIntArrayAt(0);
            DimChunkPos chunkPosKey = new DimChunkPos(dimInfo[0], dimInfo[1], dimInfo[2]);
            UUID factionID = BEIntArrayToUUID(keyValPair.getIntArrayAt(1));

            conqueredChunks.put(chunkPosKey, new ObjectIntPair<>(factionID, keyValPair.getIntArrayAt(2)[0]));
            ++index;
        }
    }

    public void WriteToNBT(NBTTagCompound tags) {
        NBTTagList factionList = new NBTTagList();
        for (HashMap.Entry<UUID, Faction> kvp : mFactions.entrySet()) {
            NBTTagCompound factionTags = new NBTTagCompound();
            factionTags.setUniqueId("id", kvp.getKey());
            kvp.getValue().writeToNBT(factionTags);
            factionList.appendTag(factionTags);
        }

        tags.setTag("factions", factionList);

        NBTTagList siegeList = new NBTTagList();
        for (HashMap.Entry<DimChunkPos, Siege> kvp : sieges.entrySet()) {
            NBTTagCompound siegeTags = new NBTTagCompound();
            siegeTags.setInteger("dim", kvp.getKey().dim);
            siegeTags.setInteger("x", kvp.getKey().x);
            siegeTags.setInteger("z", kvp.getKey().z);
            kvp.getValue().WriteToNBT(siegeTags);
            siegeList.appendTag(siegeTags);
        }

        tags.setTag("sieges", siegeList);

        writeConqueredChunks(tags);
        writeRedeemableInsurance(tags);
    }

    private void readRedeemableInsurance(NBTTagCompound tags) {
        NBTTagList payoutList = tags.getTagList("redeemableInsuranceVaults", 10);
        for (NBTBase base : payoutList) {
            NBTTagCompound payoutTag = (NBTTagCompound) base;
            UUID owner = payoutTag.getUniqueId("owner");
            ArrayList<ItemStack> items = new ArrayList<ItemStack>();
            NBTTagList itemsList = payoutTag.getTagList("items", 10);
            for (NBTBase itemBase : itemsList) {
                items.add(new ItemStack((NBTTagCompound) itemBase));
            }
            redeemableInsuranceVaults.put(owner, items);
        }
    }

    private void writeRedeemableInsurance(NBTTagCompound tags) {
        NBTTagList payoutList = new NBTTagList();
        for (Map.Entry<UUID, ArrayList<ItemStack>> entry : redeemableInsuranceVaults.entrySet()) {
            NBTTagCompound payoutTag = new NBTTagCompound();
            payoutTag.setUniqueId("owner", entry.getKey());
            NBTTagList itemsList = new NBTTagList();
            for (ItemStack stack : entry.getValue()) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                NBTTagCompound itemTag = new NBTTagCompound();
                stack.writeToNBT(itemTag);
                itemsList.appendTag(itemTag);
            }
            payoutTag.setTag("items", itemsList);
            payoutList.appendTag(payoutTag);
        }
        tags.setTag("redeemableInsuranceVaults", payoutList);
    }

    private void writeConqueredChunks(NBTTagCompound tags) {
        NBTTagCompound conqueredChunksDataList = new NBTTagCompound();
        int index = 0;
        for (DimChunkPos chunkPosKey : conqueredChunks.keySet()) {
            // values in tag list must all be same, so all types are changed to use int arrays
            NBTTagList keyValPair = new NBTTagList();
            keyValPair.appendTag(new NBTTagIntArray(new int[]{chunkPosKey.dim, chunkPosKey.x, chunkPosKey.z}));
            keyValPair.appendTag(new NBTTagIntArray(UUIDToBEIntArray(conqueredChunks.get(chunkPosKey).getObj())));
            keyValPair.appendTag(new NBTTagIntArray(new int[]{conqueredChunks.get(chunkPosKey).getInteger()}));

            conqueredChunksDataList.setTag("conqueredChunk_" + index, keyValPair);

            ++index;
        }

        tags.setTag("conqueredChunks", conqueredChunksDataList);
    }

    public void opResetFlagCooldowns() {
        for (HashMap.Entry<UUID, Faction> kvp : mFactions.entrySet()) {
            for (HashMap.Entry<UUID, PlayerData> pDataKVP : kvp.getValue().members.entrySet()) {
                //pDataKVP.getValue().mHasMovedFlagToday = false;
                pDataKVP.getValue().moveFlagCooldown = 0;
            }
        }
    }

    public void requestNamePlateCacheEntry(EntityPlayerMP playerEntity, String name) {
        PacketNamePlateChange packet = new PacketNamePlateChange();
        packet.name = name;
        EntityPlayerMP targetPlayer = MC_SERVER.getPlayerList().getPlayerByUsername(name);
        if (targetPlayer == null) {
            return; //Likely bruteforcer TODO:Kick him
        }
        if (playerEntity.dimension != targetPlayer.dimension) {
            return; //Also sus
        }


        double dx = playerEntity.posX - targetPlayer.posX;
        double dz = playerEntity.posZ - targetPlayer.posZ;
        double dy = playerEntity.posY - targetPlayer.posY;

        int viewDistanceChunks = playerEntity.world.getMinecraftServer().getPlayerList().getViewDistance();
        int maxDistanceBlocks = viewDistanceChunks * 16;
        double maxDistanceSq = maxDistanceBlocks * maxDistanceBlocks;
        int distance = (int) (dx * dx + dy * dy + dz * dz);
        if (distance > maxDistanceSq) {
            WarForgeMod.LOGGER.warn(playerEntity.getName() + "Made a nameplate request for player " + name + "who is" + distance + "blocks away.");
            packet.isRemove = false;
            WarForgeMod.NETWORK.sendTo(packet, playerEntity);
            return;
        }
        if (getFactionOfPlayer(targetPlayer.getUniqueID()) == null)
            return;

        Faction faction = getFactionOfPlayer(targetPlayer.getUniqueID());
        if (faction != null) {
            packet.faction = faction.name;
            packet.color = faction.colour;
        }
        WarForgeMod.NETWORK.sendTo(packet, playerEntity);

    }

    public static enum siegeTermination {
        WIN, LOSE, NEUTRAL
    }

    private static class ClaimedBlockSelection {
        private final int slot;
        private final Faction.ClaimType claimType;

        private ClaimedBlockSelection(int slot, Faction.ClaimType claimType) {
            this.slot = slot;
            this.claimType = claimType;
        }

        private void consume(InventoryPlayer inventory) {
            ItemStack stack = inventory.mainInventory.get(slot);
            stack.shrink(1);
            if (stack.isEmpty()) {
                inventory.mainInventory.set(slot, ItemStack.EMPTY);
            }
        }
    }
}
