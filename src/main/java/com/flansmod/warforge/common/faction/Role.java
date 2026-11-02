package com.flansmod.warforge.common.faction;

/** Declaration order is rank order; comparisons use {@link #ordinal()}. */
public enum Role {
    MEMBER,
    OFFICER,
    LEADER;

    public boolean atLeast(Role required) {
        return ordinal() >= required.ordinal();
    }
}
