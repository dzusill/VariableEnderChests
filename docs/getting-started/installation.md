# Installation

1. Stop the server.
2. Drop `OberonEnder.jar` into `plugins/`.
3. Start the server. The plugin creates `plugins/OberonEnder/` with `config.yml`, a `lang/` folder and, with SQLite, a `data.db` file.
4. Give your ranks a size permission — see [Quick Start](quick-start.md).

## Updating

Replace the jar and restart. Config changes go through an updater (`config-version`), so **your values are kept** and only new keys are added.

## Coming from VariableEnderChests

The permission nodes stay `enderchest.*`, so existing LuckPerms groups keep working. The data folder is now `plugins/OberonEnder/`: stop the server, remove the old jar, copy `data.db` (or keep your MySQL settings in `config.yml`) into the new folder and start again.

Back up `data.db` first.

## Coming from another ender chest plugin

Use a [converter](../features/converters.md). All players must be offline while it runs.
