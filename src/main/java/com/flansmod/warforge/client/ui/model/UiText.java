package com.flansmod.warforge.client.ui.model;

import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.common.faction.ClaimType;
import com.flansmod.warforge.common.faction.Role;

import net.minecraft.locale.Language;
import com.flansmod.warforge.common.network.OperationsGuiData;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class UiText {
    public static final int STATUS_GREEN = 0x55FF55;
    public static final int STATUS_YELLOW = 0xFFFF55;
    public static final int STATUS_ORANGE = 0xFFAA00;
    public static final int STATUS_RED = 0xFF5555;

    private UiText() {
    }

    public static String formatClaimType(ClaimType claimType) {
        return I18n.get("warforge.client.claim_type." + claimType.serializedName);
    }

    public static String translateVeinName(String translationKey, Object... args) {
        String resolvedKey = translationKey;
        if (!Language.getInstance().has(resolvedKey) && Language.getInstance().has(resolvedKey + ".name")) {
            resolvedKey += ".name";
        }
        return I18n.get(resolvedKey, args);
    }

    public static String veinLabel(Vein vein, Quality quality) {
        if (quality == null) {
            return translateVeinName(vein.translationKey);
        }
        return translateVeinName(vein.translationKey, I18n.get(quality.getTranslationKey()) + " [" + quality.getMultString(vein) + "]");
    }

    public static String formatRole(Role role) {
        return I18n.get(switch (role) {
            case LEADER -> "warforge.client.role.leader";
            case OFFICER -> "warforge.client.role.officer";
            default -> "warforge.client.role.member";
        });
    }

    public static int roleColor(Role role) {
        return switch (role) {
            case LEADER -> 0xFFD54A;
            case OFFICER -> 0x55E3FF;
            default -> 0xFFFFFF;
        };
    }

    public static String flagDisplayName(String flagId) {
        String raw = flagId.contains(":") ? flagId.substring(flagId.indexOf(':') + 1) : flagId;
        return raw.replace('_', ' ');
    }

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
