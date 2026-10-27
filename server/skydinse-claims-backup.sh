#!/bin/sh
set -eu

database=/var/lib/skydinse-claims/claims.db
backup_dir=/var/lib/skydinse-claims/backups
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
backup="$backup_dir/claims-$timestamp.db"

mkdir -p "$backup_dir"
chmod 700 "$backup_dir"

# Python's standard sqlite3 module uses SQLite's online backup API, which
# coordinates with WAL and creates a consistent snapshot while the API serves.
python3 - "$database" "$backup" <<'PY'
import sqlite3
import sys

source = sqlite3.connect(sys.argv[1], timeout=30)
destination = sqlite3.connect(sys.argv[2])
try:
    source.backup(destination)
finally:
    destination.close()
    source.close()
PY
chown root:root "$backup"
chmod 600 "$backup"

# Keep two weeks of snapshots. The current run is never removed by this rule.
find "$backup_dir" -type f -name 'claims-*.db' -mtime +14 -delete
