package com.flansmod.warforge.client.ui.model;

import static com.flansmod.warforge.common.ui.UiLang.tr;

import com.flansmod.warforge.common.network.FactionMemberManagerGuiData;
import com.flansmod.warforge.common.network.PacketMemberData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class MemberManagerModel {
    private MemberManagerModel() {
    }

    public static boolean needsFaction(PacketMemberData data) {
        return !data.hasFaction && data.page != FactionMemberManagerGuiData.Page.INVITES;
    }

    public static String sectionTitle(PacketMemberData data) {
        return switch (data.page) {
            case MEMBERS -> tr("warforge.ui.roster");
            case ALLIANCES -> tr("warforge.ui.alliances");
            default -> data.hasFaction ? tr("warforge.ui.invite_console") : tr("warforge.ui.pending_invites");
        };
    }

    public static String sectionDescription(PacketMemberData data) {
        return switch (data.page) {
            case MEMBERS -> tr("warforge.ui.faces_rank_presence_and_direct_faction_actions");
            case ALLIANCES -> tr("warforge.ui.ally_with_factions_to_stop_sieges_between_you_to");
            default -> data.hasFaction
                    ? tr("warforge.ui.invite_online_unaffiliated_players_into_the_fact")
                    : tr("warforge.ui.accept_one_of_your_outstanding_faction_invites");
        };
    }

    public static @Nullable String emptyMessage(PacketMemberData data) {
        return switch (data.page) {
            case MEMBERS -> data.members.isEmpty() ? tr("warforge.ui.no_faction_members_found") : null;
            case ALLIANCES -> data.alliances.isEmpty() ? tr("warforge.ui.no_factions_available_to_ally_with_yet") : null;
            default -> {
                if (!data.hasFaction) {
                    yield data.inviteCandidates.isEmpty() ? tr("warforge.ui.you_have_no_open_faction_invites") : null;
                }
                if (!data.canInvitePlayers) {
                    yield tr("warforge.ui.officer_or_leader_rank_is_required_to_send_invit");
                }
                yield data.inviteCandidates.isEmpty() ? tr("warforge.ui.no_online_players_are_currently_eligible_for_inv") : null;
            }
        };
    }

    public static String allianceHeader(byte kind) {
        return switch (kind) {
            case FactionMemberManagerGuiData.AllianceEntry.KIND_ALLY -> tr("warforge.ui.current_allies");
            case FactionMemberManagerGuiData.AllianceEntry.KIND_PENDING -> tr("warforge.ui.incoming_requests");
            default -> tr("warforge.ui.invite_a_faction");
        };
    }

    public static String allyAccessLabel(boolean enabled) {
        return tr("warforge.ui.ally_access_s", enabled ? tr("warforge.ui.enabled") : tr("warforge.ui.disabled"));
    }
}
