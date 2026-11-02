# FOB System

FOB = one-chunk warp point in neutral or enemy ground. Faction members warp to it while the faction is in an active siege (attacker or defender). No land ownership.

## Placing

- FOB block from the WarForge creative tab; Officer+ of a faction only.
- Placement refused:
  - claim within `FOB Claim Exclusion Radius` chunks,
  - chunk contested, conquered, or dimension not whitelisted,
  - FOB already in the chunk,
  - within `Minimum Distance Between Opposing Factions` of an opposing faction,
  - faction at its FOB limit (level `max_fobs` with upgrades, else `Max FOBs Per Faction`).
- Establish: Officer+ right-clicks, names the FOB, **Establish**. Structure stamped around the block: stone-brick cross floor, sandstone corner pillars, flag pole.
- Established FOB: only own faction opens the UI.
- Pick up: sneak + right-click; Officer+ of the owner (ops any). Refused while the faction is in a siege or the chunk is contested. Block returns as an item.
- Chunk unclaimable by others while the FOB exists.

## Tickets

| Event | Tickets |
|---|---|
| Warp | -1, plus `FOB Vehicle Ticket Cost` entry of the ridden entity type |
| Warp cancelled (moved, logout) | refunded |
| Any siege timer reset of the faction (as attacker or defender) | + regen, capped at limit |

- Limit and regen: level `fob_ticket_limit`, `fob_ticket_regen` with upgrades; else `FOB Ticket Limit`, `FOB Ticket Regen Per Siege Tick`. Cap refreshes on each regen.
- New FOB starts full.

## Warping

- Warp button in the FOB UI or the FOBs tab of the Faction Manager (visible to all members).
- Stand-still countdown `FOB Warp Ticks` (default 200 = 10 s); chat message each second.
- Vehicle and its passengers travel along; cross-dimension supported.
- Refused when:
  - a warp is already pending,
  - faction not in an active siege,
  - enemy standing above the FOB block in its chunk,
  - not enough tickets.
- Gap: `WarforgeAPI.addFobWarpVeto` is not yet consulted by the warp path.

## Enemy hold

- Hold timer counts up each tick while an enemy stands above the FOB block (in its chunk) and no member or ally of the owner does; counts down otherwise.
- No faction counts as enemy. Truce partners are neither enemy nor ally.
- Threshold = attacker's current momentum round time, minimum 20 ticks; no active siege => no hold.
- Threshold reached => FOB destroyed, chunk freed.
- Owner winning as defender resets all hold timers.

## Config

All in `[Claims]` of `warforge-common.toml`; level values override flat ones when upgrades are on.

| Key | Default |
|---|---|
| `Max FOBs Per Faction` | 3 |
| `FOB Ticket Limit` | 5 |
| `FOB Ticket Regen Per Siege Tick` | 1 |
| `FOB Warp Ticks` | 200 |
| `FOB Block Breakable` | false |
| `FOB Claim Exclusion Radius` | 1 |
| `FOB Vehicle Ticket Cost` | `[]` |

```toml
[Claims]
	"FOB Vehicle Ticket Cost" = ["superb_warfare:tank=2"]
```

Entity type id = registry key of the ridden entity.
