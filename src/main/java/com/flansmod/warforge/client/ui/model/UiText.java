package com.flansmod.warforge.client.ui.model;

import net.minecraft.locale.Language;
import com.flansmod.warforge.common.factories.OperationsGuiData;
import com.flansmod.warforge.server.Faction;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Text formatting and status colours shared by the screens of both UI libraries. */
@OnlyIn(Dist.CLIENT)
public final class UiText {
    public static final int STATUS_GREEN = 0x55FF55;
    public static final int STATUS_YELLOW = 0xFFFF55;
    public static final int STATUS_ORANGE = 0xFFAA00;
    public static final int STATUS_RED = 0xFF5555;

    private UiText() {
    }

    public static String formatClaimType(Faction.ClaimType claimType) {
        return switch (claimType) {
            case BASIC -> "Basic";
            case REINFORCED -> "Reinforced";
            case CITADEL -> "Citadel";
            case ADMIN -> "Admin";
            case SIEGE -> "Siege";
            default -> "None";
        };
    }

    /** Translates a vein name, falling back to the legacy "key.name" form. */
    public static String translateVeinName(String translationKey, Object... args) {
        String resolvedKey = translationKey;
        if (!Language.getInstance().has(resolvedKey) && Language.getInstance().has(resolvedKey + ".name")) {
            resolvedKey += ".name";
        }
        return I18n.get(resolvedKey, args);
    }

    public static String formatRole(Faction.Role role) {
        return switch (role) {
            case LEADER -> "Leader";
            case OFFICER -> "Officer";
            default -> "Member";
        };
    }

    /** The text colour of a faction role (leader gold, officer cyan, member white). */
    public static int roleColor(Faction.Role role) {
        return switch (role) {
            case LEADER -> 0xFFD54A;
            case OFFICER -> 0x55E3FF;
            default -> 0xFFFFFF;
        };
    }

    /** "namespace:some_flag" -> "some flag". */
    public static String flagDisplayName(String flagId) {
        String raw = flagId.contains(":") ? flagId.substring(flagId.indexOf(':') + 1) : flagId;
        return raw.replace('_', ' ');
    }

    /** The colour of a FOB row: red when occupied, otherwise by the fraction of tickets left. */
    public static int fobStatusColor(OperationsGuiData.FobEntry fob) {
        if (fob.occupied) {
            return STATUS_RED;
        }
        float fraction = fob.maxTickets > 0 ? (float) fob.tickets / fob.maxTickets : 0f;
        if (fraction >= 0.75f) {
            return STATUS_GREEN;
        }
        if (fraction >= 0.50f) {
            return STATUS_YELLOW;
        }
        if (fraction >= 0.25f) {
            return STATUS_ORANGE;
        }
        return STATUS_RED;
    }
}
