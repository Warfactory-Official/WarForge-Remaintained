# The Vein System

Veins are the resource layer of WarForge. Every chunk can hold a hidden ore **vein**, and a faction that **claims** that chunk receives a passive trickle of those ores over time. This is what makes some territory worth fighting for: location matters because the veins underneath it do.

This guide covers what veins are, how they are assigned, how yields work, the config format, and the admin commands.

## What a vein is

A vein is **metadata attached to a chunk**, not a physical structure you dig up. It defines:

- a **name** (a translation key),
- a set of ore **components** (the items it can produce),
- how much of each it yields,
- which **dimensions** it appears in and how common it is there,
- and per-quality / per-dimension yield scaling.

Each chunk has at most one vein. A chunk with no vein uses the internal "null vein", which produces nothing. You don't see a vein by looking at the terrain — you see it through the HUD overlay and the claim manager (see *Discovering veins* below).

## How veins are assigned to chunks

Vein placement is **deterministic from the world seed and the chunk coordinates**, then shaped by per-region quotas. The same seed always produces the same vein map.

- The world is divided into square regions called **megachunks**, whose side length is `megachunk_length` (config, range 4–180, default **32** chunks).
- Within each megachunk, each vein is **guaranteed** a minimum number of chunks based on its weight: `floor(megachunk_length² × weight)`. Once that quota is filled in a region, the vein stops competing there.
- Weights are therefore **soft minimums per region, not strict spawn chances**. The leftover weight in a region belongs to the null vein (empty chunks).
- Each chunk is also assigned a **quality** (Poor / Fair / Rich), derived from the same seed-based hash, roughly evenly distributed.

Because everything derives from the seed, vein data is stable across restarts and is cached/saved to disk.

> Admin caution: changing `megachunk_length` **discards all stored vein data** (the saved positions can no longer be unpacked). Changing the global `iteration` value forces previously stored chunk veins to be re-rolled. Treat both as destructive, restart-required changes.

## Quality tiers

There are three qualities: **Poor**, **Fair**, **Rich**. Quality affects **only the amount** a vein yields — it does not change which ores appear.

Each quality multiplies the yield. The global defaults are:

- **Poor** — ×0.5
- **Fair** — ×1.0
- **Rich** — ×2.0

A vein can override these per-quality in its config (the `quals` field). Where it doesn't, the global multiplier applies. In the claim manager a chunk shows its quality and multiplier, e.g. `Rich [2x]`.

## How a faction gets yields

1. A faction **claims** chunks (with the Citadel and claim blocks — see [Factions](factions.md)).
2. On every **yield day** — a real-time interval set by `Yield Day Length`, default **1 hour** — the server awards all passive yields and announces "All passive yields have been awarded."
3. For each claimed chunk, that chunk's vein rolls its components and deposits the resulting ores into the claim block's inventory (9 slots). The Citadel and every claim block act as collectors.
4. **Pending yields accumulate while a chunk is unloaded.** When the chunk loads again, the backlog is processed — offline yields are not lost, they batch up.

### How a single yield is rolled

For each component of the vein, on each yield:

- The component has a per-dimension **appearance chance** (default 100%). Roll it; if it fails, that component produces nothing this time.
- If it passes, you get the component's **guaranteed amount** (its yield, scaled by the chunk's quality multiplier and any dimension multiplier).
- The **fractional part** of the scaled yield becomes a **bonus chance** for one extra item. For example, a yield of `2.3` gives 2 guaranteed plus a 30% chance of a 3rd.

The same item can be listed as several components to create a spread of outcomes (e.g. a common small drop plus a rare large drop of the same ore).

> Note on the Citadel: although the Citadel's collector declares a 2× yield multiplier, that multiplier is **not currently applied** in the yield math — a citadel chunk yields the same as a basic claim chunk of the same vein and quality. Don't rely on citadels producing more.

## The vein config (`veins.toml`)

Veins are defined in `config/warforge/veins.toml`. A legacy `veins.cfg` is migrated automatically, and a commented example stub is written if the file is empty. Veins are loaded at startup and synced to clients — **restart the server after editing.**

### Global keys

- `iteration` — a version stamp. Bumping it forces stored chunk veins to be re-rolled.
- `megachunk_length` — region side length (4–180, default 32). Out-of-range values fall back to 32.
- `veins` — the list of vein definitions.

