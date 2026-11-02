# API

## Server: `com.flansmod.warforge.api.WarforgeAPI`
- Server thread only; any other thread => `IllegalStateException`. Exceptions: `setVeinWeightProvider`, `addFobWarpVeto`, `MOD_ID`.
- Positions: `ResourceKey<Level>` + `ChunkPos`/`BlockPos`. Nothing loads a chunk.
- Ownership != permission: use `mayPlace`/`mayBreak`.

| method | semantics |
|---|---|
| `factionOf(UUID player)` | faction id or null |
| `ownerOf(dim, chunk)` | player faction owning the chunk; null if unclaimed or zone |
| `zoneAt(dim, chunk)` | `ZoneKind` WILDERNESS, FACTION, SAFE_ZONE, WAR_ZONE |
| `siegeZoneAt(dim, chunk)` | `SiegeZoneKind` NONE, SIEGED, WAR |
| `ownersAlong(dim, Collection<ChunkPos>)` | claimed chunks only |
| `isMember(faction, player)`, `memberCount(faction)` | |
| `claimedChunks(faction)` | snapshot `Map<dim, LongSet(ChunkPos.asLong)>` |
| `loadedClaimedChunks(faction)` | same, loaded chunks only |
| `anyLoadedClaimedTile(faction, Predicate<BlockEntity>)` | short-circuits |
| `factionName`, `factionColour(id, fallback)` | |
| `areAllied`, `areAlliedOrTruced`, `maySeeFactionContent(viewerPlayer, ownerFaction)` | truce != ally |
| `mayPlace`/`mayBreak(player, dim, chunk, block)` | same rule as the protection event |
| `isExplosionProtected(level, pos, @Nullable igniterPlayer)` | |
| `filterExplosionProtected(level, @Nullable igniterFaction, Collection<BlockPos>)` | igniter is a faction, no player impersonated |
| `mayAlter(dim, chunk, @Nullable player, @Nullable faction, AlterKind)` | non-player block change; uniform per chunk. Actor faction = player's own, else `faction`; neither => outsider |
| `mayHarm(level, victim, @Nullable player, @Nullable faction, HarmKind)` | non-player victims: true; player victim: victim's chunk profile, `PLAYER_TAKE_DAMAGE_FROM_PLAYER` if `player` valid else `PLAYER_TAKE_DAMAGE_FROM_OTHER` |
| `notifyFaction(faction, @Nullable dedupeToken, Component, NotificationStyle, durationMs)` | toast to online members |
| `siegeDefenceAt(dim, chunk)` | `SiegeDefence(base, member, adjacentSupport, reinforcement, total)` from `SiegeDifficulty`; null if not player-owned |
| `getReinforcementBonus(faction, dim, chunk)` | `IChunkReinforcer` + legacy `IClaimStrengthModifier` |
| `veins()`, `findVein(key)`, `veinAt(ServerLevel, cx, cz)` | `VeinView`, `VeinSample`; `Enable Chunk Veins` false => empty / null |
| `setVeinWeightProvider(VeinWeightProvider)` | survives `/f reload`; install before players join (JourneyMap `AUTO` rolls veins around them) |
| `addFobWarpVeto(FobWarpVeto)` | reason `Component` or null. Gap: `fobWarpVeto` not called by `FobManager.requestFobWarp`, so registered vetoes have no effect |
| `pack(cx, cz)`, `unpackX`, `unpackZ` | `ChunkPos.asLong` layout, for `claimedChunks` sets |

## Golem Core protection provider
- `compat/golem/GolemProtection` registers with `dev.golem.core.world.Protection` in `FMLCommonSetupEvent` when mod `golemcore` is loaded. Compile: `compileOnly` `deps.golem` from mavenLocal (26.1.2 and 26.2 use the 26.1 artifact).
- Every answer = `WarforgeAPI.mayAlter`/`mayHarm`; client levels always allowed.
- `Source.player`/`Source.faction` => actor; unknown source => outsider; own faction may alter own claims (profile `CLAIM_FRIEND` etc.).

| Golem `Cause` | `AlterKind` | allowed iff (profile of the chunk for the actor) |
|---|---|---|
| EXPLOSION | EXPLOSION | `EXPLOSION_DAMAGE && BLOCK_REMOVAL` |
| FIRE | FIRE | `BLOCK_REMOVAL` |
| FLUID | FLUID | `BLOCK_REMOVAL` |
| RADIATION_BLOCK_CONVERSION | RADIATION | `BLOCK_REMOVAL` |
| TERRAFORM | TERRAFORM | `BREAK_BLOCKS && BLOCK_REMOVAL` |
| PLACEMENT | PLACEMENT | `PLACE_BLOCKS` |

