# Woof's Community Book

A server-synchronized book for Woof's RLCraft. Everyone on the server shares one document, and every Community Book is a key to it.

## Features

- Every player gets a Community Book when they join, if they don't already have one.
- Right-click the book to open the editor.
- Saving updates the server's shared copy and sends it to all online players.
- The document survives server restarts.
- A revision number stops an older edit from overwriting a newer one.
- The book has a stack size of 1.
- The editor has 8 pages, 12 lines per page, and 256 characters per line.

## How it works

The physical books don't each hold their own copy of the document. The server keeps one master document, and each book is an access token for it. That keeps every copy in sync.

## Supported versions

| Minecraft | Mod loader | Fabric API | Java | Status |
| --- | --- | --- | --- | --- |
| 26.2 | Fabric Loader 0.19.3 or newer | 0.158.0 or newer | 25 | Supported |

Other Minecraft versions and mod loaders aren't supported yet. Ready-to-use JAR files are in the [`builds` branch](https://github.com/woofinspace/WoofsCommunityBook/tree/builds/fabric).

## Source by version

Each mod version has its own folder with the full source for that version:

- [`versions/1.1.0`](versions/1.1.0): current. Adds Government (config modes, bills, menu).
- [`versions/1.0.0`](versions/1.0.0): the first Fabric 26.2 release, kept for history.

## Installation

The mod needs to be installed on both the server and every player's client.

1. Install Fabric Loader 0.19.3 or newer and Fabric API 0.158.0 or newer for Minecraft 26.2.
2. Download the Community Book JAR from the [`builds` branch](https://github.com/woofinspace/WoofsCommunityBook/tree/builds/fabric).
3. Put the JAR in the `mods` folder on the server and on every client.
4. Start the server and the clients.

## Getting a Community Book

Players receive one automatically when they join. To give one manually, run:

```
/give @s woofscommunitybook:community_book
```

## Building from source

1. Install JDK 25.
2. Install Gradle 9.5.1.
3. In the project folder, run `gradle build`.
4. The JAR is written to `build/libs`.

## License

See [LICENSE](LICENSE).



    99% Coded by AI
