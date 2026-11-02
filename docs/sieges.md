# Sieges

One siege = one defending chunk. Attacker places a Siege Camp (or declares from the map) next to it and wins a timed PVP contest.

## Siege Camp

`warforge:siegecampblock`, recipe (overworld):

```
 N     N banner
FBC    F furnace, B bed, C chest
OOO    O cobblestone
```

- Multiblock column; unbreakable and explosion-proof. Removed by sneak + right-click pickup or when the siege ends.
- Camp claims its own chunk for the attacker while it stands.
- Placement: Officer+; outgoing siege limit not reached (`Siege Camp Max Count Per Faction`, 0 = unlimited); chunk free (unclaimed, uncontested, not conquered, whitelisted dimension, not near an active FOB); at least one cardinally adjacent claim of another faction whose Y is within `Maximum Vertical Siege Radius` of the camp.

## Starting

Camp: right-click camp (Officer+), **Siege Target Map**, click an attackable chunk. Targets = the 4 cardinal neighbours of the camp chunk that hold a claim.

### Declaring from the map

- Territory Map **Declare Siege**: pick target claim, then launch chunk within `UI Siege Declaration Max Range` (Chebyshev) of the target.
- Consumes one Siege Camp item; no block placed. Siege anchors at the launch chunk, battle zones centre there.
- Needs `Allow UI Siege Declaration`; same dimension as the player.
- `UI Siege Requires Attacker Presence`: no attacker present for the attacker desertion timer => siege fails.

### Start checks

| Check | Refused when |
|---|---|
| Role | not Officer+ |
| Cooldown | within `Cooldown between sieges after failure` of last siege start, unless faction has momentum |
| Outgoing limit | `Siege Camp Max Count Per Faction` reached |
| Target | own claim; ally; truce partner; neutral zone |
| Offline protection | defender fully offline within `Offline Raid Protection Hours` |
| New-faction grace | defender within `New Faction Siege Grace Hours`; attacker starting a siege forfeits own grace |
| Incoming limit | `Incoming Siege Max Count Per Faction` reached |
| Claim | target claim cannot be sieged (island collectors, siege camps) |
| State | chunk already in a siege; conquered |

Start announced server-wide.

## Progress

- Signed `attackProgress`. Attacker wins at `>= threshold`; defender wins at `<= -Siege Defence Threshold` (default 5).
- `Siege Ends On Goal Reached` true: ends at once; false: settled at next timer elapse.

### Attacker threshold

`base + member bonus - adjacent support + reinforcement + attacker adjacent attack`

| Term | Value |
|---|---|
| base | defending claim strength: Basic 5, Reinforced 10, Citadel 15 |
| member bonus | `min(5, defender member count * Siege Difficulty Reinforcement Per Defender Flag)` |
| adjacent support | sum of support strength of defender's 4 cardinal neighbours (Basic 1, Reinforced 2, Citadel 3) |
| reinforcement | active reinforcers from addons ([API](api.md)) |
| attacker adjacent attack | `Siege Camp Attack Strength` per attacker claim cardinally adjacent to the target |

Minimum threshold 1. Recomputed each timer round.

### Sources of progress

| Source | Change |
|---|---|
| Attacker kills defender in kill zone | `+Siege Swing Per Defender Death` (1) |
| Defender kills attacker in kill zone | `-Siege Swing Per Attacker Death` (1) |
| Timer round elapses | `+Siege Swing Per Day` (1) toward attacker, times stall multiplier |

- Kill zone = union of War and Sieged zones around each camp.
- `Kill Detection Mode`: `PRECISE` needs an opposing player's kill; `SIMPLE` counts any participant death in the zone (fall, mob).
- Round length = attacker's momentum time. A kill resets stall escalation.
- Stall escalation: defender lead `> Siege Stall Escalation Threshold` => each kill-less round doubles the attacker's timer swing (1, 2, 4, ... max 1024).
- `Enable Per-Siege timer` false: central siege day (`Siege Day Length` hours) replaces per-siege rounds; day swing +1 base, +1 per no-defender-login, -1 per no-attacker-login.

### Momentum

| Level | Round |
|---|---|
| 0 | 15:00 |
| 1 | 10:00 |
| 2 | 8:30 |
| 3 | 5:00 |
| 4 | 2:30 |

- Attack win: +1 level (max = highest configured level), expiry refreshed (`Siege momentum duration`, 60 min). Loss: reset to 0. Expired => 0.
- Times and levels from `SiegeMomentumMultipliers`.
- Momentum waives the post-failure cooldown.

### Presence and desertion

Camp sieges (camp block entity enforces):

| Condition | Timer | Result |
|---|---|---|
| No attacker present | `Attacker Desertion Timer [s]` (180) | attackers lose |
| Attackers present, no defender within `Defender Square Chunk Radius From Siege` (15) of the camp | `Defender Desertion Timer [s]` (300) | attackers win |
| Attackers present, defenders were online and now all offline | `Time to Live Quit Siege [ms]` (900000) | attackers win |

- Attacker present = within `Attacker Square Chunk Radius From Siege` (1) of any camp, or inside the defender's connected claim island.
- Live quit applies when the largest online defender count seen exceeds `Max Players Before Online Status` (negative = absolute threshold) or `Max Player % Before Online Status` of members.
- Timers drain gradually when the condition clears.
- Camp-less sieges: only attacker presence (`UI Siege Requires Attacker Presence`) is enforced.
- Siege ends when either faction is disbanded or the target claim is gone.

## Zones and protections

| Zone | Radius from each camp | Profile (defender member / other) |
|---|---|---|
| Sieged (inner) | `Sieged Square Chunk Radius From Siege` (1) | `SiegedFriend` / `SiegedFoe`; protection effectively off |
| War (outer) | `Battle Square Chunk Radius From Siege` (2) | `WarFriend` / `WarFoe`; foes cannot break or place, kills count |

- Radii stored per siege at start.
- Own camp chunk always `ClaimFriend` for the attacker.
- Members of a defending faction elsewhere: `ClaimDefended`.
- Defaults and keys: [Configuration](configuration.md#protection-sections).

## Conclusion

| Outcome | Effects |
|---|---|
| Attacker wins | Defender loses the claim; target and camp chunks conquered for `Attacker Conquered Chunk Grace Period` (1 h); attacker +7 notoriety, +1 momentum; `Siege Captures` converts the chunk into an attacker basic claim |
| Defender wins | Target and camp chunks conquered for `Defender Conquered Chunk Grace Period` (2 h); defender +10 notoriety; attacker momentum reset |
| Terminated (op) | `WIN`/`LOSE` as above; `NEUTRAL` no rewards |

- Citadel chunk lost => defending faction defeated, insurance unlocks for its leader.
- Camps consumed at conclusion; defender FOB hold timers reset.
- Conquered chunk: nobody can claim or siege it until the timer ends. Unclaimed conquered chunk is unprotected (`Unclaimed` profile). Ops: `/f conquered`.

## Combat log

Logout during an own-faction siege and absence beyond `Time to Combat Log Action [ms]` (10 s) drops the player's inventory at the logout position. See [Configuration](configuration.md#combat-log).

## Admin

`/f siege list`, `/f siege terminate`, `/f conquered`; `/f time` shows next yield and siege-day timers. See [Commands](commands.md).

## See also

[Factions](factions.md), [Alliances](alliances.md), [FOBs](fob-system.md), [Configuration](configuration.md#sieges)
