#!/bin/bash
# Helper script to inspect the SQLite database from terminal.
DB_PATH="$HOME/.gemini/antigravity-ide/settings/ship_editor_database.sqlite"

if [ ! -f "$DB_PATH" ]; then
  # Fallback to local settings path if default doesn't exist
  DB_PATH="./ship_editor_database.sqlite"
fi

if [ ! -f "$DB_PATH" ]; then
  echo "SQLite database file not found!"
  exit 1
fi

if ! command -v sqlite3 &> /dev/null; then
  echo "sqlite3 CLI is not installed!"
  exit 1
fi

sqlite3 "$DB_PATH" "$@"
