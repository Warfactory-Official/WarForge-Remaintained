# Alliances

Managed from the **Alliances** tab of Faction Members (citadel menu **Members**, or inventory faction button). Only ally chat has a command.

## Forming

| Step | Actor |
|---|---|
| **Invite** in "Invite a Faction" | Officer+ of A |
| **Accept** / **Decline** in "Incoming Requests" | Officer+ of B |
| B invites A while A's request is pending | Alliance forms at once |

- Both factions need a free slot: `Max Allies Per Faction` (default 2, -1 unlimited).
- Neutral zones and self not invitable.
- Forming clears any truce between the pair.
- Chat message + toast on every event: request sent/received, formed, declined, broken, Ally Access toggled.

## Effects

- No siege between allies: camp or map declaration refused.
- Alliance does not stop ally PVP.
- `/f ally chat <text>` (`ally msg`, `allychat`, `ac`): message own faction and every transitively allied faction.

## Ally Access

- Per-faction toggle (Officer+) at the top of the Alliances tab; default off.
- On: allied players use profile `ClaimAlly` in that faction's non-citadel claims; default denies Break/Place only, rest allowed.
- Off: allies use `ClaimFoe`.
- One-way: grants the owner's allies access to the owner's land.
- Citadel chunk: `CitadelFoe` for allies.
- Inside siege zones allies are foes (`SiegedFoe` / `WarFoe`); siege zones override `ClaimAlly`.

## Breaking and truce

- **Break** by either side: removes the alliance immediately for both.
- Truce for `Alliance Truce Duration [min]` (default 60; 0 = none):
  - no sieges between the pair,
  - PVP damage between their players cancelled.
- Truce partners count as neither ally nor enemy for FOB holds.

## Config

| Key | Default |
|---|---|
| `Alliance Truce Duration [min]` | 60 |
| `Max Allies Per Faction` | 2 |
| `[ClaimAlly]` section | Break/Place false |

See [Configuration](configuration.md#alliances).
