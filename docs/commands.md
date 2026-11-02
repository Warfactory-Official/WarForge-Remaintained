# Command Reference

Root `faction`; aliases `f`, `factions`, `war`, `warforge`. Examples use `/f`.

- `/f`: usage hint. `/f help`: player list, plus operator list for ops.
- Operator = game-master permission. Operator subcommands are hidden from help and tab completion for others.
- Tab completion: faction names, online players, flag ids.
- Console: player-only commands reply "players only".

## GUI-only actions (no command)

| Action | Where |
|---|---|
| Claim / unclaim chunk, toggle force-load | Territory Map (`M`); claim consumes a claim block item. See [Factions](factions.md#claiming-territory) |
| Alliances, ally access | Alliances tab of Faction Members. See [Alliances](alliances.md) |
| Declare siege from map | Territory Map. See [Sieges](sieges.md#declaring-a-siege-from-the-map) |
| Upgrade, move citadel, colour, flag | Citadel menu |

## Player commands

### Membership

| Command | Role | Effect |
|---|---|---|
| `/f create` | none | Hint only; place a citadel |
| `/f invite <player>` | Officer+ | Invite online player; clickable chat prompt + toast |
| `/f accept [faction]` | none | Accept invite; name optional when exactly one invite |
| `/f promote <player>` | Leader | Member -> Officer |
| `/f demote <player>` | Leader | Officer -> Member |
| `/f setleader <name>` | Leader | Transfer leadership; name may be offline; old leader -> Officer |
| `/f expel <player>` (`remove`) | Officer+ | Remove a lower-ranked member |
| `/f leave` (`exit`) | none | Leave; Leader must transfer first unless last member; last member => disband; starts rejoin cooldown |
| `/f disband` | Leader | Disband own faction; refused while defending a siege |
| `/f msg <text>` (`chat`) | member | Faction chat |
| `/f ally chat <text>` (`ally msg`, `allychat`, `ac`) | member | Message own faction and all transitively allied factions |
| `/f vault redeem` | none | Claim unlocked insurance stash. See [Factions](factions.md#insurance) |

### Information

| Command | Effect |
|---|---|
| `/f info [faction]` | Own faction: full GUI. Other faction: name, leader, member count, citadel level, claim count. Ops additionally: notoriety, wealth, legacy. No argument without a faction => error |
| `/f list` | All factions with member counts |
| `/f time` | Time to next yield award and next siege-day advance |
| `/f top` | Total leaderboard (legacy + notoriety + wealth) |
| `/f wealth` (`wealthtop`, `bal`, `baltop`) | Wealth leaderboard |
| `/f notoriety` (`notorietytop`, `pvp`, `pvptop`) | Notoriety leaderboard |
| `/f legacy` (`legacytop`, `playtime`, `playtimetop`) | Legacy leaderboard |
| `/f borders` | Toggle own border rendering |

### Teleports

Stand-still countdown `Num Ticks for Warps`; moving or logging out cancels. Gated by `[Warps]` config.

| Command | Effect |
|---|---|
| `/f home` | To own citadel; cross-dimension only if `Allow /f home across dimensions` |
| `/f spawn` | To overworld spawn; non-overworld start only if `Allow /spawn across dimensions` |
| `/f tpa <player>` (`tprequest`, `tp`) | Request to teleport to player; expires after `TPA Request Timeout Seconds` |
| `/f tpaccept [player]` | Accept newest (or named) request; requester starts countdown to the acceptor |
| `/f tpdeny [player]` | Deny newest (or named) request |

## Operator commands

### Zones and protection

| Command | Effect |
|---|---|
| `/f zone safe` / `war` / `remove` (`zones`) | Claim current chunk as Safe Zone / War Zone, or remove the zone claim |
| `/f protection` (`opProtection`, `protectionOverride`) | Toggle own override; op bypasses protections |

### Factions

| Command | Effect |
|---|---|
| `/f disband <faction>` | Defeat any faction; unlocks insurance for its leader |
| `/f invite <player> <faction>` | Invite into a specific faction |
| `/f rename <old> <new>` | Rename |
| `/f flag <faction> set <flagId>` | Set flag, bypasses unique-flag rule (`custom:<name>`) |
| `/f flag <faction> reset` | Clear flag so the leader can choose again |
| `/f offlineprotection <faction> <enable\|disable\|status>` | Per-faction offline raid protection override |
| `/f clearnotoriety` / `/f clearlegacy` | Zero all factions' stat |
| `/f conquered clear` | Remove conquered state from current chunk |
| `/f conquered set <time>` | Set conquered timer on current chunk; `<time>` = `<n>[s\|m\|h\|d]`, bare number = seconds; credited to own faction |
| `/f reload` (`reloadConfig`, `reloadconfig`) | Reload main config, upgrade levels, veins, flags; resync players. See [Configuration](configuration.md#applying-changes) |
| `/f debugmsg <title> <subtitle> <colorHex>` | Test toast to self; `RRGGBB` or `AARRGGBB` |

### Sieges

| Command | Effect |
|---|---|
| `/f siege list` (`l`; `sieges`) | `chunkX, chunkZ, dim; Attacker-Defender` |
| `/f siege terminate <chunkX> <chunkZ> <dim> [WIN\|LOSE\|NEUTRAL]` (`t`) | Force-end; outcome from attacker view; default `NEUTRAL` (no rewards) |

### Veins

```
/f vein <info | set <vein> [quality] | clear | reroll> [at <chunkX> <chunkZ> [dim] [radius]]
```

| Part | Effect |
|---|---|
| `info` | Stored vein at chunk |
| `set <vein> [quality]` | `<vein>` = translation key or numeric id; quality `POOR`/`FAIR`/`RICH`, default `FAIR` |
| `clear` | Null vein |
| `reroll` (`seed`) | Drop override, reroll from overworld seed |
| `at ...` | Required from console; `dim` default sender dimension; `radius` clamped 0..16 (square) |

Refreshes claim views of all online players. See [Veins](veins.md).

## See also

[Factions](factions.md), [Sieges](sieges.md), [Alliances](alliances.md), [Veins](veins.md), [Configuration](configuration.md)
