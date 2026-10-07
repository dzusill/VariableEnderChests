# Commands & Permissions

## Commands

| Command | Aliases | Permission | What it does |
|---|---|---|---|
| `/enderchest` | `/echest`, `/ec` | `enderchest.command` | open your own chest |
| `/enderchest <player>` | | `enderchest.command.others` | open another player's chest (also offline) |
| `/retrieveender [player]` | | `enderchest.retrieve` · `.others` | open the [retrieval inventory](features/retrieval-inventory.md) |
| `/clearenderchest <player>` | `/clearechest` | `enderchest.clear` | clear a chest; run again within the confirm window to confirm |
| `/oberonender reload` | | `enderchest.reload` | [reload](configuration/reloading.md) config and lang files |
| `/enderchestconvert <converter>` | `/echestconvert` | console only | [import](features/converters.md) data |
| `/vecdebug` | | `enderchest.debug` | debug output for admins |

The names in the first row (`enderchest`, `echest`, `ec`) come from `open-enderchest-commands` in [config.yml](configuration/config.md).

### From the console

```
/enderchest <player> [other player]
/retrieveender <player> [other player]
```

Opens the chest of *other player* (default: *player*) for the online *player*.

## Permissions

| Permission | Default | Allows |
|---|---|---|
| `enderchest.use` | **true** | open an ender chest block |
| `enderchest.command` | op | `/enderchest` |
| `enderchest.command.others` | op | `/enderchest <player>` |
| `enderchest.modify.others` | op | take and put items in another player's chest (needs `.command.others`) |
| `enderchest.retrieve` | op | `/retrieveender` |
| `enderchest.retrieve.others` | op | `/retrieveender <player>` |
| `enderchest.clear` | op | `/clearenderchest` |
| `enderchest.reload` | op | `/oberonender reload` |
| `enderchest.debug` | op | `/vecdebug` |
| `enderchest.size.1` … `enderchest.size.6` | op | chest of 1–6 rows, highest wins |

The node names are unchanged from VariableEnderChests, so existing LuckPerms groups keep working.
