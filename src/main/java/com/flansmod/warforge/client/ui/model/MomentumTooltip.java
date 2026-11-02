package com.flansmod.warforge.client.ui.model;

import com.flansmod.warforge.common.SiegeMomentum;
import static com.flansmod.warforge.common.ui.UiLang.tr;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.api.Time;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class MomentumTooltip {
    private MomentumTooltip() {
    }

    public static List<Component> lines() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("§6" + tr("warforge.ui.momentum_system") + "§r"));
        lines.add(Component.literal("§7" + tr("warforge.ui.momentum_affects_siege_duration") + "§r"));
        lines.add(Component.literal("§7" + tr("warforge.ui.gained_via_s_sieges_lost_on_s_sieges", "§aWON§7", "§cLOST§7") + "§r"));
        lines.add(Component.literal("§7" + tr("warforge.ui.expires_after_s_minutes", "§b" + WarForgeConfig.SIEGE_MOMENTUM_DURATION + "§7") + "§r"));
        for (Map.Entry<Byte, Integer> entry : SiegeMomentum.levels().entrySet()) {
            String time = new Time(entry.getValue() * 1000L).getFormattedTime(Time.TimeFormat.MINUTES_SECONDS, Time.Verbality.SHORT);
            lines.add(Component.literal("§e" + tr("warforge.ui.level_d", entry.getKey()) + "§r: §a" + time));
        }
        return lines;
    }
}
