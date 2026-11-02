# Multi-version build

WarForge builds four jars from one source tree with [Stonecutter](https://stonecutter.kikugie.dev/) 0.9.8:

| Node | Loader | Java | UI library |
|---|---|---|---|
| `1.20.1-forge` | Forge 47.x (ModDevGradle legacyforge) | 17 | LDLib 1 |
| `1.21.1-neoforge` | NeoForge 21.1.x | 21 | LDLib2 |
| `26.1.2-neoforge` | NeoForge 26.1.2.x | 25 | LDLib2 |
| `26.2-neoforge` | NeoForge 26.2.0.x | 25 | LDLib2 |

## Building

- All nodes: `./gradlew buildAll`
- One node: `./gradlew :26.1.2-neoforge:build` (also `test`, `runClient`, `runServer`, `runClient2`, `runData`)
- Node list as JSON (CI matrix): `./gradlew -q printNodes`
- Tests: JUnit 5 in `src/test/java`, run on every node (`:<node>:test`).
  Test classpath = main plus Minecraft (`addModdingDependenciesTo`). Tests tagged `needs-registries` run on 1.21.1 only (vanilla registry bootstrap needs a launcher elsewhere).
- Dev-only map mods (JourneyMap, Xaero) are on the client runs only; `runServer` never sees them.
- The Gradle JVM must be Java 21 or later. Gradle downloads the Java 17 and 25 toolchains itself (foojay).

The jars are in `versions/<node>/build/libs/` and are named `warforge-remaintained-<mc>-<loader>-<version>.jar`.

## CI and publishing

- `ci.yml` takes the node list from `printNodes`, builds and tests each node in its own job (`build` runs `check`, which depends on
  `checkNoVersionGuards`), uploads the jar, and starts the dedicated server once (smoke test).
- `platform-release.yml` runs `buildAll` and then the `curseforge` and `modrinth` tasks of every node. Each node
  uploads its own jar with its own game version, loader and Java version. The Modrinth version number is
  `<version>+<mc>-<loader>`, for example `2.4.3+26.1.2-neoforge`.
- Dependency relations come from `CURSEFORGE_RELATIONS` and `MODRINTH_RELATIONS`. Set
  `CURSEFORGE_RELATIONS_FORGE` / `_NEOFORGE` and `MODRINTH_RELATIONS_FORGE` / `_NEOFORGE` to give a loader its own
  list (LDLib 1 on Forge, LDLib2 on NeoForge).
- The mod version is `version` in the `[mod]` section of `stonecutter.properties.toml`.
- `mavenJava` snapshot publishing (`repo.warfactory.co/snapshots`) is configured for NeoForge nodes only.

## Source layout

- `src/main` holds the shared code and resources. It is written for `1.21.1-neoforge` (the `vcsVersion`), so the
  code needs no change when that node is active.
- `src/variant/<folder>/{java,resources}` holds code and resources that differ between versions. A class has the
  same package and name in each folder that has a copy. Shared code calls it directly, with no interface or lookup.
- `build-logic/src/main/kotlin/Variants.kt` is the one table that says which folders each node compiles:

| Node | Variant folders |
|---|---|
| `1.20.1-forge` | `forge-1.20.1`, `mc-1.20.1-1.21.1` (renamed), `ui-ldlib1` |
| `1.21.1-neoforge` | `neoforge`, `neoforge-1.21.1`, `mc-1.20.1-1.21.1`, `ui-ldlib2` |
| `26.1.2-neoforge` | `neoforge`, `neoforge-26`, `neoforge-26.1`, `ui-ldlib2` |
| `26.2-neoforge` | `neoforge`, `neoforge-26`, `neoforge-26.2`, `ui-ldlib2` |

- `mc-1.20.1-1.21.1` is written with NeoForge names; the Forge node compiles a copy with `FORGE_RENAMES` applied
  (`Variants.kt`, same list as Stonecutter's replacements).
- Data packs: `data/` in variant resource folders; formats differ per version:

| Folder | Recipes | Loot, tags |
|---|---|---|
| `forge-1.20.1` | `recipes/`, `{"item"}` ingredients, result `{"item"}` | `loot_tables/`, `tags/blocks/` |
| `neoforge-1.21.1` | `recipe/`, `{"item"}` ingredients, result `{"id"}` | `neoforge`: `loot_table/`, `tags/block/` |
| `neoforge-26` | `recipe/`, plain `"id"` / `"#tag"` ingredients, result `{"id"}` | `neoforge`: `loot_table/`, `tags/block/` |

Folder names say who uses them: a loader (`neoforge`), a loader and version range (`neoforge-26` is every
NeoForge 26.x node), vanilla code shared by both loaders (`mc-1.20.1-1.21.1`), or a UI library.

Most version differences are in small classes in `com.flansmod.warforge.compat` (for example `Ids`, `Nbt`,
`Stacks`, `Players`, `ClientCompat`) and `com.flansmod.warforge.platform` (for example `Platform`, `WFRegistry`,
`WFBlock`, `WFBlockEntity`, `NetworkPlatform`). Rendering goes through `client.render` (`Gui2D`, `BorderGpu`,
`ClientRenderEvents`, `ModelSprites`).

## Rules

1. **No Stonecutter comments in `src/main` or `src/variant`.** The root `checkNoVersionGuards` task (part of every
   node's `check`) fails the build if `//?` or `/*?` appears there. Put the difference in a variant class instead.
2. **Stonecutter string replacements are only for true one-to-one renames.** They are declared in
   `stonecutter.gradle.kts` (the Forge list is `FORGE_RENAMES` in `Variants.kt`): the Forge package names and
   `ModConfigSpec` for the Forge node, and `ResourceLocation` to `Identifier` for 1.21.11 and later.
3. **Variant folders are not processed by Stonecutter.** A folder that nodes on both sides of 1.21.11 use
   (`neoforge`, `ui-ldlib2`) must not name `ResourceLocation`: use `var` or the return type of `Ids`. Forge package
   names in a folder shared with Forge: only `mc-1.20.1-1.21.1` (renamed copy, see Source layout).
4. **Resources with `${...}` tokens** are listed in `ModCommonPlugin.kt` (`mods.toml`, `neoforge.mods.toml`,
   `pack.mcmeta`, the mixin configs, `pole.json`). The values come from `stonecutter.properties.toml`. Other files
   are copied as they are.
5. **The UI library is a hard dependency** (`ldlib` on Forge, `ldlib2` on NeoForge), `required` with a lower bound
   from `ldlib_dependency_range`.

## Block UIs

- Tree structure = `BlockUiState` tag only (captured on the server). Client lacks server factions and unsynced config.
- LDLib1: tag in `BlockUiFactory` open payload. LDLib2: `PacketBlockUiState` sent before the menu opens.
- UI text: `warforge.ui.*` keys via `UiLang.tr`, resolved at build time.
- Screen widgets: zero steady-state allocation in draw; text lines, textures and tooltips cached at build.

## Adding a version

1. Add the node in `settings.gradle.kts` (`match("<mc>", "<loader>")`) and its section in
   `stonecutter.properties.toml`.
2. Add its row in `Variants.kt`. Reuse folders where the APIs are the same.
3. Add `ldlib_dependency_range` and the `data/` formats of the new version. Compile. For each error, add a copy of
   the shim in the new folder, or split an existing folder when one class no longer fits all the nodes that use it.
4. Run `runServer` and `runClient`, and check the in-game list below.

## In-game checks

Borders (own, ally, conquered, fast mode, corners, the B toggle, the distance limit, many claims at once), the
placement overlay, the HUD (timers, vein overlay, siege bar, toasts), all 9 screens and the 4 block UIs, the block
entity renderers (pole, flag wave, beacon beam, leaderboard, name plates), chat, protections (including fluid flow
into claims), chunk loading after a restart, combat-log drops, the JourneyMap and Xaero's map overlays, and
save and load with two clients.

## Carry On

- Optional (`carryon`); compileOnly on every node, dev runtime only.
- Placement: Carry On's `EntityPlaceEvent` reports the replaced state; protections judge the carried block.
- Failsafe `placeCarried` (hit, death, disconnect, command): `CarryOnPlacementMixin` (gated by `CompatMixinPlugin`); protected target => block dropped as item, nothing placed.
- Forge 1.20.1 (LDLib 1 container screens): WF screens refuse to open while carrying; Carry On cancels container screen init. LDLib2 screens are not container screens.
- In-game: place in own claim, place in foreign claim, hit while carrying (`dropCarriedWhenHit`) inside a foreign claim with a chest, map key while carrying on 1.20.1.

## Worlds and items across Minecraft versions

There is **no data migration between Minecraft versions**. WarForge keeps the same save keys on every version, but
item stacks use the save format of each Minecraft version (item NBT on 1.20.1, data components from 1.20.5). A world
or `warforgefactions.dat` from one Minecraft version is not converted when you load it on another. Insurance items and
island collector contents can be lost or wrong if you move a world between Minecraft versions. Update worlds with the
normal Minecraft upgrade path, and keep a backup.
