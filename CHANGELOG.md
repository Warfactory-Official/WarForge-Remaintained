# Changelog

## 2.4.3

- 1.20.1/1.21.1: claim borders no longer visible through terrain under Sodium.
- FTB Teams/FTB Quests: faction = FTB party team (same UUID); membership, leader, name, colour mirrored. Optional dep `ftbteams`.
- Flashback: WarForge server logic inert on the replay server (replay crash, erased borders). Recordings store full claim state per snapshot: borders correct on open, after seeks, and away from the recorded player (needs `Whole World Claim Map`). Older recordings lack it.
