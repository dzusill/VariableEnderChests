# Requirements

| Requirement | Version |
|---|---|
| Server | Spigot, Paper or Folia (`api-version` 1.13+) |
| Java | **17+** (current Paper runs on Java 21) |
| Database | SQLite (built in) or MySQL — your choice in [config.yml](../configuration/config.md) |

## Optional plugins

| Plugin | What it adds |
|---|---|
| PlaceholderAPI | `%oberonender_rows%` and `%oberonender_size%` — see [Placeholders](../placeholders.md) |
| ChestSort | the ender chest is registered as sortable |
| ShowItem · InteractiveChat | the item-preview commands show the OberonEnder chest instead of the vanilla one |

OberonEnder has no hard dependencies.

## Item storage

Items are read and written with the **server's own item codec**, so new Minecraft versions work without waiting for a library update. The bundled NBT library is only a fallback. See [Safe Storage](../features/safe-storage.md).
