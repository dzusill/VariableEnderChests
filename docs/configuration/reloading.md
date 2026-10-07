# Reloading

```
/oberonender reload
```

Permission: `enderchest.reload`.

Reloads `config.yml` and the `lang/` files **without** a restart or a plugin reload. Stored chests and the database are not touched.

If the new config is broken, nothing is applied and the command reports `OberonEnder was NOT reloaded: <reason>`.

## Needs a restart

- database settings
- `open-enderchest-commands`
- `papi-identifier`

Everything else — rows, blacklist, disabled worlds, sounds, messages — applies on reload.
