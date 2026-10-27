# Skydinse Claims Cloud Server

This service stores the shared claimed-chunk dataset for each Minecraft server address. It uses SQLite and exposes:

- `GET /health` without authentication.
- `GET /v1/chunks?server=<address>` with a bearer token.
- `POST /v1/chunks` with a bearer token and `{ "server": "...", "chunks": [...] }`.

Configure tokens with `SKYDINSE_CLAIMS_TOKENS=PlayerName:long-random-token,OtherPlayer:another-token`. Tokens grant access to the shared dataset; clients may submit any observed owner. Upserts change an existing owner and omitted chunks are retained.

Build and run with Java 25:

```text
gradlew.bat -p server installDist
set SKYDINSE_CLAIMS_DB=C:\ProgramData\skydinse-claims\claims.db
set SKYDINSE_CLAIMS_TOKENS=EinDavidoo:replace-with-a-long-random-token
server\build\install\skydinse-claims-server\bin\skydinse-claims-server.bat
```

The app listens on port `8080` by default. Put it behind HTTPS (for example, nginx with a Let's Encrypt certificate) and expose only the HTTPS port publicly.

## Client configuration

The public source does not contain a shared client token. A jar built without `cloudToken` or `SKYDINSE_CLAIMS_CLIENT_TOKEN` still supports local claim capture and rendering, but cloud sync stays disabled until a token is configured. For private releases, inject the token at build time as described in the root `README.md`. The optional `.minecraft/config/skydinseclaims-cloud.json` file can override the endpoint and token:

```json
{
  "endpoint": "https://claims.example.com",
  "token": "optional-override"
}
```

The client keeps its existing per-game-server JSON cache and uses the cloud service as an additional shared source. It uploads local changes, pulls immediately when joining, and polls every five minutes. Failed requests are logged and retried; they do not clear local claims.

For a systemd install, create the service user and data directory, copy `server.env.example` to `/etc/skydinse-claims/server.env`, install the distribution under `/opt/skydinse-claims`, then enable the unit:

```text
sudo systemctl daemon-reload
sudo systemctl enable --now skydinse-claims
sudo systemctl status skydinse-claims
```

## Backups and moderation

The deployment includes a root-owned SQLite backup timer. It runs every six hours, uses SQLite's online `.backup` operation so WAL data is included safely, and retains fourteen days of snapshots under `/var/lib/skydinse-claims/backups`. Check it with:

```text
sudo systemctl status skydinse-claims-backup.timer
sudo journalctl -u skydinse-claims-backup.service
sudo ls -l /var/lib/skydinse-claims/backups
```

The shared token is intentionally suitable only for this small private group: anyone who receives the jar can extract it. nginx records client IP addresses in `/var/log/nginx/access.log`. To block an abusive address temporarily, add a narrowly scoped UFW rule (for example, `sudo ufw deny from 203.0.113.10 to any`) and remove it later with `sudo ufw delete deny from 203.0.113.10`.
