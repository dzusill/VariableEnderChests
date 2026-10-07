# FAQ & Troubleshooting

## A player says "your enderchest could not be loaded"

The stored items could not be read, so the chest was **locked** to protect them. Nothing was deleted and the chest is not saved over. Check the console for the error, fix the cause (database access, corrupt data), and restart. See [Safe Storage](features/safe-storage.md).

## Players cannot open the chest block

Check `enderchest.use` (default `true`) and that the world is not in `disabled-worlds`.

## `/enderchest` says no permission

The command is opt-in. Give `enderchest.command` to the group.

## "You do not have an enderchest"

`default-rows` is `0` and the player has no `enderchest.size.<rows>` node. Give one.

## A player lost items after a rank drop

They are in the [retrieval inventory](features/retrieval-inventory.md): `/retrieveender` (needs `enderchest.retrieve`).

## Changes in config.yml do nothing

Run `/oberonender reload`. Database settings, `open-enderchest-commands` and `papi-identifier` need a restart.

## Converter says "All players must be offline"

Stop everyone from joining, kick online players, then run the command twice within 5 seconds. See [Converters](features/converters.md).

## The vanilla ender chest opens instead

The player is in a world from `disabled-worlds`.