| Golem `Hazard` | `HarmKind` | allowed iff (victim player, profile of the victim's chunk) |
|---|---|---|
| RADIATION, CONTAMINATION, GAS, FIRE, BLAST | same names | source player: `PLAYER_TAKE_DAMAGE_FROM_PLAYER`; else `PLAYER_TAKE_DAMAGE_FROM_OTHER` |

- Vanilla `ExplosionEvent` path unchanged (no igniter => outsider).

## Extension points
- `IChunkReinforcer` via capability `WarForgeCapabilities.CHUNK_REINFORCER`: radius (Chebyshev chunks), bonus, `stacksWithOthers()`; non-stacking => strongest one applies. Counts only in loaded defender claims. `IClaimStrengthModifier` deprecated, still honoured.
- `IItemYieldProvider` on a `Block`: yield source.
- `VeinWeightProvider`: `multipliers` (non-null => weighted roll, megachunk quotas bypassed), `assigned` (forces vein or empty), `richness` (quality shift `round(log2)`). Consulted once per chunk at first roll; result persisted.

## Client: `com.flansmod.warforge.api.client.WarforgeClientApi`
- Client only, render thread. Never load on a dedicated server.
- `factionNameOf(UUID)` (nullable, requests if absent), `ownFactionId()`, `isTeammate(UUID)`
- `setNameplatePolicy(NameplatePolicy)`: `HIDDEN`/`NORMAL`/`SEE_THROUGH` per entity per frame, no allocation
- `requestChunkMap(ns, dim, cx, cz, radius)`, `rebuildChunkMap(ns)`, `chunkMapTexture(ns, dim, cx, cz)`, `releaseChunkMap(ns)`; the request goes through the vetted `PacketRequestTerrainColors` (IN_RANGE)

## Serverbound packets
- C2S = `ServerboundPacket` + `Exposure` at registration in `WFNetwork`; `PacketVetting` runs before `handleServerSide`; failure => `PacketSecurity.violation`, no handler.
- `Exposure`: `PUBLIC` | `SELF` | `OWN_FACTION` | `OWN_FACTION_ROLE(min)` | `IN_RANGE(radius)` (packet overrides `target()`).
- Handlers keep domain checks only (officer to disband, costs, ...).
- Responses carry only what the requester may see; request fields decode through `PacketSecurity` readers with bounds.
- Regression: `WFNetworkExposureTest`; `PUBLIC` packets must equal its allowlist. Adding a public query = edit the test.

| packet | exposure |
|---|---|
| RequestFactionInfo | PUBLIC (member roster only for members; leader always) |
| RequestLeaderboardInfo | PUBLIC |
| RequestNamePlate | PUBLIC (target must be within 256 blocks, same dimension) |
| ChunkPosVeinID | IN_RANGE(2) |
| RequestClaimChunks, RequestTerrainColors | IN_RANGE(manager radius + siege declare range + 1) |
| RemoveClaim, ClaimChunkAction | IN_RANGE(manager radius + 1) |
| StartSiege, DeclareSiege | OWN_FACTION_ROLE(OFFICER) |
| DisbandFaction | OWN_FACTION_ROLE(LEADER) |
| SetFactionColour, MoveCitadel, RequestUpgradeUI, RequestUpgrade, ChooseFactionFlag, FactionMemberManagerAction, FactionAllianceAction, RequestInsurance, RequestFobWarp, FobManagerAction, RequestOperations | OWN_FACTION |
| CreateFaction, RequestMemberData | SELF (answers only the sender's own invites or faction) |

## Compat: Dynmap (`compat/dynmap`)
- Dependency: `us.dynmap:DynmapCoreAPI:3.7-beta-6` compileOnly (repo.mikeprimm.com), all nodes. Loader-neutral API; `DynmapBridge` -> `DynmapLayer` loaded only when mod `dynmap` is present.
- Dynmap runtime: Forge 1.20.1 (`Dynmap-3.7-beta-6-forge-1.20`) only. 1.21.1 / 26.1.2 / 26.2 NeoForge: no Dynmap release (Forge jars require mod `forge`; upstream NeoForge PR #4183 open) => layer inert there, untested.
- Marker set `warforge.factions`, non-persistent, rebuilt each start. One area marker per hole-free polygon; id `wf_<factionUuid>_<n>`. Safe/war zones = owners `FactionIds.SAFE_ZONE`/`WAR_ZONE`, fixed green/red.
- Geometry `common/util/ChunkOutline`: 4-connected components, boundary trace, collinear vertices dropped. AreaMarker has no holes => component with enclosed hole cut at hole min x into x < cut / x >= cut, recursed. Seam stroke visible between pieces; diagonal pinch stays one self-touching polygon.
- Update: poll every `Dynmap Update Seconds`; claim pass only when `ClaimRegistry.getRevision()` moved; rebuild only owners whose (count, chunk hash) changed; name/colour/members/leader/opacity changes restyle in place. Dynmap calls server thread only (API listener only hands over the API). Gap: polygon build on server thread.
- Dynmap world names (Forge): overworld = level name, `DIM-1`, `DIM1`, else `<ns>_<path>`.
- Reads server services directly (revision + owner index), not `WarforgeAPI`: API snapshots are per-faction and carry no revision or zones.
