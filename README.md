# Donut HUD (Fabric, client-side)

Shows your Money and Shards (read from the sidebar scoreboard), session gains, and per-hour rates.
Purely a display mod: it sends nothing to the server and automates nothing.

## Keybinds (changeable in Controls > Donut HUD)
- H: toggle HUD
- J: reset session stats
- K: move HUD to the next corner
- L: toggle the auction leaderboard
- O: clear auction bids

## Build
Requires JDK 21 and Gradle (or add the Gradle wrapper: `gradle wrapper`).
    gradle build
The jar appears in `build/libs/donut-hud-1.0.0.jar`. Drop it in `.minecraft/mods` with Fabric Loader + Fabric API.

## If it shows "?"
The mod matches scoreboard lines containing "Money" and "Shards". If the server words them differently,
edit the two regex patterns at the top of `StatsTracker.java`.

## Other Minecraft versions
Change `minecraft_version`, `yarn_mappings`, `fabric_version` in `gradle.properties`
(see https://fabricmc.net/develop). Small API renames may be needed on newer versions.

## Auction leaderboard
Watches chat for bid announcements and shows the top 5 bidders (highest bid each) on the right side of the screen.
It also pops an action-bar alert whenever a new highest bid appears.
If nothing shows up, edit the regex list at the top of `BidTracker.java` to match the server's real bid message wording.
