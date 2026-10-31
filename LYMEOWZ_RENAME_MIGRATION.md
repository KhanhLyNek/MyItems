# LyMeowz server migration (Paper 26.3)

## Runtime names and commands

- `AgarthaLib` (Bukkit identity) -> **LyMeowzLib**, main command `/lymeowzlib`; original `/agartha` alias retained
- `MyItems` (Bukkit identity) -> **LyMeowzItem**, main command `/lymeowzitem`
- Legacy `/myitems` and `/agarthalib` roots are **not** registered
- All *other* Bukkit commands and aliases (including `/myitem`, `/mi`, `/itematt`, `/lore` and `/enchant`) remain as before
- `myitems.*` and `agarthalib.*` permissions remain valid; no permission rewrites
- Historic Java packages/classes (`com.praya.myitems.*`, `com.praya.agarthalib.*`), PAPI IDs, NBT keys, YAML command IDs, and item storage formats are intentionally unchanged to preserve existing items, API integrations and user permission configs
- Gradle artifact IDs are **LyMeowzLib** / **LyMeowzItem** under the historic `com.praya` group; the GitHub repository URLs are not renamed in this update

## Upgrading a server

1. Back up the entire `plugins/` folder, worlds, player data and inventory/ender-chest data.
2. Remove the **old JARs** (AgarthaLib and MyItems); never install old and new JARs together.
3. Install both new LyMeowzLib and LyMeowzItem JARs together on Paper 26.3, then restart the server.
4. On startup both plugins copy *missing files only* from `plugins/AgarthaLib/` to `plugins/LyMeowzLib/` and from `plugins/MyItems/` to `plugins/LyMeowzItem/`. They preserve old folders, refuse to follow symlinks, and do not overwrite files already in the new folders.
5. Migrate any explicitly scripted `/myitems` root to `/lymeowzitem`. Other commands/aliases remain unchanged; existing `myitems.*` permission nodes remain valid.
6. Other plugins that check the exact Bukkit plugin name must be configured to use `LyMeowzLib` or `LyMeowzItem`. Java API class names remain compatible; their plugins may still have hard-coded `depend: [AgarthaLib]` and will need updating by their authors.

The original `resources/Resources/dependency.yml` and `resources/Resources/command.yml` are frozen for item/command compatibility. The runtime dependency resolver transparently translates old `AgarthaLib` hard-dependency entries to `LyMeowzLib`.

CI validates both manifests, runtime registration of new commands, preservation of old aliases, and migration of disposable legacy data directories.
