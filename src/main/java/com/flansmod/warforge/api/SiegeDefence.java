package com.flansmod.warforge.api;

/**
 * Difficulty of sieging a chunk with no attacker present.
 * {@code total = base + member - adjacentSupport + reinforcement}.
 */
public record SiegeDefence(int base, int member, int adjacentSupport, int reinforcement, int total) {}
