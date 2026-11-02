# Factions, Citadels and Upgrades

Faction = Citadel + claimed chunks + members with roles + stats (notoriety, wealth, legacy). Commands: [Command Reference](commands.md).

## Creating a faction

1. Craft and place a Citadel (`warforge:citadelblock`).
2. Right-click it as the placer (ops also may); **Create Faction**.
3. Enter name, pick colour (HSB sliders), confirm.

| Item | Recipe `GGG / GSG / OOO` |
|---|---|
| Citadel, overworld | G glass, S gold block, O obsidian |
| Citadel, nether | G glowstone, S ghast tear, O nether brick |

- Placement refused: chunk claimed, contested by a siege, conquered, dimension not whitelisted, near an active FOB, within `Minimum Distance Between Opposing Factions` of another faction, or already in a faction.
- Citadel = 3-block column; unbreakable and explosion-proof; removed by disband or siege loss.
- Name: ASCII letters and digits, `Max Faction Name Length`, unique, no `Faction Name Banlist` substring.
- Pure white colour => random saturated colour.
- Creator = Leader; citadel chunk = first claim; new faction gets siege grace (`Enable New Faction Siege Grace`).
- Blocked by `Faction Rejoin Cooldown Minutes` after leaving a faction.

## Claiming territory

1 claim = 1 chunk. Citadel claims its own chunk only.

| Item | Recipe `GGG / GSG / OOO` | Defence |
|---|---|---|
| Basic Claim `warforge:basicclaimblock` | G glass, S emerald, O gold block | 5 |
| Reinforced Claim `warforge:reinforcedclaimblock` | G obsidian, S nether star, O diamond block | 10 |
| Citadel | | 15 |

- Claim by placing the block (block is consumed; claim becomes data only) or from the Territory Map (consumes a claim item from inventory).
- Unclaim and force-load toggle: Territory Map.
- Rules: Officer+ (or op); chunk free (unclaimed, not contested, not conquered); dimension whitelisted; not near an active FOB; under claim limit; within `Minimum Distance Between Opposing Factions` of no opposing faction; `Enabled Isolated Claims` false => must touch an own claim.
- Claim limit: level `claim_limit` (when upgrades on) and `Max Claims Per Faction`; both apply.
- With upgrades on, `extra_claim_cost` of the faction's level is consumed per claim block placed.
- Island collector (`warforge:islandcollector`, recipe `IRI / RHR / IRI`: iron ingot, redstone, hopper): pull-only storage for yields of its connected island of claims. A claim merging two islands that each hold a collector is refused. Not siegeable.

## Citadel move

- Leader, from the citadel menu. Target must be inside an own claim, same dimension, at or above `Citadel Minimum Y`, solid floor, room for the column.
- Refused while defending a siege. Across-chunk moves limited by `Citadel Move Cooldown Seconds`.
- Old chunk stays claimed, loses its blocks.

## Upgrading the citadel

- Only when `Enable Citadel Upgrade System` (default on). Levels: `config/warforge/upgrade_levels.toml`, see [Configuration](configuration.md#upgrade-levels-upgrade_levelstoml).
- Level grants: claim limit, insurance slots, force-loaded chunks, FOB count, FOB tickets and regen.
- Flow: citadel menu **Upgrade**; Officer+; requirements for the next level consumed from the officer's inventory.
- Upgrades off: `Force-loaded Chunks Total`, `Max FOBs Per Faction`, `FOB Ticket *` apply; no per-level claim limit.

## Insurance

- Per-faction stash, size = level `insurance_slots`; opened from citadel menu.
- Faction defeated (citadel lost in a siege, or op `/f disband <faction>`): contents unlock for the Leader; `/f vault redeem` pulls them in, overflow drops.
- Joining or founding a faction voids an unredeemed stash.
- `Insurance Blacklist` items cannot be inserted.
- Voluntary Leader disband does not unlock.

## Roles

`Member < Officer < Leader`; one Leader.

| Action | Minimum |
|---|---|
| Claim, siege, FOB place/establish, upgrade, invite, alliances | Officer |
| Expel | Officer, target strictly lower |
| Promote, demote, transfer leadership, disband, colour, flag, move citadel | Leader |

- Leader cannot leave until leadership is transferred, unless last member (faction disbands).
- Disband and last-member leave refused while the faction is defending a siege.
- Disband removes claims, citadel, collector blocks, FOBs, force-loads, alliances; sieges involving it end.

## Offline raid protection

- All members offline => faction unsiegeable for `Offline Raid Protection Hours`; any login clears it.
- Op override per faction: `/f offlineprotection`.

## Keys

| Key | Action |
|---|---|
| `M` | Territory Map |
| `B` | Toggle borders |
| `O` | Toggle vein overlay |
| `Y` | Faction chat key |

## See also

[Sieges](sieges.md), [Alliances](alliances.md), [Flags](flags.md), [FOBs](fob-system.md), [Veins](veins.md), [Configuration](configuration.md)
