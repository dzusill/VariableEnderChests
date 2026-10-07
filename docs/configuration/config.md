# config.yml

Lives in `plugins/OberonEnder/config.yml`. New keys are added automatically on update; your values are never overwritten.

```yaml
config-version: 8   # managed by the plugin, do not edit
```

## Database

```yaml
database:
  mysql: false
  host: 'localhost'
  port: 3306
  database: 'database'
  username: 'username'
  password: 'password'
```

`mysql: false` stores everything in `plugins/OberonEnder/data.db`. Set it to `true` only with a MySQL database ready. **Needs a restart.**

## Chest size

| Key | Default | Meaning |
|---|---|---|
| `default-rows` | `3` | rows for players without an `enderchest.size.<rows>` node (0–6). `0` = permission required |
| `convert-current-ender-chest` | `true` | copy a player's vanilla ender chest into OberonEnder on join |

## Language

| Key | Default | Meaning |
|---|---|---|
| `default-locale` | `en_us` | locale used when the player's own locale has no lang file |

See [Messages & Language](messages.md).

## Commands and placeholders

```yaml
papi-identifier: 'oberonender'
open-enderchest-commands:
  - 'enderchest'
  - 'echest'
  - 'ec'
```

- `papi-identifier` — placeholders become `%<identifier>_rows%`. See [Placeholders](../placeholders.md).
- `open-enderchest-commands` — the first entry is the command, the rest are aliases.

Both need a **restart**.

## Restrictions

```yaml
disabled-worlds:
  - 'creative'
blacklisted-items:
  - 'COMMAND_BLOCK'
```

- `disabled-worlds` — in these worlds players get the vanilla ender chest, by block and by command.
- `blacklisted-items` — materials that cannot be put in a chest. An invalid name is logged and ignored.

## Sounds

`sounds.block.*` and `sounds.command.*` — see [Sounds](../features/sounds.md).
