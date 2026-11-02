# Veins

Vein = metadata on a chunk. A faction claiming the chunk receives its ores each yield day.

- One vein per chunk at most; no vein => nothing produced.
- Not a physical structure; read via HUD overlay and Territory Map.

## Disabling

`Enable Chunk Veins = false` in `[Yields]`:

- No vein rolled, loaded or paid out; claims gain no pending yields, wealth is 0.
- Ore overlay (`O`), Territory Map vein tooltips and JourneyMap vein layer hidden; no vein packets sent or answered.
- `/f vein ...` answers disabled.
- API: `veins()` empty, `findVein` / `veinAt` null.
- Stored veins and `veins.toml` kept; re-enabling restores them. `/f reload` applies, wealth recomputed.

## Assignment

- Deterministic from world seed and chunk coordinates; stored once rolled.
- World split into megachunks, side `megachunk_length` chunks (default 32).
- Per megachunk, each vein is guaranteed `floor(megachunk_length^2 * weight)` chunks; weights are minimum quotas, remainder is empty.
- Distance gradient (optional): density falls from 1 at `gradient_center_*` to 0 at `gradient_radius` blocks (Chebyshev); beyond it no veins. `gradient_frequency` shapes the curve.
- Quality (`POOR`/`FAIR`/`RICH`) from the same hash; affects amount only.
- `megachunk_length` or `iteration` change => stored veins discarded.
- Addons: `WarforgeAPI.setVeinWeightProvider` can reweight, assign, or shift richness; consulted at first roll only. See [API](api.md).

## Quality multipliers

| Quality | Global default | Override |
|---|---|---|
| Poor | 0.5 | vein `quals` |
| Fair | 1.0 | vein `quals` |
| Rich | 2.0 | vein `quals` |

Global keys in `[Yields]`. Territory Map shows e.g. `Rich [2x]`.

## Yields

1. Yield day every `Yield Day Length` seconds (default 3600): each claim gains one pending yield (loaded dimensions only).
2. Pending yields are paid out by:
   - island collectors, per connected island of claims (loaded collectors only),
   - the citadel, for claims not covered by a collector,
   - any collector block on chunk load.
3. Backlog accumulates while unloaded and pays out on load.

Per pending yield, per component entry:

- Appearance roll against the component's per-dimension `weights` (default 100%).
- On success: whole part of `yield` x quality x dimension multiplier, guaranteed.
- Fractional part = chance of one extra item (`2.3` => 2 + 30% for a 3rd).
- Same item can appear as several components for outcome spread.
- Citadel collector multiplies its payouts by 2; collectors and claim blocks by 1.

| Sink | Capacity |
|---|---|
| Citadel / claim block | 9 slots |
| Island collector | `Island Collector Slot Count` (default 100); pull-only |

Full sink: excess dropped.

Wealth = sum of `wealth` of veins under the faction's claims; feeds the wealth leaderboard.

## Config

`config/warforge/veins.toml`; format in [Configuration](configuration.md#veins-veinstoml).

## Discovery (client)

- `O`: toggle chunk ore overlay (default on). Cycles component items (`Vein Member Display Time`) with appearance and bonus percentages; states: waiting for data, no vein, unrecognized.
- Territory Map tooltip: vein name, quality, multiplier, ore icons.
- Server answers vein queries only for chunks within 2 chunks of the player.
- JourneyMap: `JourneyMap Vein Display Mode` `AUTO` / `PLAYER` draws veins on the map.

## Admin

`/f vein <info | set <vein> [quality] | clear | reroll> [at <chunkX> <chunkZ> [dim] [radius]]`. See [Commands](commands.md#veins).

```
/f vein info
/f vein set warforge.veins.iron RICH
/f vein set 3 FAIR at 100 -40 minecraft:overworld 2
/f vein reroll at 100 -40 minecraft:overworld 3
```

- `/f reload` applies edited `veins.toml`; stored veins kept unless `iteration` / `megachunk_length` changed.
