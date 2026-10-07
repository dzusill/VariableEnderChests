# Integrations

All integrations are optional soft dependencies and hook in automatically when the plugin is present.

| Plugin | Effect |
|---|---|
| **PlaceholderAPI** | `%oberonender_rows%`, `%oberonender_size%` — see [Placeholders](placeholders.md) |
| **ChestSort** | the ender chest is registered as a sortable inventory |
| **ShowItem** | item previews that show the ender chest display the OberonEnder chest, not the vanilla one |
| **InteractiveChat** | same for InteractiveChat's ender chest preview |

The console prints `Hooked into <plugin>` on startup for every hook that was found.

## Permission plugins

Any permission plugin works. Example for LuckPerms:

```
/lp group vip permission set enderchest.size.4 true
```
