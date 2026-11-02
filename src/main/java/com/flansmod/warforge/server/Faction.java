package com.flansmod.warforge.server;

import com.flansmod.warforge.common.SiegeMomentum;
import com.flansmod.warforge.common.WarClock;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.ClaimType;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.faction.Role;
import com.flansmod.warforge.common.network.FactionDisplayInfo;
import com.flansmod.warforge.common.network.PlayerDisplayInfo;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.compat.Ids;
import com.flansmod.warforge.compat.Keys;
import com.flansmod.warforge.compat.Nbt;
import com.flansmod.warforge.compat.PlayerRef;
import com.flansmod.warforge.compat.Profiles;
import com.flansmod.warforge.compat.Stacks;
import com.flansmod.warforge.server.Leaderboard.FactionStat;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/** Faction state. Claim collections are mutated only by {@link ClaimRegistry}; the player index by {@link MembershipService}. */
public class Faction {
    @Getter
    private UUID uuid;
    @Getter
    private String name;
    @Getter
    private String flagId = "";
    @Getter
    private DimBlockPos citadelPos;
    @Getter
    private int colour = 0xFF_FF_FF;
    @Getter
    private int notoriety = 0;
    @Getter
    private int wealth = 0;
    @Getter
    private int legacy = 0;
    @Getter
    private short citadelLevel = 0;
    @Getter
    private long lastSiegeTimestamp = 0;
    @Getter
    private long offlineRaidProtectionUntil = 0L;
    @Getter
    private boolean offlineRaidProtectionDisabled = false;
    // 0 = none (never granted, expired, or forfeited by starting a siege)
    @Getter
    private long siegeGraceUntil = 0L;
    @Getter
    private long citadelMoveTimeStamp = 0;
    @Getter
    private boolean allowAllyInteraction = false;
    private boolean loggedInToday;
    private byte siegeMomentum = 0;
    @Getter
    private long momentumExpiryTimestamp = 0L;

    private final HashMap<DimBlockPos, Integer> claims = new HashMap<>();
    private final HashMap<DimBlockPos, ClaimType> claimTypes = new HashMap<>();
    private final HashMap<DimChunkPos, DimBlockPos> claimByChunk = new HashMap<>();
    private final HashSet<DimChunkPos> forcedChunks = new HashSet<>();
    private final HashSet<DimBlockPos> islandCollectors = new HashSet<>();
    private final HashMap<UUID, PlayerData> members = new HashMap<>();
    private final HashSet<UUID> pendingInvites = new HashSet<>();
    private final HashMap<UUID, Integer> killCounter = new HashMap<>();
    private final ArrayList<ItemStack> insuranceStacks = new ArrayList<>();
    private final HashSet<UUID> allies = new HashSet<>();
    private final HashSet<UUID> pendingAllianceRequests = new HashSet<>();
    private final HashMap<UUID, Long> truces = new HashMap<>();

    Faction() {
    }

