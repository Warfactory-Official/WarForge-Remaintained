# Command Reference

WarForge exposes a single command, `faction`, with the aliases `f`, `factions`, `war`, and `warforge`. Every example below uses `/f`, but any alias works (so `/faction info` and `/war info` are the same command).

Most day-to-day group actions are also available through in-game GUIs (the citadel menu, the **Faction Members** screen, and the **Territory Map**). A few things — **alliances**, **claiming chunks**, and **declaring a siege from the map** — are GUI-only and have no chat command; those are called out below and documented in their own guides.

- `/f` or `/f help` lists the commands available to you. Operators see extra entries.
- Tab-completion is provided for subcommands, faction names, online players, and the `vein` / `offlineprotection` sub-options.

## Player commands

These are available to everyone. Some require a faction role (Officer or Leader); those are noted.

### Faction membership

| Command | Role | What it does |
|---|---|---|
| `/f create` | — | Informational only. Replies "Craft a Citadel to create a faction." You create a faction by placing a citadel and using its menu — see [Factions](factions.md). |
| `/f invite <player>` | Officer+ | Invite an online player to your faction. They receive a clickable chat prompt and a toast. |
| `/f accept [faction]` | — | Accept a pending faction invite. The name is optional if you have exactly one invite. |
| `/f promote <player>` | Leader | Promote one of your Members to Officer. |
| `/f demote <player>` | Leader | Demote one of your Officers to Member. |
| `/f setleader <player>` | Leader | Transfer leadership to another member. The old leader becomes an Officer. |
| `/f expel <player>` (alias `/f remove`) | Officer+ | Remove a player from your faction. You must outrank them. |
| `/f leave` (alias `/f exit`) | — | Leave your faction. If you are the last member, the faction disbands. |
| `/f disband` | Leader | Disband your faction (removes all claims and the citadel). |
| `/f msg <text>` (alias `/f chat`) | — | Send a message to your faction members only. |

> The same membership actions (invite, promote, demote, kick, leave, transfer) can be done from the **Faction Members** GUI, reached from the citadel menu's **Members** button or your inventory's faction button.

### Information and leaderboards

| Command | What it does |
|---|---|
| `/f info [faction]` | Show faction info. Players may only see their **own** faction's details; the name argument is mainly for the console. |
| `/f time` | Time remaining until the next yield award and the next siege-progress advance. |
| `/f top` | Overall leaderboard (legacy + notoriety + wealth). |
| `/f wealth` (aliases `wealthtop`, `bal`, `baltop`) | Wealth leaderboard. |
| `/f notoriety` (aliases `notorietytop`, `pvp`, `pvptop`) | Notoriety (PVP) leaderboard. |
| `/f legacy` (aliases `legacytop`, `playtime`, `playtimetop`) | Legacy (playtime) leaderboard. |

### Teleports and display

| Command | What it does |
|---|---|
| `/f home` | Teleport to your faction home. Can be disabled in config, may require standing still, and may not cross dimensions depending on server settings. |
| `/f spawn` | Teleport to world spawn (subject to the same kind of config gates as `/f home`). |
| `/f vault redeem` | Redeem an unlocked insurance stash into your inventory (only after your faction was defeated and you are/were its leader). See [Factions](factions.md#insurance). |
| `/f borders` | Toggle rendering of territory borders for yourself. |

> `/f tpa`, `/f tp`, `/f tpaccept`, `/f tprequest` are not real teleport requests — they reply with a hint to brew a Teleportation / Telereception potion.

## GUI-only actions (no command)

Some features are intentionally driven entirely through the UI:

- **Claiming / unclaiming chunks and toggling force-load** — the **Territory Map** (default hotkey, or the citadel/inventory buttons). See [Factions](factions.md#claiming-territory).
- **Alliances** — the **Alliances** tab of the Faction Members GUI: invite a faction, accept/decline requests, break an alliance, and toggle whether allies may use your land. See [Alliances](alliances.md).
- **Declaring a siege from the map** — pick a target chunk and a launch chunk on the Territory Map, consuming a Siege Camp block instead of physically placing one. See [Sieges](sieges.md#declaring-a-siege-from-the-map-no-camp).

## Operator commands

These `/f` subcommands require operator permission. They appear in `/f help` only for operators.

### Zones and claims

| Command | What it does |
|---|---|
| `/f safe` (aliases `safezone`, `claimsafe`) | Claim the chunk you are standing in as a **Safe Zone**. |
| `/f war` (aliases `warzone`, `claimwarzone`) | Claim the chunk you are standing in as a **War Zone**. |
| `/f protection` (aliases `opProtection`, `protectionOverride`) | Toggle the global op override that lets admins build inside protected areas. Announces the new state. |

### Faction administration

| Command | What it does |
|---|---|
| `/f disband <faction>` | Disband any faction (treated as a defeat — unlocks its insurance to its leader). |
| `/f invite <player> <faction>` | Invite a player into a specific faction (not just your own). |
| `/f rename <oldName> <newName>` | Rename a faction. |
| `/f offlineprotection <faction> <enable\|disable\|status>` | Enable, disable, or query a faction's offline raid protection. `status` also prints online count and whether it is protected right now. |
| `/f clearnotoriety` | Reset **all** factions' notoriety to 0. |
| `/f clearlegacy` | Reset **all** factions' legacy to 0. |
| `/f resetflagcooldowns` | Reset flag-move cooldowns. |
| `/f debugmsg <title> <subtitle> <colorHex>` | Send yourself a test notification toast. `colorHex` is an RGB or ARGB hex value (e.g. `55FF55`). |

### Sieges

`/f siege` (alias `/f sieges`) manages active sieges. Full details in [Sieges](sieges.md#admin-commands).

| Command | What it does |
|---|---|
| `/f siege list` (alias `l`) | List active sieges as `chunkX, chunkZ, dim; Attacker-Defender`. |
| `/f siege terminate <chunkX> <chunkZ> <dim> [WIN\|LOSE\|NEUTRAL]` (alias `t`) | Force-end the siege on a defending chunk. Outcome is from the **attacker's** perspective; `WIN`/`LOSE` apply normal rewards and grace periods, `NEUTRAL` (the default) just cancels it. |

### Veins

`/f vein` manages ore veins. Full details in [Veins](veins.md).

```
/f vein <info|set <vein> [quality]|clear|reroll> [at <chunkX> <chunkZ> [dim] [radius]]
```

- `info` — show the stored vein at the location.
- `set <vein> [quality]` — force a vein (quality is `POOR`, `FAIR`, or `RICH`; default `FAIR`).
- `clear` — remove the vein from the location.
- `reroll` (alias `seed`) — reroll the vein from the world seed.
- `at <chunkX> <chunkZ> [dim] [radius]` — operate on a specific chunk instead of the one you're standing in; `radius` (0–16) applies the action to a square area.

## See also

- [Factions, Citadels and Upgrades](factions.md)
- [The Siege System](sieges.md)
- [Alliances](alliances.md)
- [The Vein System](veins.md)
- [Configuration Guide](configuration.md)
