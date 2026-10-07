# CLAUDE.md

`OberonEnder` — permission-sized ender chests for the Oberon server. Main class `me.saif.betterenderchests.OberonEnder`,
data folder `plugins/OberonEnder/`. The Java package name `me.saif.betterenderchests` was kept on purpose.

## Build

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn -f pom.local.xml clean package   # -> target/OberonEnder.jar
```

- `pom.xml` depends on `org.spigotmc:spigot` (BuildTools only, not installed here). `pom.local.xml` (git-ignored via
  `.git/info/exclude`) is `pom.xml` without that dependency; regenerate it after editing `pom.xml`.
- `ShowItem` and `InteractiveChat` are system-scope jars under `~/.m2`; locally they are compile-only stubs.
- Folia API needs JDK 21+ to compile, the code still targets Java 17.

## Contracts

- **Unreadable chests are never saved** (`ItemStackSerializer` throws, `EnderChestSnapshot.loadFailed`). Never turn
  a read failure into an empty chest again; that wiped player chests on the live server.
- **Items are read and written with the server's own codec** (`ItemStack.deserializeBytes` / `serializeAsBytes`, container
  handled by `utils/RawNbt`). The bundled NBT-API is only the fallback: it does not know new Minecraft versions and on
  Paper 26.3 build 151 it failed with `ITEMSTACK_BUKKITMIRROR`, which left every chest unreadable (and locked).
- **Permission nodes stay `enderchest.*`** — the live server's LuckPerms groups use them.
- Config changes go through `data/ConfigUpdater` (`config-version`), so owner values are kept.

## Test

`mvn test` covers the SQL safety, the item container (`RawNbt`) and sound parsing. Behaviour on a real server was checked on a local
Paper 26.2 server with a probe plugin that loads every stored chest through the API.
