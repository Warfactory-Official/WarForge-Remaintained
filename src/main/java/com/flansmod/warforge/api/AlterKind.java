package com.flansmod.warforge.api;

/** Why a block changes without a player break or place event; see docs/api.md for the claim flag each maps to. */
public enum AlterKind {
    EXPLOSION,
    FIRE,
    FLUID,
    RADIATION,
    TERRAFORM,
    PLACEMENT
}
