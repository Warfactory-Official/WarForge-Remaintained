package com.flansmod.warforge.common;

import com.flansmod.warforge.common.network.PacketTimeUpdates;
import com.flansmod.warforge.common.util.TimeHelper;
import com.flansmod.warforge.compat.Nbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Server clocks (wall or tick based per config) and the siege/yield day counters. */
public final class WarClock {
    public static long numberOfSiegeDaysTicked = 0L;
    public static long numberOfYieldDaysTicked = 0L;
    public static long timestampOfFirstDay = 0L;
    public static long previousUpdateTimestamp = 0L;
    public static long serverTick = 0L;
    public static long currTickTimestamp = 0L;
    public static long serverStopTimestamp = 0L;
    public static long gameTimeMs = 0L;

    public static final TimeHelper TIME = new TimeHelper();

    private WarClock() {}

    private static long getTime(boolean tickBased) {
        return tickBased ? gameTimeMs : System.currentTimeMillis();
    }

    public static long yieldClock() { return getTime(WarForgeConfig.TICK_YIELDS); }
    public static long siegeClock() { return getTime(WarForgeConfig.TICK_SIEGES); }
    public static long momentumClock() { return getTime(WarForgeConfig.TICK_MOMENTUM); }
    public static long offlineProtectionClock() { return getTime(WarForgeConfig.TICK_OFFLINE_PROTECTION); }
    public static long graceClock() { return getTime(WarForgeConfig.TICK_SIEGE_GRACE); }
    public static long citadelMoveClock() { return getTime(WarForgeConfig.TICK_CITADEL_MOVE); }
    public static long truceClock() { return getTime(WarForgeConfig.TICK_TRUCES); }
    public static long factionRejoinClock() { return getTime(WarForgeConfig.TICK_FACTION_REJOIN); }

    public static void start() {
        timestampOfFirstDay = System.currentTimeMillis();
        numberOfSiegeDaysTicked = 0L;
        numberOfYieldDaysTicked = 0L;
    }

    public static void read(CompoundTag tags) {
        timestampOfFirstDay = Nbt.getLong(tags, "zero-timestamp");
        numberOfSiegeDaysTicked = Nbt.getLong(tags, "num-days-elapsed");
        numberOfYieldDaysTicked = Nbt.getLong(tags, "num-yields-awarded");
        serverStopTimestamp = Nbt.getLong(tags, "shutdown-timestamp");
        gameTimeMs = Nbt.getLong(tags, "game-time-ms");
        currTickTimestamp = siegeClock();
    }

    public static void write(CompoundTag tags) {
        tags.putLong("zero-timestamp", timestampOfFirstDay);
        tags.putLong("num-days-elapsed", numberOfSiegeDaysTicked);
        tags.putLong("num-yields-awarded", numberOfYieldDaysTicked);
        tags.putLong("shutdown-timestamp", serverStopTimestamp);
        tags.putLong("game-time-ms", gameTimeMs);
    }

    /** One server tick: advances game time, runs siege/yield timers, announces new days. */
    public static void tick() {
        gameTimeMs += 50L;
        previousUpdateTimestamp = currTickTimestamp;
        currTickTimestamp = siegeClock();
        if (previousUpdateTimestamp == 0L) previousUpdateTimestamp = currTickTimestamp;

        WarForgeMod.FACTIONS.updateConqueredChunks(currTickTimestamp);

        ++serverTick;

        boolean dayChanged = false;

        if (!WarForgeConfig.SIEGE_ENABLE_NEW_TIMER) {
            long siegeDayNumber = (currTickTimestamp - timestampOfFirstDay) / TimeHelper.getSiegeDayLengthMS();

            if (siegeDayNumber > numberOfSiegeDaysTicked) {
                numberOfSiegeDaysTicked = siegeDayNumber;
                Notifications.messageAll(Component.translatable("warforge.common.siege_day_advanced"));
                WarForgeMod.FACTIONS.siegeService.advanceDay();
                dayChanged = true;
            } else if (siegeDayNumber < numberOfSiegeDaysTicked) {
                numberOfSiegeDaysTicked = siegeDayNumber;
            }
        } else {
            WarForgeMod.FACTIONS.siegeService.updateTimers();
        }

        WarForgeMod.FACTIONS.siegeSync.tickHeartbeat();

        long yieldDayNumber = (yieldClock() - timestampOfFirstDay) / TimeHelper.getYieldDayLengthMs();

        if (yieldDayNumber > numberOfYieldDaysTicked) {
            numberOfYieldDaysTicked = yieldDayNumber;
            Notifications.messageAll(Component.translatable("warforge.common.yields_awarded"));
            WarForgeMod.FACTIONS.advanceYieldDay();
            dayChanged = true;
        } else if (yieldDayNumber < numberOfYieldDaysTicked) {
            numberOfYieldDaysTicked = yieldDayNumber;
        }

        if (dayChanged) {
            WarForgeMod.NETWORK.sendToAll(timeUpdate());
        }
    }

    public static PacketTimeUpdates timeUpdate() {
        PacketTimeUpdates packet = new PacketTimeUpdates();
        if (!WarForgeConfig.SIEGE_ENABLE_NEW_TIMER) {
            packet.msUntilNextSiegeDay = TIME.getTimeToNextSiegeAdvanceMs();
        }
        packet.msUntilNextYieldDay = TIME.getTimeToNextYieldMs();
        return packet;
    }
}
