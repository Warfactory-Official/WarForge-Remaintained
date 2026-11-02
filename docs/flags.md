# Faction Flags

Flag = banner on every claim, citadel and FOB pole of the faction.

| Kind | Id | Source |
|---|---|---|
| Default | `default:<name>` | Solid colour: dye name (`red`, `light_blue`) or 6-digit hex (`ff8800`) from `Available Default Flags` |
| Custom | `custom:<name>` | Image file on the server; sent to clients automatically, no client install |

## Choosing

- Leader only; once per faction; players cannot change it afterwards.
- Citadel UI, "Faction Flag" panel, **Choose**, pick, confirm. Applies to all poles at once.
- `Unique Flags` (default true): a flag held by another faction is refused.
- `Flag Whitelist`: flag exclusive to listed player UUIDs; others never see it.
- Admin: `/f flag <faction> set <flagId>` (bypasses `Unique Flags`), `/f flag <faction> reset` (leader may choose again).

## Adding a custom flag

1. Put the image in `resources/warforge/flags/` under the server root (created on first load). File name without extension = flag name; no sub-folders.
2. `[Claims] "Available Custom Flags"` must match the name (default `["*"]`; `*` globs, case-insensitive).
3. `/f reload` (or restart). Connected players receive new images on next login.

| Rule | Value |
|---|---|
| Format | Any image the server JVM decodes; PNG recommended |
| Dimensions | 8..512 px per side |
| Aspect | longer side <= 2x shorter |
| Size | <= 2 MB, checked before and after re-encoding to PNG |

- Violations are skipped with a server log warning naming the file.
- Banner keeps the image aspect; about 3:2 gives the largest banner.

```toml
[Claims]
	"Available Custom Flags" = ["red_dragon", "season1_*"]
```

## Troubleshooting

| Symptom | Cause |
|---|---|
| Not in list | Not reloaded; sub-folder; name not matched by `Available Custom Flags`; failed validation (see log) |
| In list, banner blank | Client connected before the file was added; relog |
| Pole blank after choosing | Relog to resync |
