# Alliances

Factions can form **alliances** with each other. Allies cannot siege one another, can optionally use each other's land, and — when an alliance is broken — are held apart by a temporary **truce** so neither side can instantly backstab the other.

Alliances are managed entirely from the GUI; there is no `/f ally` command.

## Forming an alliance

Open the **Faction Members** screen (the **Members** button on the citadel menu, or your inventory's faction button) and switch to the **Alliances** tab.

The Alliances tab has three groups:

- **Current Allies** — factions you are allied with.
- **Incoming Requests** — factions that have asked to ally with you.
- **Invite a Faction** — every other faction you could send a request to.

To ally:

1. In **Invite a Faction**, click **Invite** next to the faction you want to ally with. This sends them a request (chat message + notification).
2. That faction opens their own Alliances tab and clicks **Accept** on your request (or **Decline** to reject it).
3. Once accepted, you are mutually allied. Both factions are notified.

Alliances require mutual consent — a request only becomes an alliance when the other side accepts. If two factions invite each other, the second invite is treated as an acceptance and the alliance forms immediately.

> Only **Officers and Leaders** (or operators) can manage alliances. Other members can view the Alliances tab but the buttons are disabled for them.
>
> A faction can hold up to **Max Allies Per Faction** alliances (default **10**; set to `-1` for unlimited).

## What an alliance does

- **No sieges between allies.** While two factions are allied, neither can start a siege against the other — by placing a camp **or** by declaring from the map. The attempt is rejected with a message telling you to break the alliance first.
- **Optional land access.** Each faction independently decides whether its allies may interact in its claimed chunks (see below). This is a one-way switch — enabling it lets *your* allies use *your* land; it does not affect what you can do in theirs.

Alliances do **not** stop ally-versus-ally PVP. Allies can still fight each other if they choose; the alliance only blocks sieges (and grants land access if enabled).

## Ally land access (the `CLAIM_ALLY` profile)

By default, an ally is treated like any other foreign player in your land. To let allies in, toggle **Ally Access** at the top of the Alliances tab.

When **Ally Access** is **enabled** for your faction, players from your allied factions are governed by the new **`ClaimAlly`** protection profile inside your claims. Out of the box that profile allows allies to:

- open doors, buttons, containers and other blocks (**interact**),
- use items,
- mount and dismount,

but **not** to break or place blocks. Your citadel chunk is excluded — allies are still treated as outsiders there.

Every flag of the `ClaimAlly` profile is fully configurable in `warforge.cfg` under the `ClaimAlly` section, exactly like the other claim profiles (see the [Configuration Guide](configuration.md#protection-sections)). If you want allies to build like full members, set `ClaimAlly - Can Break Blocks` and `ClaimAlly - Can Place Blocks` to `true`.

When **Ally Access** is **disabled** (the default), allies fall back to the normal `ClaimFoe` rules in your land.

## Breaking an alliance and the truce

Either faction can end an alliance at any time from the Alliances tab: click **Break** next to the ally.

Breaking is **unilateral and immediate** — the alliance is removed from both sides at once. To stop the obvious abuse of allying for protection and then instantly sieging or ambushing your former friend, breaking an alliance starts a **truce** between the two factions:

- During the truce, **neither faction can siege the other** (camp or map declaration).
- During the truce, **players of the two factions cannot damage each other** in PVP. Combat between them is simply cancelled until the truce expires.

The truce length is set by **Alliance Truce Duration [min]** (default **60 minutes**). Setting it to `0` disables the truce entirely, allowing immediate retaliation. Forming a fresh alliance with the same faction clears any leftover truce.

Both factions are notified (chat + toast) when an alliance is broken, with the truce time included.

## Notifications

Every alliance event is announced through **both** the chat and the on-screen notification (toast) system, so players see it whether or not they are watching chat:

- request sent / received,
- alliance formed,
- request declined,
- alliance broken (with the truce duration),
- ally access toggled on/off.

## Configuration

All in `config/warforge.cfg`, category **Alliances** (and the `ClaimAlly` protection section). See the [Configuration Guide](configuration.md#alliances) for the full list.

| Option | Default | Effect |
|---|---|---|
| `Alliance Truce Duration [min]` | 60 | How long the post-break truce blocks sieges and PVP. `0` disables it. |
| `Max Allies Per Faction` | 10 | Maximum simultaneous alliances. `-1` for unlimited. |
| `ClaimAlly` section | use/interact, no build | What allies may do in a faction that enabled ally access. |

Restart the server after changing any of these.

## See also

- [Factions, Citadels and Upgrades](factions.md)
- [The Siege System](sieges.md)
- [Command Reference](commands.md)
- [Configuration Guide](configuration.md)
