package com.flansmod.warforge.compat.ftbteams;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.faction.Role;
import com.flansmod.warforge.platform.GameEvents;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.FactionTeams;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import dev.ftb.mods.ftbteams.data.TeamManagerImpl;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

/** Faction == FTB party team with the same UUID, so FTB Quests progress and rewards are shared by faction members. */
public final class FtbTeamsLink implements FactionTeams.Link {
    // FTB creates the personal team in its own login handler; sync one tick later.
    private final HashSet<UUID> pendingLogins = new HashSet<>();

    private FtbTeamsLink() {}

    public static void register() {
        FtbTeamsLink link = new FtbTeamsLink();
        FactionTeams.install(link);
        GameEvents.onServerTickEnd(link::drainLogins);
    }

    private static TeamManagerImpl manager() {
        return FTBTeamsAPI.api().isManagerLoaded() ? (TeamManagerImpl) FTBTeamsAPI.api().getManager() : null;
    }

    private static boolean ours(Team team) {
        return team.isPartyTeam() && WarForgeMod.FACTIONS.registry.getFaction(team.getId()) != null;
    }

    @Override
    public void playerLoggedIn(UUID player) {
        pendingLogins.add(player);
    }

    private void drainLogins() {
        if (pendingLogins.isEmpty()) return;
        TeamManagerImpl mgr = manager();
        for (UUID id : new ArrayList<>(pendingLogins)) {
            pendingLogins.remove(id);
            if (mgr == null) continue;
            Faction faction = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(id);
            if (faction != null) {
                sync(faction);
                continue;
            }
            Team team = mgr.getTeamForPlayerID(id).orElse(null);
            if (team != null && ours(team)) leave(team, id);
        }
    }

    @Override
    public void sync(Faction faction) {
        TeamManagerImpl mgr = manager();
        if (mgr == null) return;
        FTBTeamsAPI.api().setPartyCreationFromAPIOnly(WarForgeConfig.FTB_LOCK_PARTY_CREATION);
        ServerPlayer leader = WarForgeMod.MC_SERVER.getPlayerList().getPlayer(faction.getLeaderId());
        PartyTeam party = mgr.getTeamByID(faction.getUuid()).filter(t -> t instanceof PartyTeam).map(t -> (PartyTeam) t).orElse(null);
        if (party == null) {
            ServerPlayer founder = leader != null ? leader : firstOnlineMember(faction);
            if (founder == null) return;
            leaveForeignParty(mgr, founder, faction);
            try {
                party = mgr.createParty(faction.getUuid(), founder, faction.getName(), "", Color4I.rgb(faction.getColour()));
            } catch (CommandSyntaxException e) {
                WarForgeMod.LOGGER.warn("FTB Teams refused party for faction {}: {}", faction.getName(), e.getMessage());
                return;
            }
        }
        if (!faction.getName().equals(party.getProperty(TeamProperties.DISPLAY_NAME))) {
            party.setProperty(TeamProperties.DISPLAY_NAME, faction.getName());
        }
        Color4I colour = Color4I.rgb(faction.getColour());
        if (!colour.equals(party.getProperty(TeamProperties.COLOR))) {
            party.setProperty(TeamProperties.COLOR, colour);
        }
        for (UUID id : faction.getMemberIds()) {
            ServerPlayer member = WarForgeMod.MC_SERVER.getPlayerList().getPlayer(id);
            if (member == null || party.getMembers().contains(id)) continue;
            leaveForeignParty(mgr, member, faction);
            try {
                party.join(member);
            } catch (CommandSyntaxException e) {
                WarForgeMod.LOGGER.warn("FTB Teams refused {} joining {}: {}", member.getName().getString(), faction.getName(), e.getMessage());
            }
        }
        UUID leaderId = faction.getLeaderId();
        if (FactionIds.isValid(leaderId) && party.getMembers().contains(leaderId) && !leaderId.equals(party.getOwner())) {
            try {
                FtbProfiles.transferOwnership(party, WarForgeMod.MC_SERVER, leaderId);
            } catch (CommandSyntaxException e) {
                WarForgeMod.LOGGER.warn("FTB Teams refused owner change in {}: {}", faction.getName(), e.getMessage());
            }
        }
        for (UUID id : new ArrayList<>(party.getMembers())) {
            if (!faction.isPlayerInFaction(id)) leave(party, id);
        }
        boolean ranksChanged = false;
        for (UUID id : party.getMembers()) {
            if (id.equals(party.getOwner())) continue;
            TeamRank want = faction.getRole(id) == Role.OFFICER ? TeamRank.OFFICER : TeamRank.MEMBER;
            if (party.getRankForPlayer(id) != want) {
                party.addMember(id, want);
                ranksChanged = true;
            }
        }
        if (ranksChanged) {
            party.markDirty();
            mgr.syncToAll(party);
        }
    }

    @Override
    public void dissolve(UUID factionId, Collection<UUID> formerMembers) {
        TeamManagerImpl mgr = manager();
        if (mgr == null) return;
        Team team = mgr.getTeamByID(factionId).orElse(null);
        if (team == null || !ours(team)) return;
        UUID owner = team.getOwner();
        for (UUID id : new ArrayList<>(team.getMembers())) {
            if (!id.equals(owner)) leave(team, id);
        }
        leave(team, owner);
    }

    private static ServerPlayer firstOnlineMember(Faction faction) {
        for (UUID id : faction.getMemberIds()) {
            ServerPlayer player = WarForgeMod.MC_SERVER.getPlayerList().getPlayer(id);
            if (player != null) return player;
        }
        return null;
    }

    private static void leaveForeignParty(TeamManagerImpl mgr, ServerPlayer player, Faction faction) {
        Team current = mgr.getTeamForPlayerID(player.getUUID()).orElse(null);
        if (current != null && current.isPartyTeam() && !current.getId().equals(faction.getUuid())) {
            leave(current, player.getUUID());
        }
    }

    private static void leave(Team party, UUID player) {
        try {
            ((PartyTeam) party).leave(player);
        } catch (CommandSyntaxException e) {
            WarForgeMod.LOGGER.warn("FTB Teams refused {} leaving party {}: {}", player, party.getShortName(), e.getMessage());
        }
    }
}