### Per-vein fields

- `id` — a unique numeric id (0–8191). **Omit this key** to have the system auto-assign one. **Auto-assignment rewrites the file and strips comments — back the file up first** (the in-file example warns about this too).
- `key` — the translation/identifier key, e.g. `warforge.veins.iron_mix`.
- `quals` — optional inline table of per-quality multiplier overrides, e.g. `quals = { RICH = 10.0, POOR = 0.1 }`. Omitted qualities use the global multiplier.
- `dims` — an array of inline tables for the dimensions this vein appears in. Each entry has:
  - `id` — dimension ResourceLocation (`minecraft:the_nether`, `minecraft:overworld`, `minecraft:the_end`, or a modded dimension id).
  - `weight` — generation weight in that dimension, `0.0`–`1.0`.
  - `mult` — optional yield multiplier for that dimension (default `1.0`).
- `components` — the ores this vein can produce, an array of inline tables. Each entry has:
  - `item` — the item id, e.g. `minecraft:iron_ore`.
  - `yield` — base amount. The whole part is guaranteed; the decimal is a bonus chance for one extra.
  - `weights` — optional array of `{ id = <dimension>, weight = <0..1> }` tables (per-dimension appearance chance); omitted dims default to `1.0`.
  - `mults` — optional array of `{ id = <dimension>, mult = <float> }` tables (per-dimension yield multiplier); omitted dims fall back to the vein's `dims.mult`.

> Validation: if the total weight of all veins in one dimension exceeds `1.0`, the offending vein is logged as an error and ignored. Keep each dimension's weights summing to at most 1.0 (the remainder is empty chunks).

## Discovering veins (client side)

Players don't dig up veins — they read them off the UI:

- **HUD overlay** — toggled with the **O** key (on by default). Standing over a chunk shows its vein: a cycling display of the component items (each shown ~1 second), with appearance and bonus-yield percentages. It distinguishes "waiting for data", "no ore", and "unrecognized vein".
- **Claim manager** — each chunk's tooltip shows the vein name with its quality and multiplier (e.g. `Rich [2x]`) and an icon of the component ores, or "No ores in this chunk".

The server only sends a chunk's vein to a player who is within one chunk of it, so the overlay populates as you move around.

## Admin commands

All vein commands are **operator only**, under `/f vein` (aliases `/factions`, `/war`, `/warforge`).

```
/f vein <info | set <vein> [quality] | clear | reroll> [at <chunkX> <chunkZ> [dim] [radius]]
```

**Targeting:**

- With `at <chunkX> <chunkZ> [dim] [radius]` you specify chunk coordinates. `dim` defaults to your current dimension; `radius` (default 0, clamped to 0–16) applies the action to a square area around the base chunk.
- Without `at`, a player targets their **current chunk**. From the console you **must** use `at`.

**Subcommands:**

- `info` — report the **stored** vein at the chunk (does not roll). Shows the vein and quality, or "no stored vein (rolls from the seed on first access)", or "cleared (no vein)".
- `set <veinKeyOrId> [quality]` — force chunks to a specific vein. The vein is matched by numeric id or translation key. Quality is `POOR`, `FAIR` or `RICH` (default `FAIR`).
- `clear` — set chunks to the null vein (no ore).
- `reroll` (alias `seed`) — remove any override and re-roll the chunk from the world seed.

After `set`, `clear` and `reroll`, the claim views of all online players are refreshed so the change shows up immediately.

Examples:

```
/f vein info
/f vein set warforge.veins.iron_mix RICH
/f vein set 12 FAIR at 100 -40 0 2
/f vein clear at 100 -40
/f vein reroll at 100 -40 0 3
```

## Related configuration

In `config/warforge.cfg`, category **Yields**:

- `Yield Day Length` — the real-time interval between yield awards. **Default: 1 hour.**
- `Global Poor/Fair/Rich Quality Multiplier` — the default quality multipliers (0.5 / 1.0 / 2.0).

Client display (category **Client**): the vein overlay position and the `Vein Member Display Time` (how long each ore is shown, default 1000 ms).

Vein definitions: `config/warforge/veins.toml`. See the [Configuration Guide](configuration.md) for the full picture.
