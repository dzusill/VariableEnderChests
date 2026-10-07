# Permission-Sized Chests

A player's chest size is decided by the **highest** `enderchest.size.<rows>` node they have.

| Permission | Rows | Slots |
|---|---|---|
| `enderchest.size.1` | 1 | 9 |
| `enderchest.size.2` | 2 | 18 |
| `enderchest.size.3` | 3 | 27 |
| `enderchest.size.4` | 4 | 36 |
| `enderchest.size.5` | 5 | 45 |
| `enderchest.size.6` | 6 | 54 |

Without any size node the player gets `default-rows` from [config.yml](../configuration/config.md) (default **3**, clamped to 0–6).

## No chest by default

Set `default-rows: 0` to make the chest a rank perk. Players without a size node then get the `no-rows` message and cannot open a chest.

## Opening

| How | Needs |
|---|---|
| Click an ender chest block | `enderchest.use` (default `true`) |
| `/enderchest` | `enderchest.command` |
| `/enderchest <player>` | `enderchest.command.others` |

Taking and placing items in another player's chest needs `enderchest.modify.others` on top.

## Limits

- Worlds listed in `disabled-worlds` get the **vanilla** ender chest, by block and by command.
- Materials in `blacklisted-items` cannot be put into a chest (`blacklisted-message`).

## When a rank changes

Rows are re-read when the player joins and when the chest opens. Dropping to fewer rows moves the extra items to the [Retrieval Inventory](retrieval-inventory.md) — nothing is deleted.
