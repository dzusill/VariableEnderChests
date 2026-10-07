# Placeholders

Requires PlaceholderAPI. The identifier is `papi-identifier` in [config.yml](configuration/config.md) (default `oberonender`).

| Placeholder | Returns |
|---|---|
| `%oberonender_rows%` | rows of the player's chest (0–6) |
| `%oberonender_size%` | slots of the player's chest (rows × 9) |
| `%oberonender_slots%` | same as `size` |

The value follows the player's `enderchest.size.<rows>` permission, or `default-rows`.

Placeholders return nothing when there is no player (console, offline).
