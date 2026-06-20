# Factions, Citadels and Upgrades

This guide explains how to start a faction, claim territory, and upgrade your citadel. It is written around how the current code actually behaves.

A faction is the central group entity in WarForge. It owns a **Citadel** (its core block), a set of claimed chunks, members with roles, and stats such as notoriety, wealth and legacy. Everything you do as a group flows through the Citadel and the `/faction` command.

The command is `faction`, with aliases `f`, `factions`, `war` and `warforge`. All examples below use `/f`. See the [Command Reference](commands.md) for the full command list.

## Quick start

1. Craft a **Citadel** (`warforge:citadelblock`).
2. Place it on solid ground in an unclaimed, uncontested chunk.
3. Right-click it (you must be the player who placed it).
4. Click **Create Faction**, enter a name, pick a colour, and confirm.
5. You are now the faction **Leader**, and the citadel's chunk is your first claim.

## Creating a faction

### Step 1 — Craft a Citadel

The Citadel is a shaped recipe in a `GGG / GSG / OOO` pattern. There are two ingredient sets that both produce `warforge:citadelblock`:

- **Overworld recipe:** G = Glass, S = Gold Block, O = Obsidian.
- **Nether recipe:** G = Glowstone, S = Ghast Tear, O = Nether Brick.

> Note: `/f create` is informational only. It just replies "Craft a Citadel to create a faction." There is no command that creates a faction — you must place a citadel.

### Step 2 — Place the Citadel

Placement is rejected if the chunk is already claimed by another faction, is contested by an active siege, or the block has no solid surface beneath it.

The Citadel is a three-block-tall multiblock: the base block, a statue one block above, and a translucent beacon-laser block two blocks above. Leave headroom when placing it. The Citadel is **unbreakable** and immune to explosions — it cannot be mined or bombed out; it is only removed by disbanding or by losing it in a siege.

By default, claims are only allowed in the **Overworld** (dimension `0`). This is controlled by the `Claim Dimension Whitelist` config option.

The player who places the citadel is recorded as its placer. **Only that player can open the create-faction interface** on that citadel.

### Step 3 — Open the interface and create

Right-click the placed citadel. If you are the placer and not already in a faction, the citadel menu opens with a **Create Faction** button.

**Create Faction** opens the creation screen:

- **Enter Name** — a text field, up to 64 characters.
- **Colour** — three sliders for Hue, Saturation and Brightness. This colour tints your faction's territory borders and nameplates.
- **Create** / **Cancel** — Enter confirms, Escape cancels.

When you confirm:

- The name is validated and must be unique; you must not already be in a faction.
- If you leave the colour at pure white (`0xFFFFFF`), the server assigns a **random saturated colour** for you instead.
- The faction is created, the citadel's own chunk becomes your first claim, you are added as a member and set as **Leader**, and a server-wide message announces the new faction.
- The citadel plays a creation sound and particle burst.

## Claiming territory

A **claim is one chunk**. The Citadel claims only its own chunk when you create the faction. There is no automatic claim radius — you expand by placing more claim blocks, one per chunk.

### Claim blocks

- **Basic Claim** (`warforge:basicclaimblock`) — `GGG / GSG / OOO`: G = Glass, S = Emerald, O = Gold Block. Defence strength 5.
- **Reinforced Claim** (`warforge:reinforcedclaimblock`) — `GGG / GSG / OOO`: G = Obsidian, S = Nether Star, O = Diamond Block. Defence strength 10.

For comparison, the Citadel itself has defence strength 15. Higher defence strength makes a chunk harder to siege (see [Sieges](sieges.md)).

When you place a basic or reinforced claim, the physical block is consumed and the claim becomes **data only** — no visible block remains in the world, but the chunk is now yours and shows up in the claim manager and on the map.

### Rules for claiming

- You must be an **Officer** or higher (or an operator) to claim.
- The chunk must be unclaimed and not contested by a siege.
- The dimension must be on the claim whitelist (default: Overworld only).
- You must be under your claim limit (see below).
- If `Enabled Isolated Claims` is **off**, new claims must be adjacent to an existing claim of yours. By default isolated claims are **allowed**.

### Claim limit

Two caps apply together:

- **Per-level limit** — only when the upgrade system is enabled. Each citadel level defines a claim limit (a value of `-1` means unlimited at that level). See *Upgrading the citadel* below.
- **Global cap** — `Max Claims Per Faction`, default `-1` (unlimited).

If the upgrade system is **disabled** (the default), only the global cap applies.

The claim manager UI displays a 4-chunk radius around you by default (`Claim Manager Radius`).

## Upgrading the Citadel

> The upgrade system is **off by default**. It is enabled with the `Enable Citadel Upgrade System` config option. When disabled, there is no upgrade button and no per-level claim limit.

### What levels grant

Each citadel level can grant:

- A higher **claim limit**.
- More **insurance slots** (see below).
- More **force-loaded chunks**. The cap is `base + level × per-level`, defaulting to `4 + level` chunks. So a level-2 citadel keeps up to 6 chunks loaded while the faction is offline.

### The upgrade config

Levels and their costs are defined in `config/warforge/upgrade_levels.yml`. (A legacy `upgrade_levels.cfg` is migrated automatically, and a stub file is written if the file is missing or empty.)

Each level entry has:

- `level` — the level number.
- `claim_limit` — must be greater than `0`, or `-1` for unlimited. Claim limits must not decrease as levels rise.
- `insurance_slots` — optional, default `0`.
- `requirements` — a list of materials needed to advance **into** this level. Each requirement has:
  - `type` — `ore` (an OreDictionary name) or `item` (a `modid:item` or `modid:item:meta` id).
  - `id` — the ore name or item id.
  - `count` — optional, default `1`.

