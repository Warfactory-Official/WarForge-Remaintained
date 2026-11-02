package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.network.LeaderboardInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Ranking view over the live faction registry (zones excluded); sorted on demand. */
public class Leaderboard {
    public enum FactionStat {
        NOTORIETY,
        WEALTH,
        LEGACY,
        TOTAL,
    }

    private static final Comparator<Faction> BY_NOTORIETY = (a, b) -> Integer.compare(b.getNotoriety(), a.getNotoriety());
    private static final Comparator<Faction> BY_WEALTH = (a, b) -> Integer.compare(b.getWealth(), a.getWealth());
    private static final Comparator<Faction> BY_LEGACY = (a, b) -> Integer.compare(b.getLegacy(), a.getLegacy());
    private static final Comparator<Faction> BY_TOTAL = (a, b) -> Integer.compare(
            b.getLegacy() + b.getNotoriety() + b.getWealth(), a.getLegacy() + a.getNotoriety() + a.getWealth());

    public static Comparator<Faction> getSorter(FactionStat stat) {
        return switch (stat) {
            case NOTORIETY -> BY_NOTORIETY;
            case WEALTH -> BY_WEALTH;
            case LEGACY -> BY_LEGACY;
            case TOTAL -> BY_TOTAL;
        };
    }

    public List<Faction> getSortedList(FactionStat stat) {
        ArrayList<Faction> sorted = new ArrayList<>();
        for (Faction faction : WarForgeMod.FACTIONS.registry.getAllFactions()) {
            if (!FactionIds.isNeutralZone(faction.getUuid())) {
                sorted.add(faction);
            }
        }
        sorted.sort(getSorter(stat));
        return sorted;
    }

    public int getOneIndexedRankOf(Faction faction, FactionStat stat) {
        return getZeroIndexedRankOf(faction, stat) + 1;
    }

    public int getZeroIndexedRankOf(Faction faction, FactionStat stat) {
        Comparator<Faction> sorter = getSorter(stat);
        int numBetterFactions = 0;
        for (Faction other : WarForgeMod.FACTIONS.registry.getAllFactions()) {
            if (!FactionIds.isNeutralZone(other.getUuid()) && sorter.compare(faction, other) > 0) {
                numBetterFactions++;
            }
        }
        return numBetterFactions;
    }

    public LeaderboardInfo createInfo(int firstIndex, FactionStat stat, UUID playerAsking) {
        LeaderboardInfo info = new LeaderboardInfo();
        info.firstIndex = firstIndex;
        info.stat = stat;
        if (FactionIds.isValid(playerAsking)) {
            Faction faction = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(playerAsking);
            if (faction != null) {
                info.myFaction = faction.createInfo(playerAsking);
            }
        }
        List<Faction> sorted = getSortedList(stat);
        for (int i = firstIndex; i < firstIndex + LeaderboardInfo.NUM_LEADERBOARD_ENTRIES_PER_PAGE; i++) {
            // out-of-range entries stay null; client handles
            if (0 <= i && i < sorted.size()) {
                info.factionInfos[i - firstIndex] = sorted.get(i).createInfo(playerAsking);
            }
        }
        return info;
    }
}
