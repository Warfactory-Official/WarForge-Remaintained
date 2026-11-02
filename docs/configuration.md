# Configuration

| File | Format | Role |
|---|---|---|
| `config/warforge-common.toml` | TOML (mod config spec) | Server rules; server values override clients on join |
| `config/warforge-client.toml` | TOML | Per-player HUD and rendering; never synced |
| `config/warforge/upgrade_levels.toml` | TOML | Citadel levels; read only when `Enable Citadel Upgrade System` |
| `config/warforge/veins.toml` | TOML | Vein definitions |
| `resources/warforge/flags/` (server root) | PNG | Custom flags. See [Flags](flags.md) |

- Missing or empty `upgrade_levels.toml` / `veins.toml` => stub written.
- Legacy `.cfg` and `.yml` files not read; recreate as TOML.

## Applying changes

- `/f reload` re-reads the main config, upgrade levels, veins and flags, then resyncs online players.
- Reload failure keeps previous upgrade levels (`levels FAILED`) / previous veins (`veins FAILED`).
- Reload does NOT apply safely:

| Setting | Constraint |
|---|---|
| `Tick-Based *` toggles | Flip with server stopped; stored timestamps not converted, active timers corrupt |
| `Island Collector Slot Count` | Placed collectors keep size until their chunk reloads |
| `megachunk_length`, `iteration` (veins) | Change discards stored vein data |

## Main config format

- Section per category; keys with spaces are quoted:

```toml
[Claims]
	"Claim Manager Radius" = 4
	"Whole World Claim Map" = true
	"Claim Dimension Whitelist" = ["minecraft:overworld"]

[Sieges]
	"Battle Square Chunk Radius From Siege" = 2
	SiegeMomentumMultipliers = ["0=15:00", "1=10:00"]
```

- Sections: `Claims`, `Sieges`, `Alliances`, `General`, `Yields`, `Notoriety`, `Legacy`, `Display`, `Warps`, plus 13 protection sections.
- Values below are defaults.

### Claims

| Key | Default | Note |
|---|---|---|
| `Claim Dimension Whitelist` | `["minecraft:overworld"]` | |
| `Citadel / Reinforced / Basic Claim Strength` | 15 / 10 / 5 | Siege base defence |
| `Citadel / Reinforced / Basic Support Strength` | 3 / 2 / 1 | Subtracted from siege difficulty of adjacent own claims |
| `Force-loaded Chunks Total` | 8 | Ignored with upgrades (`loaded_chunks`) |
| `Max Claims Per Faction` | -1 | -1 unlimited; applies with level limit |
| `Max FOBs Per Faction` | 3 | Ignored with upgrades (`max_fobs`) |
| `FOB Ticket Limit` | 5 | Ignored with upgrades (`fob_ticket_limit`) |
| `FOB Ticket Regen Per Siege Tick` | 1 | Ignored with upgrades (`fob_ticket_regen`) |
| `FOB Warp Ticks` | 200 | |
| `FOB Block Breakable` | false | |
| `FOB Claim Exclusion Radius` | 1 | Chunks (Chebyshev) between a FOB and any claim or new claim; 0 = FOB chunk only |
| `FOB Vehicle Ticket Cost` | `[]` | `"namespace:entity_type=extraCost"` |
| `Minimum Distance Between Opposing Factions` | 1 | Chunks; allies exempt; 0 disables |
| `Claim Manager Radius` | 4 | 1..12 |
| `Whole World Claim Map` | true | true: JourneyMap/Xaero show owner colour of every claim; false: only nearby and previously seen |
| `Island Collector Slot Count` | 100 | Pull-only slots |
| `Enable Offline Raid Protection` / `Offline Raid Protection Hours` | true / 24 | |
| `Enable New Faction Siege Grace` / `New Faction Siege Grace Hours` | true / 24 | Attacking forfeits grace |
| `Faction Rejoin Cooldown Minutes` | 10 | After leaving; kick/disband exempt; 0 disables |
| `Citadel Move Cooldown Seconds` | 25200 | Across-chunk moves |
| `Citadel Minimum Y` | 63 | |
| `Enable Citadel Upgrade System` | true | |
| `Enabled Isolated Claims` | true | true: claims need no adjacency; false: new claim must touch an own claim |
| `Insurance Blacklist` | `["minecraft:*shulker_box", "appliedenergistics2:*cell*"]` | `*` wildcards on item ids |
| `Available Default Flags` | 16 dye names | Dye name or `rrggbb` hex |
| `Available Custom Flags` | `["*"]` | Globs on file name without extension |
| `Flag Whitelist` | `[]` | `"flagId=uuid1,uuid2"`; listed flag exclusive to those players |
| `Unique Flags` | true | Admin `/f flag set` bypasses |

