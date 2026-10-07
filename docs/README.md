# OberonEnder

**OberonEnder** replaces the vanilla ender chest with a **permission-sized** one. Every player gets 1–6 rows depending on their rank, the contents are stored in a database instead of the player file, and a chest that cannot be read is **locked, never wiped**.

It is the Oberon build of [VariableEnderChests](https://github.com/minion325/VariableEnderChests) by minion325, with safer storage, a `/oberonender reload` command, a separate `enderchest.use` permission and configurable open/close sounds.

---

## What it does

- 📦 **Permission-sized chests** — `enderchest.size.1` … `enderchest.size.6` give 9–54 slots. The highest node wins, `default-rows` covers everyone else.
- 🔒 **Never wipes a chest** — if stored items cannot be read, the chest is locked and never saved over. See [Safe Storage](features/safe-storage.md).
- 🎒 **Retrieval inventory** — items above a player's current row count are not lost when their rank drops; `/retrieveender` brings them back.
- 🔊 **Configurable sounds** — separate open/close sounds for the chest block and for the command, resource-pack sounds included.
- 🚫 **World and item rules** — disable the plugin per world, blacklist materials.
- 🔄 **Converters** — import from vanilla, EnderPlus, AdvancedEnderchest and more. See [Converters](features/converters.md).
- 🌍 **Translations** — English, Spanish, Hungarian and Simplified Chinese lang files.
- 🧵 **Folia supported.**

---

## Requirements

| Requirement | Version |
|---|---|
| Server | Spigot / Paper / Folia, `api-version` 1.13+ |
| Java | **17+** (Java 21 on current Paper) |
| PlaceholderAPI | optional — `%oberonender_rows%` / `%oberonender_size%` |
| ChestSort · ShowItem · InteractiveChat | optional soft dependencies |

See [Requirements](getting-started/requirements.md).

---

## Quick links

- [Installation](getting-started/installation.md)
- [Quick Start](getting-started/quick-start.md)
- [Permission-Sized Chests](features/permission-sizes.md)
- [config.yml reference](configuration/config.md)
- [Commands & Permissions](commands-and-permissions.md)
- [Placeholders](placeholders.md)
- [FAQ & Troubleshooting](faq.md)
