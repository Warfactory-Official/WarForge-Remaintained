# WarForge Branch Guide

This file documents the branch features that are already present in this tree and the siege changes completed for the current `AGENTS.md` task set.

It is meant to work as both:

- a quick tutorial for server operators and testers
- a branch changelog for the custom systems that differ from older WarForge behavior

## Quick Summary

This branch currently adds or changes the following major systems:

- hidden non-citadel claims
- ModularUI claim manager with claim / unclaim / force-load actions
- one-per-island faction yield collectors
- faction chunk loading for both manually force-loaded chunks and collector chunks
- a second dev client run task for multiplayer testing
- siege battle radius as an explicit synced setting
- conquered chunk borders using the restricted border texture
- siege-area protection that is resolved from the active battle zone instead of broad faction-wide defending state

## Player And Tester Tutorial

### 1. Claiming Land

Citadels still exist as physical multiblocks and remain the anchor of the faction.

Basic and reinforced claims now behave differently:

- placing a basic or reinforced claim block still consumes the block and claims the chunk
- after the claim is registered, the placed claim block is removed from the world
- the claim remains in faction storage as a data-only claim

This keeps normal claimed chunks visually cleaner while preserving citadels as physical structures.

### 2. Managing Claims Without Claim Blocks

There are two main ways to open the claim manager:

- press `M` in-game
- use the claims button added to the inventory GUI

The claim manager is an FTB-claims-style chunk panel around the player.

Controls:

- left click an available wilderness chunk to claim it
- left click one of your removable chunks to unclaim it
- right click one of your chunks to toggle force-loading

Important details:

- claiming from the UI consumes the first basic or reinforced claim block found in your inventory
- unclaiming a hidden claim refunds the correct claim block back to inventory, or drops it if inventory is full
- the panel header shows both claim capacity and manual force-load capacity
- the map can page when the configured radius is larger than the visible window

### 3. Yield Collectors

Hidden claims no longer have a local placed block inventory to receive pending yields. That is what island collectors are for.

Rules:

- an island collector must be placed inside your faction land
- only one collector is allowed per contiguous faction island
- if a new claim would merge two existing collector-bearing islands, the claim is rejected

Behavior:

- the collector acts as the inventory sink for yields from hidden claims on that island
- when yields advance, pending hidden-claim yields are pushed into the collector inventory if its chunk is loaded
- the collector chunk is automatically chunk-loaded by the faction chunk loading manager

Practical use:

- place one collector somewhere central on each disconnected landmass
- open it like a normal inventory block to pull gathered items out

### 4. Force-Loading Chunks

Force-loading is managed from the claim manager UI.

Rules:

- only officers or ops can toggle force-loading
- only chunks owned by your faction can be force-loaded

Capacity:

- there is a base force-load limit from config
- extra capacity scales with citadel level

Notes:

- the counter shown in the claim UI is for manual force-loaded chunks
- collector chunks are also loaded automatically, but through the collector system rather than the manual force-load toggle

### 5. Inventory Shortcuts

The inventory GUI has extra WarForge buttons on this branch.

They currently open or trigger:

- claims UI
- faction member manager
- faction stats
- citadel move targeting

The citadel move button uses the block you are currently looking at as the target surface.

### 6. Multiplayer Dev Workflow

This branch already includes a second local client run target.

Use:

```bash
./gradlew runClient
./gradlew runClient2
```

Behavior:

- `runClient` uses `run/client`
- `runClient2` uses `run/client2`
- the second client gets a separate default username suffix so local multiplayer testing is easier

## Siege Changes

### Battle Radius

Sieges now have an explicit battle radius instead of relying on the old implicit fixed kill-area radius.

Config:

- `Battle Square Chunk Radius From Siege`

What it does:

- defines the chunk radius around each active siege camp that counts as the active battle zone
- this radius is stored on the siege itself
- this radius is synced to clients with siege progress info
- this radius is used for siege progress events such as PVP kill validation

This is separate from:

- `Attacker Square Chunk Radius From Siege`
- `Defender Square Chunk Radius From Siege`

Those two settings still control abandon / presence checks. The new battle radius controls where the actual active battle zone is.

### Conquered Chunk Borders

Recently conquered chunks now render with their own border style.

Behavior:

- normal claimed chunks use the normal border texture
- conquered chunks use `assets/warforge/world/borders_restricted.png`
- conquered chunks use the colour of the faction recorded in conquered-chunk storage
- conquered-only chunks can render borders even when the chunk is no longer a normal active claim

This makes post-siege grace territory visible in-world instead of looking identical to normal claims.

### Siege-Area Protections

Protection resolution is now tied to the actual active siege battle zone.

Attacker behavior inside the active battle radius:

- attackers use the `Sieger` protection profile
- by default they cannot freely break or place ordinary blocks
- they can still interact and use items
- explosions are allowed there by default
- default break exceptions include `gregtech:machine`
- default placement exceptions include siege-style items such as `minecraft:tnt` and `minecraft:end_crystal`

Defender behavior:

- defending faction members use `ClaimDefended` only inside active battle-zone chunks
- this is more precise than the old faction-wide defending behavior

Why this matters:

- older behavior keyed too much off a faction-level defending flag
- the new behavior resolves protection from the real active siege area instead

## Branch Changelog

### Already Present On This Branch

- Non-citadel claim blocks become hidden data-only claims after successful placement.
- Claim management is available through a dedicated ModularUI chunk map instead of relying on physical claim blocks for ongoing management.
- Claim UI supports direct claim, unclaim, and force-load toggling.
- Faction island collectors exist and enforce one collector per contiguous island.
- Collector chunks are auto-loaded through the faction chunk loading manager.
- Manual faction chunk loading exists and is capped by configurable limits plus citadel-level scaling.
- The inventory GUI includes branch-specific WarForge shortcut buttons.
- `runClient2` exists for local multiplayer testing.

### Implemented For The Current AGENTS.md Task

- Added an explicit siege battle radius setting and stored it per active siege.
- Synced the siege battle radius to the client in siege progress packets.
- Switched conquered chunks to their own border texture path instead of rendering like normal claims.
- Synced border metadata separately from claim ownership so the claim UI stays truthful while border rendering can still show conquered state.
- Reworked siege-area protection lookup so attackers use siege-zone rules only inside the active battle zone.
- Tightened defender special protection so `ClaimDefended` applies to active battle-zone chunks instead of every claim owned by a faction that happens to be defending somewhere.

## Operator Notes

- If your server already has a custom config, review the `Sieger` protection category after updating because it is now actively used by the siege-zone resolver.
- If you want a harsher or looser battle zone, change `Battle Square Chunk Radius From Siege` without touching abandon radii.
- If you want different attacker exceptions, adjust the `Sieger` break and place whitelists in config.
