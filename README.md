# Skydinse Claims

Skydinse Claims is a client-side Fabric mod that records Skydinse claim information and highlights chunks on Xaero's World Map. It includes an optional shared cloud service built as a separate Java application under `server/`.

## Requirements

- Minecraft 26.3
- Java 25 or newer
- Fabric Loader 0.19.5 or newer
- Fabric API
- Xaero's World Map 1.47.0

## Build

Run from the project root:

```powershell
.\gradlew.bat check
.\gradlew.bat build
.\gradlew.bat -p server test
.\gradlew.bat -p server installDist
```

The client mod jar is written to `build/libs/`.

## Cloud Sync Configuration

The public source does not contain the shared bearer token. Without one, local claim capture and map display work, while cloud sync remains disabled until configured.

To configure an installed client, edit `config/skydinseclaims-cloud.json` in the Minecraft game directory:

```json
{
  "endpoint": "https://130.61.216.244",
  "token": "your-cloud-token"
}
```

For a private release build, provide the token outside the repository through a local Gradle property or environment variable:

```powershell
$env:SKYDINSE_CLAIMS_CLIENT_TOKEN = "your-cloud-token"
.\gradlew.bat build
```

The build injects this value into the jar's default cloud configuration. Treat any jar built this way as containing an extractable shared credential. Do not publish that jar or a sources archive alongside the public source unless you intend to distribute the credential.

## Cloud Server

The cloud API, systemd units, nginx examples, and backup scripts are in `server/`. See [server/README.md](server/README.md) for service setup. Keep the real environment file, database, backups, TLS keys, and VPS private keys out of Git.

## Behavior

The client stores claims locally by game-server address, syncs asynchronously when configured, pulls on join, and polls every five minutes. Unclaimed labels (`nobody` and `niemand`) display as `Unclaimed`. Automatic claim capture requires speed at most 20 blocks per second and ping below 90 ms.

See [HANDOFF.md](HANDOFF.md) for project architecture, security context, and current development follow-up.