    Faction(UUID uuid, String name, int colour, DimBlockPos citadelPos) {
        this.uuid = uuid;
        this.name = name;
        this.colour = colour;
        this.citadelPos = citadelPos;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFlagId(String flagId) {
        this.flagId = flagId;
    }

    public void setColour(int colour) {
        this.colour = colour;
    }

    void setCitadelPos(DimBlockPos citadelPos) {
        this.citadelPos = citadelPos;
    }

    public void addNotoriety(int amount) {
        notoriety += amount;
    }

    public void clearNotoriety() {
        notoriety = 0;
    }

    public void clearLegacy() {
        legacy = 0;
    }

    void addWealth(int delta) {
        wealth += delta;
    }

    void setWealth(int wealth) {
        this.wealth = wealth;
    }

    public void incrementCitadelLevel() {
        citadelLevel++;
    }

    public void setLastSiegeTimestamp(long timestamp) {
        lastSiegeTimestamp = timestamp;
    }

    public void setOfflineRaidProtectionUntil(long until) {
        offlineRaidProtectionUntil = until;
    }

    public void setOfflineRaidProtectionDisabled(boolean disabled) {
        offlineRaidProtectionDisabled = disabled;
    }

    public void grantSiegeGrace(long until) {
        siegeGraceUntil = until;
    }

    public void forfeitSiegeGrace() {
        siegeGraceUntil = 0L;
    }

    public void setCitadelMoveTimeStamp(long timestamp) {
        citadelMoveTimeStamp = timestamp;
    }

    public void setAllowAllyInteraction(boolean allow) {
        allowAllyInteraction = allow;
    }

    /** @return true if the level rose; false if capped (expiry still extended) */
    public boolean increaseSiegeMomentum() {
        boolean increased = false;
        if (siegeMomentum < SiegeMomentum.max()) {
            siegeMomentum++;
            increased = true;
        }
        momentumExpiryTimestamp = WarClock.momentumClock() + (long) WarForgeConfig.SIEGE_MOMENTUM_DURATION * 60 * 1000;
        return increased;
    }

    public void stopMomentum() {
        siegeMomentum = 0;
        momentumExpiryTimestamp = 0L;
    }

    public byte getSiegeMomentum() {
        return WarClock.momentumClock() > momentumExpiryTimestamp ? 0 : siegeMomentum;
    }

    public void update() {
        if (!loggedInToday && hasOnlineMember()) {
            loggedInToday = true;
        }
    }

    public boolean hasLoggedInToday() {
        return loggedInToday;
    }

    public void increaseLegacy() {
        if (loggedInToday) {
            legacy += WarForgeConfig.LEGACY_PER_DAY;
        }
        loggedInToday = false;
    }

    /** @param excluded member not counted (a player mid-logout is still in the player list); may be null */
    public int countOnlineMembers(UUID excluded) {
        int count = 0;
        for (UUID id : members.keySet()) {
            if (!id.equals(excluded) && getPlayer(id) != null) {
                count++;
            }
        }
        return count;
    }

    public boolean hasOnlineMember() {
        for (UUID id : members.keySet()) {
            if (getPlayer(id) != null) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Player> getOnlinePlayers(Predicate<Player> playerCondition) {
        ArrayList<Player> players = new ArrayList<>(members.size());
        for (UUID playerID : members.keySet()) {
            Player player = getPlayer(playerID);
            if (player != null && playerCondition.test(player)) players.add(player);
        }
        return players;
    }

    private static Player getPlayer(UUID playerID) {
        return WarForgeMod.MC_SERVER.getPlayerList().getPlayer(playerID);
    }

    /** Protected from sieges while every member is offline (until the window set at the last logout ends). */
    public boolean isOfflineRaidProtected() {
        return WarForgeConfig.ENABLE_OFFLINE_RAID_PROTECTION && !offlineRaidProtectionDisabled
                && !hasOnlineMember() && WarClock.offlineProtectionClock() < offlineRaidProtectionUntil;
    }

    /** New-faction grace: unsiegeable until it expires or the faction starts a siege itself. */
    public boolean isSiegeGraceProtected() {
        return WarForgeConfig.ENABLE_SIEGE_GRACE_PERIOD && WarClock.graceClock() < siegeGraceUntil;
    }

    public int getMemberCount() {
        return members.size();
    }

    public Set<UUID> getMemberIds() {
        return Collections.unmodifiableSet(members.keySet());
    }

    public boolean isPlayerInFaction(UUID playerID) {
        return members.containsKey(playerID);
    }

    /** @return null if not a member */
    public Role getRole(UUID playerID) {
        PlayerData data = members.get(playerID);
        return data == null ? null : data.role;
    }

    public boolean isPlayerRoleInFaction(UUID playerID, Role role) {
        Role actual = getRole(playerID);
        return actual != null && actual.atLeast(role);
    }

    public boolean isPlayerOutrankingOfficer(UUID playerID, UUID targetID) {
        Role playerRole = getRole(playerID);
        Role targetRole = getRole(targetID);
        return playerRole != null && targetRole != null
                && playerRole.atLeast(Role.OFFICER)
                && playerRole.ordinal() > targetRole.ordinal();
    }

    public UUID getLeaderId() {
        for (Map.Entry<UUID, PlayerData> entry : members.entrySet()) {
            if (entry.getValue().role == Role.LEADER) {
                return entry.getKey();
            }
        }
        return FactionIds.NONE;
    }

    void addMember(UUID playerID) {
        members.put(playerID, new PlayerData());
    }

    void removeMember(UUID playerID) {
        members.remove(playerID);
    }

    void clearMembers() {
        members.clear();
    }

    /** @return false if not a member */
    boolean setLeader(UUID playerID) {
        if (!members.containsKey(playerID)) {
            return false;
        }
        for (Map.Entry<UUID, PlayerData> entry : members.entrySet()) {
            if (entry.getKey().equals(playerID)) {
                entry.getValue().role = Role.LEADER;
            } else if (entry.getValue().role == Role.LEADER) {
                entry.getValue().role = Role.OFFICER;
            }
        }
        return true;
    }

    /** @return true if MEMBER -> OFFICER happened */
    boolean promote(UUID playerID) {
        PlayerData data = members.get(playerID);
        if (data == null || data.role != Role.MEMBER) {
            return false;
        }
        data.role = Role.OFFICER;
        return true;
    }

    /** @return true if OFFICER -> MEMBER happened */
    boolean demote(UUID playerID) {
        PlayerData data = members.get(playerID);
        if (data == null || data.role != Role.OFFICER) {
            return false;
        }
        data.role = Role.MEMBER;
        return true;
    }

    public boolean isInvitingPlayer(UUID playerID) {
        return pendingInvites.contains(playerID);
    }

    void addInvite(UUID playerID) {
        pendingInvites.add(playerID);
    }

    void removeInvite(UUID playerID) {
        pendingInvites.remove(playerID);
    }

    /** @return kills of {@code victim} by this faction after counting this one */
    int recordKill(UUID victim) {
        return killCounter.merge(victim, 1, Integer::sum);
    }

    void forgetKillsOf(UUID victim) {
        killCounter.remove(victim);
    }

    public Set<UUID> getAllies() {
        return Collections.unmodifiableSet(allies);
    }

    public Set<UUID> getPendingAllianceRequests() {
        return Collections.unmodifiableSet(pendingAllianceRequests);
    }

    public boolean isAllyOf(UUID factionID) {
        return factionID != null && allies.contains(factionID);
    }

    public boolean hasPendingAllianceRequestFrom(UUID factionID) {
        return pendingAllianceRequests.contains(factionID);
    }

    void addAlly(UUID factionID) {
        allies.add(factionID);
    }

    void removeAlly(UUID factionID) {
        allies.remove(factionID);
    }

    void addAllianceRequest(UUID factionID) {
        pendingAllianceRequests.add(factionID);
    }

    boolean removeAllianceRequest(UUID factionID) {
        return pendingAllianceRequests.remove(factionID);
    }

    void setTruce(UUID factionID, long expiryMs) {
        truces.put(factionID, expiryMs);
    }

    void removeTruce(UUID factionID) {
        truces.remove(factionID);
    }

    /** Expired truces are pruned lazily. */
    public boolean isInTruceWith(UUID factionID) {
        if (factionID == null) {
            return false;
        }
        Long expiry = truces.get(factionID);
        if (expiry == null) {
            return false;
        }
        if (expiry <= WarClock.truceClock()) {
            truces.remove(factionID);
            return false;
        }
        return true;
    }

    public long getTruceRemainingMs(UUID factionID) {
        Long expiry = truces.get(factionID);
        return expiry == null ? 0L : Math.max(0L, expiry - WarClock.truceClock());
    }

    public int getClaimCount() {
        return claims.size();
    }

    public Set<DimBlockPos> getClaimPositions() {
        return Collections.unmodifiableSet(claims.keySet());
    }

    /** @return null if the faction holds no claim in the chunk */
    public DimBlockPos getSpecificPosForClaim(DimChunkPos pos) {
        return claimByChunk.get(pos);
    }

    public boolean ownsClaimAt(DimBlockPos pos) {
        return claims.containsKey(pos);
    }

    public ClaimType getClaimType(DimChunkPos pos) {
        DimBlockPos claimPos = claimByChunk.get(pos);
        if (claimPos == null) {
            return ClaimType.NONE;
        }
        return claimTypes.getOrDefault(claimPos, claimPos.equals(citadelPos) ? ClaimType.CITADEL : ClaimType.BASIC);
    }

    public int getPendingYields(DimBlockPos pos) {
        return claims.getOrDefault(pos, 0);
    }

    public int takePendingYields(DimBlockPos pos) {
        Integer pending = claims.replace(pos, 0);
        return pending == null ? 0 : pending;
    }

    void putClaim(DimBlockPos pos, ClaimType type, int pendingYields) {
        claims.put(pos, pendingYields);
        claimTypes.put(pos, type);
        claimByChunk.put(pos.toChunkPos(), pos);
    }

    /** @return pending yields of the removed claim, 0 if absent */
    int removeClaim(DimBlockPos pos) {
        Integer pending = claims.remove(pos);
        claimTypes.remove(pos);
        claimByChunk.remove(pos.toChunkPos());
        return pending == null ? 0 : pending;
    }

    void clearClaims() {
        claims.clear();
        claimTypes.clear();
        claimByChunk.clear();
    }

    public boolean canPlaceClaim() {
        if (WarForgeConfig.ENABLE_CITADEL_UPGRADES) {
            int claimLimitForLevel = WarForgeMod.UPGRADE_HANDLER.getClaimLimitForLevel(citadelLevel);
            if (claimLimitForLevel != -1 && claims.size() >= claimLimitForLevel) {
                return false;
            }
        }
        return WarForgeConfig.MAX_CLAIMS_PER_FACTION < 0 || claims.size() < WarForgeConfig.MAX_CLAIMS_PER_FACTION;
    }

    /** Pending yields +1 per claim in a loaded dimension. */
    void incrementPendingYields() {
        for (Map.Entry<DimBlockPos, Integer> kvp : claims.entrySet()) {
            if (WarForgeMod.MC_SERVER.getLevel(kvp.getKey().dim) != null) {
                kvp.setValue(kvp.getValue() + 1);
            }
        }
    }

    public Set<DimChunkPos> getForcedChunks() {
        return Collections.unmodifiableSet(forcedChunks);
    }

    public boolean isForceLoaded(DimChunkPos chunk) {
        return forcedChunks.contains(chunk);
    }

    boolean forceLoad(DimChunkPos chunk) {
        return forcedChunks.add(chunk);
    }

    boolean unforceLoad(DimChunkPos chunk) {
        return forcedChunks.remove(chunk);
    }

    public Set<DimBlockPos> getIslandCollectors() {
        return Collections.unmodifiableSet(islandCollectors);
    }

    boolean addIslandCollector(DimBlockPos pos) {
        return islandCollectors.add(pos);
    }

    boolean removeIslandCollector(DimBlockPos pos) {
        return islandCollectors.remove(pos);
    }

    boolean removeIslandCollectors(Collection<DimBlockPos> positions) {
        return islandCollectors.removeAll(positions);
    }

    void clearIslandCollectors() {
        islandCollectors.clear();
    }

    void clearForcedChunks() {
        forcedChunks.clear();
    }

    public boolean canForceLoadMore() {
        return forcedChunks.size() < getMaxForceLoadedChunks();
    }

    public int getMaxForceLoadedChunks() {
        if (WarForgeConfig.ENABLE_CITADEL_UPGRADES) {
            return Math.max(0, WarForgeMod.UPGRADE_HANDLER.getLoadedChunksForLevel(citadelLevel));
        }
        return Math.max(0, WarForgeConfig.FORCE_LOADED_CHUNKS_TOTAL);
    }

    public int getMaxFobs() {
        if (WarForgeConfig.ENABLE_CITADEL_UPGRADES) {
            return Math.max(0, WarForgeMod.UPGRADE_HANDLER.getMaxFobsForLevel(citadelLevel));
        }
        return Math.max(0, WarForgeConfig.MAX_FOBS);
    }

    public int getFobTicketLimit() {
        if (WarForgeConfig.ENABLE_CITADEL_UPGRADES) {
            return Math.max(0, WarForgeMod.UPGRADE_HANDLER.getFobTicketLimitForLevel(citadelLevel));
        }
        return Math.max(0, WarForgeConfig.FOB_TICKET_LIMIT);
    }

    public int getFobRegenPerSiegeTick() {
        if (WarForgeConfig.ENABLE_CITADEL_UPGRADES) {
            return Math.max(0, WarForgeMod.UPGRADE_HANDLER.getFobRegenForLevel(citadelLevel));
        }
        return Math.max(0, WarForgeConfig.FOB_TICKET_REGEN_PER_SIEGE_TICK);
    }

    public int getInsuranceSlotCount() {
        return Math.max(insuranceStacks.size(), WarForgeMod.UPGRADE_HANDLER.getInsuranceSlotsForLevel(citadelLevel));
    }

    public ItemStack getInsuranceStack(int slot) {
        ensureInsuranceSize(slot + 1);
        return insuranceStacks.get(slot);
    }

    public void setInsuranceStack(int slot, ItemStack stack) {
        ensureInsuranceSize(slot + 1);
        insuranceStacks.set(slot, stack == null ? ItemStack.EMPTY : stack);
    }

    public boolean hasInsuranceContents() {
        for (ItemStack stack : insuranceStacks) {
            if (stack != null && !stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<ItemStack> drainInsuranceContents() {
        ArrayList<ItemStack> contents = new ArrayList<>();
        for (int i = 0; i < insuranceStacks.size(); i++) {
            ItemStack stack = insuranceStacks.get(i);
            if (stack != null && !stack.isEmpty()) {
                contents.add(stack.copy());
                insuranceStacks.set(i, ItemStack.EMPTY);
            }
        }
        return contents;
    }

    void clearInsurance() {
        insuranceStacks.clear();
    }

    private void ensureInsuranceSize(int targetSize) {
        while (insuranceStacks.size() < targetSize) {
            insuranceStacks.add(ItemStack.EMPTY);
        }
    }

    public void messageAll(Component chat) {
        for (UUID playerID : members.keySet()) {
            Player player = getPlayer(playerID);
            if (player != null) {
                player.sendSystemMessage(chat);
            }
        }
    }

    public void soundEffectAll(SoundEvent soundEvent, float volume, float pitch) {
        for (UUID playerID : members.keySet()) {
            ServerPlayer player = (ServerPlayer) getPlayer(playerID);
            if (player != null && !player.hasDisconnected()) {
                player.connection.send(new ClientboundSoundPacket(
                        Holder.direct(soundEvent),
                        SoundSource.PLAYERS,
                        player.getX(), player.getY(), player.getZ(),
                        volume, pitch,
                        0L
                ));
            }
        }
    }

    public void soundEffectAll(SoundEvent soundEvent) {
        soundEffectAll(soundEvent, 1f, 1f);
    }

    /** Citadel position is filled in only for members; outsiders get {@link DimBlockPos#ZERO}. */
    public FactionDisplayInfo createInfo(UUID viewerId) {
        FactionDisplayInfo info = new FactionDisplayInfo();
        info.factionId = uuid;
        info.factionName = name;
        info.colour = colour;
        info.flagId = flagId;
        info.notoriety = notoriety;
        info.wealth = wealth;
        info.legacy = legacy;
        info.lvl = citadelLevel;

        Leaderboard leaderboard = WarForgeMod.LEADERBOARD;
        info.legacyRank = leaderboard.getOneIndexedRankOf(this, FactionStat.LEGACY);
        info.notorietyRank = leaderboard.getOneIndexedRankOf(this, FactionStat.NOTORIETY);
        info.wealthRank = leaderboard.getOneIndexedRankOf(this, FactionStat.WEALTH);
        info.totalRank = leaderboard.getOneIndexedRankOf(this, FactionStat.TOTAL);

        info.numClaims = claims.size();
        info.citadelPos = (viewerId != null && isPlayerInFaction(viewerId)) ? citadelPos : DimBlockPos.ZERO;

        boolean viewerIsMember = viewerId != null && isPlayerInFaction(viewerId);
        for (Map.Entry<UUID, PlayerData> entry : members.entrySet()) {
            if (entry.getValue().role == Role.LEADER) {
                info.leaderId = entry.getKey();
            } else if (!viewerIsMember) {
                continue;
            }
            PlayerDisplayInfo playerInfo = new PlayerDisplayInfo();
            PlayerRef profile = Profiles.get(WarForgeMod.MC_SERVER, entry.getKey()).orElse(null);
            playerInfo.username = profile == null ? "Unknown Player" : profile.getName();
            playerInfo.playerUuid = entry.getKey();
            playerInfo.role = entry.getValue().role;
            info.members.add(playerInfo);
        }
        return info;
    }

    /** Fobs persist under the same compound ("fobs"), written by FobManager. */
    public void readFromNBT(CompoundTag tags) {
        clearClaims();
        forcedChunks.clear();
        islandCollectors.clear();
        members.clear();
        pendingInvites.clear();
        allies.clear();
        pendingAllianceRequests.clear();
        truces.clear();
        killCounter.clear();

        uuid = Nbt.getUUID(tags, "uuid");
        name = Nbt.getString(tags, "name");
        colour = Nbt.getInt(tags, "colour");
        citadelLevel = Nbt.getShort(tags, "citadel_lvl");
        citadelPos = DimBlockPos.readFromNBT(tags, "citadelPos");

        for (Tag base : Nbt.getList(tags, "claims", Tag.TAG_COMPOUND)) {
            CompoundTag claimInfo = (CompoundTag) base;
            DimBlockPos pos = DimBlockPos.readFromNBT(Nbt.getCompound(claimInfo, "pos"));
            int pendingYields = Nbt.getInt(claimInfo, "pendingYields");
            ClaimType claimType = ClaimType.fromSerialized(Nbt.getString(claimInfo, "type"));
            if (claimType == ClaimType.NONE && pos.equals(citadelPos)) {
                claimType = ClaimType.CITADEL;
            }
            putClaim(pos, claimType, pendingYields);
        }
        if (FactionIds.isNeutralZone(uuid)) {
            // Zones have no citadel; the sentinel citadelPos must not hold a claim
            if (claimTypes.get(citadelPos) == ClaimType.CITADEL) {
                removeClaim(citadelPos);
            }
        } else if (!claims.containsKey(citadelPos)) {
            WarForgeMod.LOGGER.error("Citadel of faction {} was not claimed; forcing claim", name);
            putClaim(citadelPos, ClaimType.CITADEL, 0);
        }

        for (Tag base : Nbt.getList(tags, "kills", Tag.TAG_COMPOUND)) {
            CompoundTag killInfo = (CompoundTag) base;
            killCounter.put(Nbt.getUUID(killInfo, "id"), Nbt.getInt(killInfo, "count"));
        }

        for (Tag base : Nbt.getList(tags, "forcedChunks", Tag.TAG_COMPOUND)) {
            DimChunkPos chunkPos = readChunkPosFromNBT((CompoundTag) base);
            if (chunkPos != null) {
                forcedChunks.add(chunkPos);
            }
        }

        for (Tag base : Nbt.getList(tags, "collectors", Tag.TAG_COMPOUND)) {
            DimBlockPos collectorPos = DimBlockPos.readFromNBT((CompoundTag) base);
            if (!collectorPos.equals(DimBlockPos.ZERO)) {
                islandCollectors.add(collectorPos);
            }
        }

        flagId = Nbt.getString(tags, "flagId");
        notoriety = Nbt.getInt(tags, "notoriety");
        wealth = Nbt.getInt(tags, "wealth");
        legacy = Nbt.getInt(tags, "legacy");
        offlineRaidProtectionUntil = Nbt.getLong(tags, "offlineRaidProtectionUntil");
        offlineRaidProtectionDisabled = Nbt.getBoolean(tags, "offlineRaidProtectionDisabled");
        siegeGraceUntil = Nbt.getLong(tags, "siegeGraceUntil");
        citadelMoveTimeStamp = Nbt.getLong(tags, "citadelMoveTimestamp");
        lastSiegeTimestamp = Nbt.getLong(tags, "lastSiegeTimestamp");
        siegeMomentum = Nbt.getByte(tags, "siegeMomentum");
        momentumExpiryTimestamp = Nbt.getLong(tags, "momentumExpireryTimestamp");
        loggedInToday = Nbt.getBoolean(tags, "loggedInToday");

        for (Tag base : Nbt.getList(tags, "members", Tag.TAG_COMPOUND)) {
            CompoundTag memberTags = (CompoundTag) base;
            PlayerData data = new PlayerData();
            data.readFromNBT(memberTags);
            members.put(Nbt.getUUID(memberTags, "uuid"), data);
        }

        for (Tag base : Nbt.getList(tags, "pendingInvites", Tag.TAG_COMPOUND)) {
            pendingInvites.add(Nbt.getUUID((CompoundTag) base, "uuid"));
        }

        allowAllyInteraction = Nbt.getBoolean(tags, "allowAllyInteraction");
        for (Tag base : Nbt.getList(tags, "allies", Tag.TAG_COMPOUND)) {
            allies.add(Nbt.getUUID((CompoundTag) base, "uuid"));
        }
        for (Tag base : Nbt.getList(tags, "pendingAllianceRequests", Tag.TAG_COMPOUND)) {
            pendingAllianceRequests.add(Nbt.getUUID((CompoundTag) base, "uuid"));
        }
        for (Tag base : Nbt.getList(tags, "truces", Tag.TAG_COMPOUND)) {
            CompoundTag truceTag = (CompoundTag) base;
            truces.put(Nbt.getUUID(truceTag, "uuid"), Nbt.getLong(truceTag, "expiry"));
        }

        insuranceStacks.clear();
        for (Tag base : Nbt.getList(tags, "insurance", Tag.TAG_COMPOUND)) {
            CompoundTag insuranceTag = (CompoundTag) base;
            int slot = Nbt.getInt(insuranceTag, "slot");
            ItemStack stack = Stacks.parse(WarForgeMod.MC_SERVER.registryAccess(), Nbt.getCompound(insuranceTag, "stack"));
            ensureInsuranceSize(slot + 1);
            insuranceStacks.set(slot, stack);
        }
    }

    public void writeToNBT(CompoundTag tags) {
        Nbt.putUUID(tags, "uuid", uuid);
        tags.putString("name", name);
        tags.putString("flagId", flagId);
        tags.putInt("colour", colour);
        tags.putShort("citadel_lvl", citadelLevel);

        ListTag claimsList = new ListTag();
        for (Map.Entry<DimBlockPos, Integer> kvp : claims.entrySet()) {
            CompoundTag claimTags = new CompoundTag();
            claimTags.put("pos", kvp.getKey().writeToNBT());
            claimTags.putInt("pendingYields", kvp.getValue());
            claimTags.putString("type", claimTypes.getOrDefault(kvp.getKey(),
                    kvp.getKey().equals(citadelPos) ? ClaimType.CITADEL : ClaimType.BASIC).serializedName);
            claimsList.add(claimTags);
        }
        tags.put("claims", claimsList);
        citadelPos.writeToNBT(tags, "citadelPos");

        ListTag killsList = new ListTag();
        for (Map.Entry<UUID, Integer> kvp : killCounter.entrySet()) {
            CompoundTag killTags = new CompoundTag();
            Nbt.putUUID(killTags, "id", kvp.getKey());
            killTags.putInt("count", kvp.getValue());
            killsList.add(killTags);
        }
        tags.put("kills", killsList);

        ListTag forceLoadList = new ListTag();
        for (DimChunkPos chunkPos : forcedChunks) {
            forceLoadList.add(writeChunkPosToNBT(chunkPos));
        }
        tags.put("forcedChunks", forceLoadList);

        ListTag collectorsList = new ListTag();
        for (DimBlockPos collectorPos : islandCollectors) {
            collectorsList.add(collectorPos.writeToNBT());
        }
        tags.put("collectors", collectorsList);

        tags.putInt("notoriety", notoriety);
        tags.putInt("wealth", wealth);
        tags.putInt("legacy", legacy);

        tags.putLong("offlineRaidProtectionUntil", offlineRaidProtectionUntil);
        tags.putBoolean("offlineRaidProtectionDisabled", offlineRaidProtectionDisabled);
        tags.putLong("siegeGraceUntil", siegeGraceUntil);
        tags.putLong("citadelMoveTimestamp", citadelMoveTimeStamp);
        tags.putLong("lastSiegeTimestamp", lastSiegeTimestamp);
        tags.putByte("siegeMomentum", siegeMomentum);
        tags.putLong("momentumExpireryTimestamp", momentumExpiryTimestamp);

        ListTag memberList = new ListTag();
        for (Map.Entry<UUID, PlayerData> kvp : members.entrySet()) {
            CompoundTag memberTags = new CompoundTag();
            Nbt.putUUID(memberTags, "uuid", kvp.getKey());
            kvp.getValue().writeToNBT(memberTags);
            memberList.add(memberTags);
        }
        tags.put("members", memberList);

        ListTag inviteList = new ListTag();
        for (UUID invitee : pendingInvites) {
            CompoundTag inviteTags = new CompoundTag();
            Nbt.putUUID(inviteTags, "uuid", invitee);
            inviteList.add(inviteTags);
        }
        tags.put("pendingInvites", inviteList);

        tags.putBoolean("allowAllyInteraction", allowAllyInteraction);
        ListTag allyList = new ListTag();
        for (UUID ally : allies) {
            CompoundTag allyTag = new CompoundTag();
            Nbt.putUUID(allyTag, "uuid", ally);
            allyList.add(allyTag);
        }
        tags.put("allies", allyList);
        ListTag allyRequestList = new ListTag();
        for (UUID requester : pendingAllianceRequests) {
            CompoundTag requestTag = new CompoundTag();
            Nbt.putUUID(requestTag, "uuid", requester);
            allyRequestList.add(requestTag);
        }
        tags.put("pendingAllianceRequests", allyRequestList);
        ListTag truceList = new ListTag();
        for (Map.Entry<UUID, Long> entry : truces.entrySet()) {
            CompoundTag truceTag = new CompoundTag();
            Nbt.putUUID(truceTag, "uuid", entry.getKey());
            truceTag.putLong("expiry", entry.getValue());
            truceList.add(truceTag);
        }
        tags.put("truces", truceList);

        ListTag insuranceList = new ListTag();
        for (int i = 0; i < insuranceStacks.size(); i++) {
            ItemStack stack = insuranceStacks.get(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            CompoundTag insuranceTag = new CompoundTag();
            insuranceTag.putInt("slot", i);
            insuranceTag.put("stack", Stacks.save(stack, WarForgeMod.MC_SERVER.registryAccess()));
            insuranceList.add(insuranceTag);
        }
        tags.put("insurance", insuranceList);
        tags.putBoolean("loggedInToday", loggedInToday);
    }

    // dimension persisted as its location string
    private static CompoundTag writeChunkPosToNBT(DimChunkPos chunkPos) {
        CompoundTag tag = new CompoundTag();
        tag.putString("dim", Keys.id(chunkPos.dim).toString());
        tag.putIntArray("pos", new int[]{chunkPos.x, chunkPos.z});
        return tag;
    }

    private static DimChunkPos readChunkPosFromNBT(CompoundTag tag) {
        if (tag.contains("dim") && tag.contains("pos")) {
            int[] data = Nbt.getIntArray(tag, "pos");
            if (data.length == 2) {
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, Ids.parse(Nbt.getString(tag, "dim")));
                return new DimChunkPos(dim, data[0], data[1]);
            }
        }
        return null;
    }

    private static final class PlayerData {
        private Role role = Role.MEMBER;

        private void readFromNBT(CompoundTag tags) {
            // Role persisted by name; unknown (e.g. removed GUEST) => MEMBER
            String stored = Nbt.getString(tags, "role");
            try {
                role = Role.valueOf(stored);
            } catch (IllegalArgumentException e) {
                role = Role.MEMBER;
                WarForgeMod.LOGGER.warn("Unknown role '{}', defaulting to MEMBER", stored);
            }
        }

        private void writeToNBT(CompoundTag tags) {
            tags.putString("role", role.name());
        }
    }
}
