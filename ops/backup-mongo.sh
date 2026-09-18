#!/usr/bin/env bash
# Nightly MongoDB backup. Usage: MONGODB_URI="mongodb+srv://..." ./backup-mongo.sh [backup-dir] [keep-days]
# Schedule with cron, e.g.:  0 2 * * *  MONGODB_URI=... /opt/harmonie/ops/backup-mongo.sh /var/backups/harmonie 14
# Restore drill (do this on a scratch cluster, not prod):  mongorestore --uri "$RESTORE_URI" --gzip --archive=<file>
set -euo pipefail
: "${MONGODB_URI:?set MONGODB_URI}"
DIR="${1:-./backups}"; KEEP="${2:-14}"
mkdir -p "$DIR"
FILE="$DIR/harmonie-$(date +%Y%m%d-%H%M%S).archive.gz"
mongodump --uri "$MONGODB_URI" --gzip --archive="$FILE"
find "$DIR" -name 'harmonie-*.archive.gz' -mtime +"$KEEP" -delete
echo "backup written: $FILE"
