# Quick Start

Give each rank a size with LuckPerms (or any permission plugin):

```
/lp group default permission set enderchest.size.1 true
/lp group vip permission set enderchest.size.3 true
/lp group mvp permission set enderchest.size.6 true
```

Players now get 9, 27 and 54 slots. Anyone with no size node gets `default-rows` (3 rows out of the box).

## Let players use the command

The chest **block** works for everyone (`enderchest.use` defaults to `true`). The `/enderchest` command is opt-in:

```
/lp group default permission set enderchest.command true
```

## Check it

- `%oberonender_rows%` shows a player's current rows.
- `/enderchest <player>` (needs `enderchest.command.others`) opens someone else's chest.
- Lowering a rank never deletes items — see [Retrieval Inventory](../features/retrieval-inventory.md).
