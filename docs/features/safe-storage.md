# Safe Storage

OberonEnder is built so a bad read can never turn into an empty chest.

## Unreadable chests are never saved

If the stored items of a player cannot be read (database error, an item the server cannot decode), the chest is marked **load failed**:

- the player gets `internal.enderchest.load-failed` ("locked to protect your items") and cannot open it;
- the chest is **skipped when saving**, so the stored data stays exactly as it was;
- the problem is logged to the console.

Fix the cause, restart, and the chest loads with all items.

## Server item codec

Items are serialized with the server's own codec (`ItemStack.serializeAsBytes` / `deserializeBytes`), so new Minecraft versions and their item components work without a plugin update. The bundled NBT library is only a fallback for servers that lack the codec.

## Storage

| Backend | Setting |
|---|---|
| SQLite (default) | `database.mysql: false` → `plugins/OberonEnder/data.db` |
| MySQL | `database.mysql: true` plus host, port, database, username, password |

Online chests are saved on a timer, on quit and on shutdown. Database settings need a restart to change.

## Back up

Copy `data.db` (or dump the MySQL database) before updates and before running a [converter](converters.md).
