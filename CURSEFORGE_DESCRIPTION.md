# Skydinse Claims

See claimed chunks on Xaero's World Map while playing on Skydinse-supported servers.

Skydinse Claims detects claim information from the server and highlights those chunks on your map. It supports shared cloud data, so players using the configured release can see claim updates collected by other players.

## Features

- Highlights claimed chunks on Xaero's World Map.
- Uses distinct colors for different owners.
- Marks unclaimed chunks in a translucent red tint with an `Unclaimed` label.
- Gives your own claimed chunks a distinctive outline.
- Stores a local claim cache for each game server, so claims remain available if cloud sync is temporarily unavailable.
- Synchronizes claims with the shared cloud service when connected, then checks for updates periodically.
- Filters automatic claim capture to movement speeds of 20 blocks per second or less and server ping below 90 ms to reduce inaccurate records.
- Includes a map button to toggle claim highlighting.

## Requirements

- Minecraft `26.3`
- Java `25` or newer
- Fabric Loader `0.19.5` or newer
- Fabric API
- Xaero's World Map `1.47.0`
- A supported Skydinse server

Install the mod on the Minecraft client. Xaero's World Map is required for the map highlighting feature.

## Cloud Sync and Data

The official release is configured to connect to the Skydinse Claims cloud service. It sends observed claim owners, chunk coordinates, dimensions, and the address of the game server being played on. The service combines updates from participating players; newer reports can update an owner's name, while chunks omitted from an update are retained.

The shared sync credential is included in the distributed client jar so players can connect without manual setup. As with any credential shipped in a client application, it can be extracted from the jar. Cloud access is shared among release users, and the service operator may review or moderate submitted data. Local claim caching continues to work when the cloud service cannot be reached.

## Author

DerDavidoo