The shipped stub is an example, not a guarantee — admins are expected to edit it:

- **Level 0:** 5 claims, 0 insurance, no cost.
- **Level 1:** 10 claims, 9 insurance, costs 64 iron ingots (`ingotIron`) + 1 diamond.
- **Level 2:** 15 claims, 18 insurance, costs a placeholder item.

> Admin note: the stub's level 2 requirement is a non-existent placeholder item (`modid:custom_item:3`). If you enable upgrades, **edit `upgrade_levels.yml`** to use real items, or level 2 will be unreachable. Restart the server after editing.

### The upgrade flow

1. Right-click your citadel and choose **Upgrade**.
2. The **Citadel Upgrade** screen shows the requirements to advance from your current level to the next, each with a `have / required` counter that turns green when met, and an **Upgrade Outcome** card comparing current vs. next-level claims and insurance.
3. The **Upgrade** button is only enabled when you have all the materials **and** you are an Officer or higher. Hovering it lists anything you still need.
4. Confirm. The required items are consumed **from your personal inventory**, your citadel level increases by one, and an upgrade sound and particle effect play.

If no requirements are defined for the next level, the screen shows that your citadel is already at its maximum level.

## Insurance

Insurance is a per-faction stash whose size is tied to citadel level (with the default stub: 0 slots at level 0, 9 at level 1, 18 at level 2). It is opened from the citadel menu.

If your faction is **defeated** (its citadel is lost), the insurance contents are unlocked to the **Leader**, who redeems them with:

```
/f vault redeem
```

This pulls the items into the leader's inventory (overflow drops on the ground). Joining a new faction before redeeming **voids** an unredeemed stash. Some items (shulker boxes and AE2 cells by default) are blacklisted from being insured.

## Membership and roles

Roles, from lowest to highest: **Guest → Member → Officer → Leader**. There is exactly one Leader. New members join as Member.

- `/f invite <player>` — Officer+ invites a player. The invitee gets a clickable chat prompt.
- `/f accept [faction]` — accept an invite. The name is optional if you have only one pending invite.
- `/f promote <player>` — Leader promotes Member → Officer.
- `/f demote <player>` — Leader demotes Officer → Member.
- `/f setleader <player>` — Leader transfers leadership (the old leader becomes an Officer).
- `/f expel <player>` (alias `/f remove`) — kick a player; you must outrank them.
- `/f leave` (alias `/f exit`) — leave your faction. If the last member leaves, the faction disbands automatically.
- `/f msg <text>` (alias `/f chat`) — send a message to faction members only.

## Disbanding

Only the **Leader** (or an operator) can disband a faction. Use the **Disband** button in the citadel menu, or:

```
/f disband
```

Disbanding removes all of your claims, turns the citadel and every claim/collector block back into air, clears nameplates, releases force-loaded chunks, and announces the disband. Losing the citadel chunk in a siege is the other way a faction ends — it triggers the same defeat/cleanup, and unlocks insurance to the leader.

## Alliances

Factions can ally with one another. Allies cannot siege each other and can optionally use each other's land, and breaking an alliance starts a cooling-off **truce** that briefly blocks sieges and PVP between the former allies. Alliances are managed from the **Alliances** tab of the Faction Members GUI — there is no chat command for them. See [Alliances](alliances.md) for the full flow.

## Useful player commands

The most common commands are below; the [Command Reference](commands.md) documents every subcommand.

| Command | Purpose |
|---|---|
| `/f help` | List commands. |
| `/f info [faction]` | Show faction info. |
| `/f time` | Time until the next yield award and next siege advance. |
| `/f home` | Teleport to the faction home. |
| `/f spawn` | Teleport to spawn. |
| `/f top` | Overall leaderboard. |
| `/f wealthtop` / `/f notorietytop` / `/f legacytop` | Stat leaderboards. |
| `/f borders` | Toggle territory border rendering. |
| `/f vault redeem` | Redeem an unlocked insurance stash. |

## Operator commands

These `/f` subcommands require operator permission:

| Command | Purpose |
|---|---|
| `/f rename <oldName> <newName>` | Rename a faction. |
| `/f disband <faction>` | Disband any faction (unlocks its insurance). |
| `/f invite <player> <faction>` | Invite a player into a specific faction. |
| `/f offlineprotection <faction> <enable\|disable\|status>` | Manage a faction's offline raid protection. |
| `/f safe` | Claim the current chunk as a Safe Zone. |
| `/f war` | Claim the current chunk as a War Zone. |
| `/f protection` | Toggle the op build-in-protected-areas override. |
| `/f siege <list\|terminate ...>` | Manage sieges — see [Sieges](sieges.md). |
| `/f vein <...>` | Manage ore veins — see [Veins](veins.md). |
| `/f clearnotoriety` / `/f clearlegacy` | Reset all notoriety / legacy. |

## Related configuration

The most relevant options (full details in the [Configuration Guide](configuration.md)):

- `Enable Citadel Upgrade System` — turns the upgrade/claim-limit system on. **Default: off.**
- `Claim Dimension Whitelist` — claimable dimensions. **Default: Overworld.**
- `Max Claims Per Faction` — global claim cap. **Default: unlimited.**
- `Enabled Isolated Claims` — whether claims must touch an existing claim. **Default: allowed.**
- `Force-loaded Chunks Base Limit` / `Force-loaded Chunks Per Citadel Level` — offline chunk-loading capacity (**4** + **1** per level).
- Upgrade levels: `config/warforge/upgrade_levels.yml`.

Restart the server after changing any of these.