### Sieges

| Key | Default | Note |
|---|---|---|
| `Siege Camp Attack Strength` | 1 | Added to target difficulty per adjacent attacker claim |
| `Siege Camp Max Count Per Faction` | 3 | Outgoing sieges and camps; 0 unlimited |
| `Incoming Siege Max Count Per Faction` | 3 | 0 unlimited |
| `Attacker Desertion Timer [s]` / `Defender Desertion Timer [s]` | 180 / 300 | |
| `Attacker / Defender Conquered Chunk Grace Period [ms]` | 3600000 / 7200000 | 0 = none |
| `Time to Combat Log Action [ms]` | 10000 | See [Combat log](#combat-log) |
| `Time to Live Quit Siege [ms]` | 900000 | |
| `Max Players Before Online Status` | 0 | Negative => `-value` is the absolute threshold, ignoring percent |
| `Max Player % Before Online Status` | 0.5 | |
| `Maximum Vertical Siege Radius [Inclusive]` | 40 | |
| `Battle Square Chunk Radius From Siege` | 2 | Outer War zone |
| `Sieged Square Chunk Radius From Siege` | 1 | Inner Sieged zone |
| `Kill Detection Mode` | `PRECISE` | `PRECISE` or `SIMPLE` |
| `Attacker Square Chunk Radius From Siege` / `Defender ...` | 1 / 15 | |
| `Siege Swing Per Defender Death` / `... Attacker Death` | 1 / 1 | |
| `Siege Stall Escalation Threshold` | 10 | |
| `Siege Defence Threshold` | 5 | |
| `Siege Ends On Goal Reached` | true | |
| `Siege Info Radius` | 200 | Blocks |
| `Cooldown between sieges after failure` | 30 | Minutes; skipped with momentum |
| `Siege Difficulty Reinforcement Per Defender Flag` | 1 | Per defender member, total capped at 5 |
| `Siege Captures` | false | Won chunk becomes attacker's basic claim |
| `Enable Per-Siege timer` | true | false => central siege day |
| `Siege momentum duration` | 60 | Minutes |
| `SiegeMomentumMultipliers` | `["0=15:00","1=10:00","2=8:30","3=5:00","4=2:30"]` | `level=mm:ss`; highest level = max momentum |
| `Allow UI Siege Declaration` | true | |
| `UI Siege Declaration Max Range` | 4 | Chunks |
| `UI Siege Requires Attacker Presence` | true | |

Central-siege-day mode only (`Enable Per-Siege timer = false`): `Siege Day Length` 24 (hours), `Siege Swing Per Day` 1, `Siege Swing Per Day Without Attacker Logins` 1, `Siege Swing Per Day Without Defender Logins` 1.

### Alliances

| Key | Default |
|---|---|
| `Alliance Truce Duration [min]` | 60 (0 disables) |
| `Max Allies Per Faction` | 2 (-1 unlimited) |

### General

| Key | Default |
|---|---|
| `Max Faction Name Length` | 32 (3..128) |
| `Faction Name Banlist` | `["admin","mod","staff"]` (case-insensitive substrings) |

- Names: ASCII letters and digits only.

### Yields

| Key | Default | Note |
|---|---|---|
| `Enable Chunk Veins` | true | false: see [Veins](veins.md#disabling) |
| `Yield Day Length` | 3600 | Seconds |
| `Global Poor / Fair / Rich Quality Multiplier` | 0.5 / 1.0 / 2.0 | Fallback when a vein has no `quals` |
| `Tick-Based Yield Timer`, `Tick-Based Siege Timers`, `Tick-Based Siege Momentum`, `Tick-Based Offline Raid Protection`, `Tick-Based Siege Grace`, `Tick-Based Citadel Move Cooldown`, `Tick-Based Truces`, `Tick-Based Faction Rejoin Cooldown` | false | true: timer advances on server ticks, not wall clock |

### Notoriety and Legacy

| Key | Default |
|---|---|
| `Notoriety gain per PVP kill` | 1 |
| `Max # kills per player` | 3 |
| `Notoriety gain per siege attack win` / `defend win` | 7 / 10 |
| `Legacy gain per day` | 3 |
| `Legacy uses yield timer` | true (false: siege timer) |

### Display

| Key | Default |
|---|---|
| `Faction Prefix In Chat` / `Faction Prefix In Tab List` | true / true |
| `JourneyMap Vein Display Mode` | `DISABLED`; `AUTO` streams veins around each player, `PLAYER` only chunks the player loaded |
| `JourneyMap Vein Auto Radius` | 24 (1..128) |

### Dynmap

- Layer shows only when Dynmap is installed on the server (dedicated or integrated) and `Enable Dynmap Layer`. Layer keys marked "start" are read at server start; opacity, interval and zone labels apply live.
- Runtime exists for Forge 1.20.1 only (Dynmap has no NeoForge build); see `docs/api.md` compat note.

| Key | Default |
|---|---|
| `Enable Dynmap Layer` | true (start) |
| `Dynmap Layer Label` | `Factions` (start) |
| `Dynmap Safe Zone Label` / `Dynmap War Zone Label` | `Safe Zone` / `War Zone` |
| `Dynmap Hide By Default` | false (start) |
| `Dynmap Layer Priority` | 0 (-1000..1000, start) |
| `Dynmap Fill Opacity` / `Dynmap Stroke Opacity` | 0.35 / 0.8 |
| `Dynmap Update Seconds` | 5 (1..3600) |

### Warps

| Key | Default |
|---|---|
| `Enable /f home Command` / `Allow /f home across dimensions` | true / false |
| `Enable /spawn Command` / `Allow /spawn across dimensions` | true / false (governs `/f spawn`) |
| `Respawn At Citadel` | false; disables bed and anchor spawns |
| `Allow Bed Spawn In Claims` | false; with `Respawn At Citadel`, beds only in own claims |
| `Num Ticks for Warps` | 400 |
| `Enable /f tpa Command` / `TPA Request Timeout Seconds` | true / 60 |

## Protection sections

One profile per viewer per chunk; first match wins.

| Section | Applies |
|---|---|
| `SafeZone`, `WarZone` | Chunk owned by that admin zone |
| (own siege camp chunk) | `ClaimFriend` for the owner, even inside a siege zone |
| `SiegedFriend` / `SiegedFoe` | Inner zone of a siege; friend = defending faction member |
| `WarFriend` / `WarFoe` | Outer zone, outside the inner zone |
| `ClaimDefended` | Member of a faction under siege, anywhere else (citadel included) |
| `CitadelFriend` / `CitadelFoe` | Citadel chunk; member / other |
| `ClaimFriend` | Member in own claim |
| `ClaimAlly` | Ally of the owner, only while owner enabled Ally Access; else `ClaimFoe` |
| `ClaimFoe` | Everyone else in a claim |
| `Unclaimed` | No claim, no siege zone |

- Siege zones override ownership; Sieged over War where they overlap.
- Conquered wilderness is unclaimed: `Unclaimed` applies.
- Fluid and piston checks: actor = owner of the source chunk (fluid source, piston), profile = target chunk.

### Keys

Each section holds `"<Section> - <Key>"`:

| Key | Type |
|---|---|
| `Break Blocks`, `Place Blocks`, `Interact`, `Use Items` | bool |
| `Block Removal` | bool; false blocks all removal incl. explosions and mobs, overrides `Break Blocks` |
| `Explosion Damage` | bool |
| `Take Dmg From Mob`, `Take Dmg From Player`, `Take Any Other Dmg`, `Deal Damage` | bool; `Take Dmg From Player` = PVP |
| `Allow Mob Spawns`, `Allow Mob Entry`, `Allow Mount Entity`, `Allow Dismount Entity` | bool |
| `Allow Foreign Fluid Inflow` | bool; fluid from another owner's chunk |
| `Allow Foreign Piston Push` | bool; piston of another owner pushing into or out of the chunk |
| `Break/Place/Interact Whitelist`, `Break/Place/Interact Blacklist` | block ids, `#namespace:tag` allowed |
| `Use Whitelist`, `Use Blacklist` | item ids |
| `MineTime Enabled`, `MineTime Default Mode`, `MineTime Default Value`, `MineTime Whitelist`, `MineTime Blacklist` | in nested `[<Section>.MineTime]` |

- Whitelist re-allows when the action is denied; blacklist re-denies when allowed.
- MineTime: slows a break the profile denies instead of cancelling.
  - Mode `MULTIPLIER` (natural time x value) or `FIXED` (value seconds); default `MULTIPLIER`, 5.0, disabled.
  - Entry: `pattern[=spec]`; pattern = exact id, `*` glob, or `#tag`; spec = `x10`/`10` (time multiplier) or `30s` (fixed).
  - Whitelist applies even when disabled; whitelist wins over blacklist.
- Base defaults (sections override): all true except `Explosion Damage` false; place whitelist `minecraft:torch`; break whitelist `minecraft:torch`, `warforge:siegecampblock`; interact whitelist `minecraft:ender_chest`, `warforge:citadelblock`, `warforge:basicclaimblock`, `warforge:reinforcedclaimblock`, `warforge:siegecampblock`; use whitelist `minecraft:snowball`.

### Section overrides

| Section | Differences from base |
|---|---|
| `Unclaimed` | `Explosion Damage` true |
| `SafeZone` | Break/Place/Interact/Use, all damage dealt and taken, `Block Removal`, mob spawns and entry, mount/dismount, foreign fluid, foreign piston: false. Place and break whitelists empty; interact whitelist `minecraft:ender_chest`, `minecraft:lever`, `#minecraft:buttons`, `warforge:leaderboard` |
| `WarZone` | Break/Place, `Block Removal`, foreign fluid, foreign piston false. Break and place whitelist `minecraft:cobweb`, `minecraft:tnt`, `minecraft:end_crystal`; interact whitelist as `SafeZone` |
| `CitadelFoe`, `ClaimFoe` | Break/Place/Interact/Use, mount/dismount, foreign fluid, foreign piston false |
| `ClaimAlly` | Break/Place false |
| `SiegedFriend`, `SiegedFoe` | `Explosion Damage` true; otherwise base |
| `WarFriend` | Base (explosions off) |
| `WarFoe` | Break/Place/Interact, foreign fluid, foreign piston false; `Use Items` true |
| `CitadelFriend`, `ClaimFriend`, `ClaimDefended` | Base |

## Client config (`warforge-client.toml`)

| Key | Default |
|---|---|
| `Show Opponent Chunk Borders` / `Show Ally Chunk Borders` | true / true |
| `New Area Timer` | 200 ticks |
| `Show yield timers` | true |
| `Vein Member Display Time` | 1000 ms (min 100) |
| `HUD Vertical cutoff` | 0.4 |
| `Yield timer position` | `BOTTOM_RIGHT` |
| `Siege status position`, `Toast indicator position`, `Chunk vein indicator position` | `TOP` |
| `Border Render Distance` | 0 (follow client render distance), max 256 |

## Combat log

- Member logs out while own faction is attacker or defender in any siege => entry created.
- Not back within `Time to Combat Log Action [ms]` => saved inventory emptied from the player data file and dropped at the logout position.
- Login within threshold cancels.

## Upgrade levels (`upgrade_levels.toml`)

Array of `[[levels]]`; stub defines levels 0..3.

| Key | Required | Default | Note |
|---|---|---|---|
| `level` | yes | | Index; gaps filled with undefined levels |
| `claim_limit` | yes | | `> 0` or `-1`; must not decrease, `-1` exempt |
| `insurance_slots` | no | 0 | `>= 0` |
| `loaded_chunks` | no | 0 | `>= 0`; force-load cap |
| `max_fobs`, `fob_ticket_limit`, `fob_ticket_regen` | no | 0 | |
| `requirements` | no | none | Cost to reach this level; consumed from the officer's inventory |
| `extra_claim_cost` | no | none | Consumed per claim block placed; count `<= 0` skipped; creative exempt |

- Entry: `{ type = "ore" | "item", id = "...", count = n }`; `count` default 1.
- `ore` id = item tag (namespace `forge` on 1.20.1, `c` on NeoForge); `item` id = `namespace:path` (a second `:` is rejected and skipped).
- Unresolved tag/item: logged and skipped. Unknown `type`, malformed entries, or non-monotonic `claim_limit` => file rejected.
- Level beyond the last defined level: claim limit -1 (unlimited), no requirements = max level.

```toml
[[levels]]
level = 1
claim_limit = 8
insurance_slots = 9
loaded_chunks = 6
fob_ticket_limit = 5
fob_ticket_regen = 1
max_fobs = 2
extra_claim_cost = []
requirements = [
    { type = "ore", id = "c:ingots/iron", count = 32 },
    { type = "item", id = "minecraft:netherite_ingot", count = 1 },
]
```

## Veins (`veins.toml`)

| Root key | Default | Note |
|---|---|---|
| `iteration` | 0 | Short; mismatch with stored value discards stored veins |
| `megachunk_length` | 32 | 4..180, else 32; side of quota regions |
| `gradient_radius` | 0 | Blocks, Chebyshev; `<= 0` disables; density 0 at the edge, no veins beyond |
| `gradient_frequency` | 1.0 | `>= 0`; 1 linear, `> 1` dense center, `< 1` thins quickly |
| `gradient_center_x`, `gradient_center_z` | 0 | Blocks |
| `[[veins]]` | | One table per vein |

| Vein key | Required | Note |
|---|---|---|
| `id` | no | 0..8191; omitted => auto-assigned and file rewritten, comments lost |
| `key` | yes | Translation key |
| `wealth` | no | Default 1; added to faction wealth per claimed chunk |
| `quals` | no | Inline table `{ POOR, FAIR, RICH }` of multipliers |
| `dims` | yes | `{ id, weight 0..1, mult }`; `mult` default 1 |
| `components` | yes | `{ item, yield, weights, mults }` |

- Component `item`: item id or item tag (tag preferred when both exist); a second `:` rejected.
- `weights`: `[{ id, weight 0..1 }]`, omitted dimension 1.0. `mults`: `[{ id, mult }]`, omitted dimension falls back to the dim `mult`.
- Sum of `weight` across veins per dimension must stay `<= 1.0`; excess vein ignored with an error.
- Details: [Veins](veins.md).

```toml
iteration = 0
megachunk_length = 32

[[veins]]
id = 0
key = "warforge.veins.iron"
wealth = 1
quals = { POOR = 0.4, RICH = 3.0 }
dims = [ { id = "minecraft:overworld", weight = 0.15 } ]
components = [
    { item = "minecraft:coal_ore", yield = 3.0 },
    { item = "minecraft:iron_ore", yield = 2.0, weights = [ { id = "minecraft:overworld", weight = 1.0 } ] },
]
```
