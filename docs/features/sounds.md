# Sounds

Sounds play to the player when a chest opens or closes. There are four independent settings under `sounds:` in [config.yml](../configuration/config.md):

| Section | Played when |
|---|---|
| `sounds.block.open` / `close` | the ender chest **block** is used |
| `sounds.command.open` / `close` | the chest is opened with `/enderchest` and its aliases |

```yaml
sounds:
  block:
    open:
      enabled: true
      sound: 'BLOCK_ENDER_CHEST_OPEN'
      volume: 1.0
      pitch: 1.0
```

- `sound` takes a Bukkit `Sound` name (`BLOCK_ENDER_CHEST_OPEN`) **or** a resource key (`block.ender_chest.open`). Resource-pack sounds such as `myserver:ui.vault_open` work too.
- `enabled: false` plays nothing.
- `volume` and `pitch` are plain numbers.

Apply changes with [`/oberonender reload`](../configuration/reloading.md).
