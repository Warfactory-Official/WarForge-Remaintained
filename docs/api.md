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
| `notifyFaction(faction, @Nullable dedupeToken, Component, NotificationStyle, durationMs)` | toast to online members |
| `siegeDefenceAt(dim, chunk)` | `SiegeDefence(base, member, adjacentSupport, reinforcement, total)` from `SiegeDifficulty`; null if not player-owned |
| `getReinforcementBonus(faction, dim, chunk)` | `IChunkReinforcer` + legacy `IClaimStrengthModifier` |
| `veins()`, `findVein(key)`, `veinAt(ServerLevel, cx, cz)` | `VeinView`, `VeinSample` |
| `setVeinWeightProvider(VeinWeightProvider)` | survives `/f reload`; install before players join (JourneyMap `AUTO` rolls veins around them) |
| `addFobWarpVeto(FobWarpVeto)` | reason `Component` or null. Gap: `fobWarpVeto` not called by `FobManager.requestFobWarp`, so registered vetoes have no effect |
| `pack(cx, cz)`, `unpackX`, `unpackZ` | `ChunkPos.asLong` layout, for `claimedChunks` sets |

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
