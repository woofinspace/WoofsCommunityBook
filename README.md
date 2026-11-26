# Woof's Community Clipboard 1.0.0

A custom Fabric 26.2 mod for Woof's RLCraft.

## What it does

- Adds a physical item named **Woof's Community Clipboard**.
- The server owns one master document.
- Every player is automatically given a clipboard when they join if they don't already have one.
- Right-clicking the clipboard opens a custom editor.
- Saving updates the server master and broadcasts the new version to all online players.
- The document survives server restarts using Minecraft Saved Data.
- A revision number prevents an older edit from overwriting a newer edit.
- The clipboard is intentionally stack size 1.

## Important design detail

The physical copies do NOT each contain an independent copy of the document. The server stores the master document and the item is the player's access token. This prevents copies from getting permanently out of sync.

## Current editor

- 8 pages
- 12 lines per page
- 256 characters per line
- 8 pages total
- Page navigation and Save button

## Commands / obtaining the item

For the first test, use:

`/give @s woofscommunitybook:community_book`

After the mod is installed, joining players automatically receive one if they don't have one.

## Installation

This is a Fabric 26.2 mod and should be installed on **both the dedicated server and every client**.

Your pack already uses Fabric 26.2. Fabric's 26.2 docs specify Loom 1.17, Loader 0.19.3, and Java 25 for development.

1. Install JDK 25 on the computer used to build the mod.
2. Open this project in IntelliJ IDEA.
3. Let Gradle import the project.
4. Run `build` or `gradlew.bat build`.
5. Take the shortest-name JAR from `build/libs`.
6. Put that JAR in the server `mods` folder.
7. Put the same JAR in every player's client `mods` folder.
8. Restart the server and clients.

## Why there is no JAR in this package

The current build environment used to prepare this source package does not have JDK 25 or the Gradle/Fabric dependency cache available, so I did not pretend an untested JAR was ready. The source is configured for your 26.2 toolchain and is intended to be compiled with Java 25.

## Suggested next upgrade

The next version can make the item look like a real clipboard instead of using the vanilla paper texture, and can add:
- named documents / multiple shared clipboards
- admin-only editing
- `/clipboard give <player>`
- `/clipboard reset`
- `/clipboard backup`
- automatic copy recovery if a player loses their clipboard
- a more Create-style clipboard UI
