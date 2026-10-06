# CLAUDE.md

`OberonEnder` — permission-sized ender chests for the Oberon server. A fork of minion325/VariableEnderChests
(fork: dzusill/VariableEnderChests), renamed for the client. Internal packages and class names
(`me.saif.betterenderchests`, `VariableEnderChests`) were kept on purpose; only player/owner-facing names changed.

## Build

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn -f pom.local.xml clean package   # -> target/OberonEnder.jar
```

- `pom.xml` depends on `org.spigotmc:spigot` (BuildTools only, not installed here). `pom.local.xml` (git-ignored via
  `.git/info/exclude`) is `pom.xml` without that dependency; regenerate it after editing `pom.xml`.
- `ShowItem` and `InteractiveChat` are system-scope jars under `~/.m2`; locally they are compile-only stubs.
- Folia API needs JDK 21+ to compile, the code still targets Java 17.

## Contracts

- **Data folder migration** (`data/LegacyDataMigration`): the first start copies `plugins/VariableEnderChests/`
  into `plugins/OberonEnder/` (owner files replace generated defaults), then writes `.migrated-from-VariableEnderChests`
  so it never runs again. The old folder is left as a backup.
- **Unreadable chests are never saved** (`ItemStackSerializer` throws, `EnderChestSnapshot.loadFailed`). Never turn
  a read failure into an empty chest again; that wiped player chests on the live server.
- **Permission nodes stay `enderchest.*`** — the live server's LuckPerms groups use them.
- Config changes go through `data/ConfigUpdater` (`config-version`), so owner values are kept.

## Test

`mvn test` covers the SQL safety, the migration and sound parsing. Behaviour on a real server was checked on a local
Paper 26.2 server with a probe plugin that loads every stored chest through the API.
