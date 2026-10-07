# Converters

Converters import ender chests from another source into OberonEnder.

```
/enderchestconvert <converter> [flags]
```

Alias: `/echestconvert`. **Console only.**

| Converter | What it does |
|---|---|
| `VanillaConverter` | imports the vanilla ender chests from the world's `playerdata` folder. Players who already have an OberonEnder chest are skipped; add the flag `overwrite` to replace them |
| `EnderPlusOld` | imports old EnderPlus data |
| `EnderPlusNew` | imports current EnderPlus data |
| `AdvancedEnderchest` | imports AdvancedEnderchest data |
| `CustomEnderChestFlatFile` | imports CustomEnderChest flat files |
| `BukkitToNBT` | converts items already stored by OberonEnder from the old Bukkit format to NBT |
| `SQLiteMySQLConverter` | moves stored data between SQLite and MySQL |

Names are not case sensitive and are tab-completed in the console. A wrong name prints the list.

## How it runs

1. **Every player must be offline.**
2. Run the command once — it prints a warning and waits.
3. Run the **same command again within 5 seconds** to start.
4. The console prints `Conversion successfully finished` or `Conversion failed`.

The converters that touch stored data write a backup to `plugins/OberonEnder/backups/` first. Still keep your own copy of `data.db` before a big import.

## Vanilla chests on join

`convert-current-ender-chest: true` (default) copies a player's vanilla ender chest into OberonEnder when they join, so new setups need no converter. The vanilla chest itself is not changed. Set it to `false` to start everyone with an empty chest.
