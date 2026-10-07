# Messages & Language

Messages live in `plugins/OberonEnder/lang/`. Bundled files:

`english.yml` · `spanish.yml` · `hungarian.yml` · `chinese-simplified.yml`

Each file starts with the locale it serves:

```yaml
lang: 'en'
countries:
  - 'US'
  - 'AU'
  - 'UK'
  - '**'     # any other country
```

A player gets the file that matches their client locale; otherwise `default-locale` from [config.yml](config.md) is used. To add a language copy `english.yml`, set `lang` and `countries`, and translate.

## Configurable messages

Under `configurable:`. Every message is a list of lines and supports `&` colors.

| Key | Used for |
|---|---|
| `enderchest-names.1-rows` … `6-rows` | inventory title per size, `<player>` is replaced |
| `enderchest-names.retrieval` | title of the [retrieval inventory](../features/retrieval-inventory.md) |
| `no-permission-use` | no `enderchest.use` |
| `no-rows` | the player has no chest (`default-rows: 0`) |
| `no-enderchest-found` | target player has no chest |
| `blacklisted-message` | blacklisted item |
| `enderchest-command-messages.*` | `/enderchest` denied or world disabled |
| `retrieval-command-messages.*` | `/retrieveender` denied or nothing to retrieve |
| `command-usage`, `command-console-only` | usage and console-only notices |

The `internal:` section (converter, clear and console messages) is meant for translation only.

Reload with [`/oberonender reload`](reloading.md).
