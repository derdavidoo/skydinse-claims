# Skydinse Claims Handoff

Last updated: 2026-10-27

## Project

Skydinse Claims is a client-side Fabric mod for Minecraft that observes the Skydinse server's claim output, stores claimed chunks, and displays them on Xaero's World Map. It also includes a separate Java HTTP service for sharing claims between players. The repository currently has two Gradle projects: the mod at the root and the cloud service in `server/`.

The mod targets Minecraft `26.3`, uses Fabric Loader and Fabric API, integrates with Xaero's World Map, and requires Java 25. Check the Gradle properties before upgrading these dependencies.

## Client Behavior

- `SkydinseClaimsClient` detects the active game server and records claim data from `/claim list` responses and the scoreboard.
- Capturing new claims requires a valid player movement sample, speed at most 20 blocks per second, and reported ping strictly below 90 ms. The first movement sample after joining or changing servers is ignored because there is no previous position to compare.
- Parsed `/claim list` entries are appended to the local store. Repeated coordinates update their owner; unrelated existing chunks are retained.
- Local claims are cached per game-server address in the Minecraft `config` directory. The in-memory store supports dimension and map-region snapshots used by the renderer.
- Cloud requests run asynchronously. The client pushes its local cache at initial sync, pulls on join, polls every five minutes, and retries failed pushes. Pulled chunks are merged locally and do not trigger a second push. Network failure should leave local cached claims available.
- `ScoreboardRecorder` currently writes scoreboard snapshots to `skydinseclaims-scoreboard.log` every 200 client ticks. Confirm this diagnostic behavior is intended for a release build.

Relevant client code is under `src/main/java/net/skydinse/claims/client/`; parser checks and fixtures are under `src/test/`.

## Cloud Service

The service is a Java 25 application using the JDK HTTP server, Gson, and SQLite JDBC. Its entry point is `server/src/main/java/net/skydinse/claims/server/ClaimsServer.java`.

- `GET /health` is unauthenticated.
- `GET /v1/chunks?server=<address>` pulls claims and requires a bearer token.
- `POST /v1/chunks` upserts a batch and requires a bearer token. Batches are limited to 10,000 entries and request bodies to 2 MB.
- Records are keyed by game-server address, dimension, and chunk coordinates. A later upsert changes the owner. Omitted records are retained; sync has no delete operation.
- Tokens are loaded from `SKYDINSE_CLAIMS_TOKENS`. The service binds to `127.0.0.1:8080` by default and is intended to sit behind an HTTPS reverse proxy.

Deployment examples are in `server/README.md`, `server/*.service`, `server/*.timer`, `server/nginx.conf.example`, and `server/server.env.example`. The documented backup timer makes SQLite online backups every six hours and keeps fourteen days of snapshots. Do not put real environment files, databases, backup files, TLS keys, or credentials in source control.

## Security State

The public source keeps the shared release bearer credential out of Java source. The build can inject `cloudToken` from a local Gradle property or `SKYDINSE_CLAIMS_CLIENT_TOKEN`; a jar built with that value contains an extractable shared credential. The configured endpoint is in the public client defaults. Review both source and generated artifacts before release. Exclude local databases, configs, logs, build outputs, and all deployment secrets from source control.

The shared token grants access to the shared dataset; it does not prove a Minecraft account identity and does not limit a caller to submitting claims under their own player name. HTTPS protects traffic in transit, while abusive clients still require server-side moderation such as proxy/IP controls and database backups. Never place VPS private keys, environment files, live tokens, or other deployment secrets in this project handoff or repository.

## Build and Verification

Run these from the project root with Java 25 installed:

```powershell
.\gradlew.bat check
.\gradlew.bat build
.\gradlew.bat -p server test
.\gradlew.bat -p server installDist
```

The root `check` task also runs `verifyClaimParser`, a standalone parser verification that uses `src/test/resources/claim-list.txt`. The mod jar is written under `build/libs/`. The service distribution is written under `server/build/install/skydinse-claims-server/`.

Previous development work reported a successful mod build and parser verification. Rerun the commands above after changing code or dependencies; do not treat old build folders as proof that the current sources pass.

## Open Work for the Next Developer

1. Rotate the shared bearer credential if a token-bearing jar has been distributed beyond the intended group. Keep release values in local Gradle properties or CI secrets; never commit them.
2. Finish or remove the allowed-server restriction hook in `ClaimsHttpApi`: its optional set is not wired from `ClaimsServer` or an environment setting, and a rejected server currently falls into the generic `400` validation response. Add focused HTTP tests for authentication, validation, upsert, pull, and any restriction behavior.
3. Add a root `.gitignore` before initializing or pushing a Git repository. Exclude all `build*/` outputs, `.gradle/`, `server/smoke.db*`, runtime databases, logs, local configs, and credentials. Review files before staging; do not commit generated jars or VPS data by default.
4. Verify the movement and ping guard in-game, especially after server changes, latency spikes, and large position corrections. Current code checks `ping < 90` and `speed <= 20`.
5. Confirm whether periodic scoreboard snapshot logging should ship to players.
6. Recheck the remote deployment before making operational changes. Use `server/README.md` and the example systemd/nginx/backup files as deployment guidance; actual host details and credentials belong in the operator's secure configuration, not this repository.

## Repository State

At handoff creation, this workspace was not initialized as a Git repository. A root README and `.gitignore` now describe builds and exclude generated output, local databases, configs, logs, and credentials. Review `git status` and scan staged files before every public push. Keep repository work separate from VPS deployment.
