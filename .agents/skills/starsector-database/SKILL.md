---
name: starsector-database
description: SQLite database design, indexing engine, transaction strategies, and query service in Starsector Ship Editor.
---

# Starsector Ship Editor — Database Design

## Skill Directory Structure

This skill is organized as follows:
- **`SKILL.md`**: Main instructions (this file).
- **`resources/`**: Configurations and schemas.
  - [schema.sql](file:///media/lechibang/WORK1/projects/starsector-shipmaker/.agents/skills/starsector-database/resources/schema.sql): Raw DDL SQL script defining the DB tables.
- **`examples/`**: Code references.
  - [TransactionExample.java](file:///media/lechibang/WORK1/projects/starsector-shipmaker/.agents/skills/starsector-database/examples/TransactionExample.java): Reference implementation of batch updates in a transaction block.
- **`scripts/`**: Tooling.
  - [sqlite_query.sh](file:///media/lechibang/WORK1/projects/starsector-shipmaker/.agents/skills/starsector-database/scripts/sqlite_query.sh): Command-line helper to execute sqlite3 queries.

## 1. Overview

The editor uses an embedded **SQLite** database (`ship_editor_database.sqlite`) managed by [DatabaseManager.java](file:///media/lechibang/WORK1/projects/starsector-shipmaker/src/main/java/shipeditor/persistence/database/DatabaseManager.java). The database indexes all game and mod files by entity type and ID, enabling instant lookups without full-disk rescans on every launch.

---

## 2. Schema (DDL)

### `mods` Table
```sql
CREATE TABLE IF NOT EXISTS mods (
    id TEXT PRIMARY KEY,          -- Internal mod ID or "starsector-core"
    name TEXT NOT NULL,           -- Friendly display name
    folder_path TEXT NOT NULL,    -- Absolute path to the mod folder
    last_scanned INTEGER NOT NULL -- Epoch milliseconds of last full scan
);
```

### `indexed_files` Table
```sql
CREATE TABLE IF NOT EXISTS indexed_files (
    uuid TEXT PRIMARY KEY,          -- UUID as string (not native SQLite UUID)
    mod_id TEXT,                    -- FK → mods.id
    entity_id TEXT,                 -- hullId / skinHullId / variantId / weapon id
    entity_name TEXT,               -- Friendly name (filename without extension)
    entity_type TEXT NOT NULL,      -- SHIP, SKIN, WEAPON, VARIANT, PROJECTILE, *_CSV, *_JSON
    file_name TEXT NOT NULL,        -- Just the filename
    file_path TEXT NOT NULL,        -- Absolute path on disk
    last_modified INTEGER NOT NULL, -- File's lastModified epoch ms
    FOREIGN KEY(mod_id) REFERENCES mods(id) ON DELETE CASCADE
);
```

### Indexes
```sql
CREATE INDEX IF NOT EXISTS idx_entity_id ON indexed_files(entity_id);
CREATE INDEX IF NOT EXISTS idx_entity_type ON indexed_files(entity_type);
```

### Entity Types
| `entity_type` | Source Files |
|---|---|
| `SHIP` | `.ship` files |
| `SKIN` | `.skin` files |
| `WEAPON` | `.wpn` files |
| `VARIANT` | `.variant` files |
| `PROJECTILE` | `.proj` files |
| `SHIP_CSV` | `data/hulls/ship_data.csv` |
| `WEAPON_CSV` | `data/weapons/weapon_data.csv` |
| `HULLMOD_CSV` | `data/hullmods/hull_mods.csv` |
| `SHIPSYSTEM_CSV` | `data/shipsystems/ship_systems.csv` |
| `WING_CSV` | `data/hulls/wing_data.csv` |
| `ENGINE_STYLE_JSON` | `data/config/engine_styles.json` |
| `HULL_STYLE_JSON` | `data/config/hull_styles.json` |

---

## 3. Connection Management (HikariCP Pooling & WAL)

```java
public static Connection getConnection() throws SQLException {
    if (dataSource == null) {
        initDataSource();
    }
    return dataSource.getConnection();
}
```

The database utilizes **HikariCP** (`HikariDataSource`) connection pooling (`SQLite-Pool` with maximum pool size 10) configured with SQLite PRAGMAs:
- `PRAGMA foreign_keys = ON;`
- `PRAGMA journal_mode = WAL;` (Write-Ahead Logging for non-blocking concurrent reads during indexing)
- `PRAGMA synchronous = NORMAL;`
- `PRAGMA cache_size = -64000;` (64 MB page cache)
- `PRAGMA temp_store = MEMORY;`
- `busyTimeout = 5000;`

### Path Forward-Slash Normalization
```java
"jdbc:sqlite:" + getDatabaseFilePath().toAbsolutePath().toString().replace("\\", "/")
```
SQLite JDBC URLs require forward slashes even on Windows to prevent path escape errors.

---

## 4. In-Memory Core Index & Query Deduplication

### `CoreIndexManager`
- Manages an in-memory cache of `starsector-core` files so that core data remains accessible instantly without full SQLite round-trips.
- **Cache Accumulation Safeguard**: All collections (`coreFilesByType`, `coreFilesByEntityId`, `coreFilesByPath`) must be cleared at the start of `loadCoreData()` under `synchronized (LOCK)` before loading from DB cache or scanning from disk.

### `DatabaseQueryService.getFilesByType`
- Merges results from `CoreIndexManager` and active mods querying the SQLite database.
- Results are deduplicated by canonical file path (`LinkedHashMap<String, IndexedFile>`) to prevent duplicate entries from appearing in UI tree components.


---

## 4. Integrity Checking & Self-Healing

`isDatabaseValid()` performs three checks:
1. **Connectivity**: Can a connection be opened?
2. **Integrity**: `PRAGMA integrity_check` returns `"ok"`?
3. **Schema**: Can `SELECT count(*) FROM mods` and `SELECT count(*) FROM indexed_files` execute without error?

If any check fails, `initializeDatabase()` **deletes the database file** and recreates it from scratch:
```java
if (databaseExists() && !isDatabaseValid()) {
    log.warn("Database file exists but is invalid or corrupted. Deleting and recreating...");
    Files.deleteIfExists(getDatabaseFilePath());
}
```

This is acceptable because the database is a **derived cache** — all data can be reconstructed by rescanning the filesystem.

---

## 5. Indexing Engine: `IndexScannerTask`

[IndexScannerTask.java](file:///media/lechibang/WORK1/projects/starsector-shipmaker/src/main/java/shipeditor/parsing/loading/IndexScannerTask.java) orchestrates the full indexing pipeline.

### Differential Update Strategy
On subsequent runs, the scanner compares the `last_modified` timestamp of each mod folder against the stored `last_scanned` value. Only changed folders are rescanned.

### Within a Folder: File-Level Diffing
For each mod folder being scanned, the scanner:
1. Queries `getFilesLastModifiedMap(conn, modId)` to get all known files and their timestamps.
2. Walks the filesystem and compares `file.lastModified()` against the database value.
3. Only files with newer timestamps are re-parsed for entity ID extraction and upserted.

### Transaction Strategy (Quirk)
The entire scan runs in a **single SQLite transaction**:
```java
conn.setAutoCommit(false);
// ... all upserts ...
conn.commit();
```
This is critical for performance — without a transaction, each `INSERT` would trigger a separate WAL flush, making a full index of ~10,000 files take minutes instead of seconds.

### Batch Size
PreparedStatement batches are flushed every **500 operations** (`batchCount % 500 == 0`), balancing memory usage against round-trip overhead.

### Orphan Cleanup
After scanning, `deleteOrphanedFiles(conn, modId, activePaths)` purges database records for files that no longer exist on disk. The implementation:
1. Queries all stored `file_path` values for the mod.
2. Compares against the `activePaths` set (built during scanning).
3. Deletes any path not in the active set, batched in groups of 500.

### `LibModFilter` (Quirk)
Mods identified as "library mods" (e.g., LazyLib, MagicLib) are **skipped** during scanning. These mods contain no ship/weapon data — only API code — and scanning them wastes time.

### Core Game Mod ID
The core Starsector game folder is always indexed with the hardcoded mod ID `"starsector-core"`.

### User Confirmation Dialog
On first run or when changes are detected, the scanner prompts the user via `JOptionPane` (on EDT via `SwingUtilities.invokeAndWait`). On first run the dialog is informational (OK only). On subsequent runs it's a Yes/No confirmation. In headless mode (e.g., testing), the prompt is skipped and the scan proceeds automatically.

---

## 6. Query Service: `DatabaseQueryService`

[DatabaseQueryService.java](file:///media/lechibang/WORK1/projects/starsector-shipmaker/src/main/java/shipeditor/persistence/database/DatabaseQueryService.java) is a static utility class with no instance state.

### Synchronous Methods (Background Scanner)
- `upsertMod()`, `deleteMod()`, `upsertIndexedFile()`, `deleteIndexedFile()`, `deleteOrphanedFiles()`
- These open their own connections (except `deleteOrphanedFiles` which takes a shared `Connection` parameter to participate in the scanner's transaction).

### Synchronous Lookups (Data Loading)
- `getFileNameForEntity(entityId, type)` — Returns the filename for an entity.
- `getFilePathForEntity(entityId, type)` — Returns the absolute `Path` for an entity.
- `getFileByPath(path)` — Reverse lookup by absolute path.
- `getFilesByType(type)` — All indexed files of a given type, ordered by `mod_id ASC, entity_id ASC`.
- `getFilesByTypeGroupedByMod(type)` — Same, but returned as `Map<String, List<IndexedFile>>`.

### Asynchronous Lookups (UI)
- `getFilesByTypeAsync(type)` — Returns `CompletableFuture<List<IndexedFile>>`. Runs the query on `ForkJoinPool.commonPool()`.
- `getFilesByModAndTypeAsync(modId, type)` — Filtered by mod.

These async methods are used by the UI to populate tree views and tables without blocking the Swing EDT.

### `IndexedFile` Record
[IndexedFile.java](file:///media/lechibang/WORK1/projects/starsector-shipmaker/src/main/java/shipeditor/persistence/database/IndexedFile.java) is a Lombok `@Builder` POJO. All fields are `final` — it's an immutable value object. The `filePath` is stored as a `java.nio.file.Path` (converted from the stored `TEXT` column via `Path.of()`).

---

## 7. Database File Location

The database is co-located with the settings file. `DatabaseManager.getDatabaseFilePath()` resolves the settings file path, takes its parent directory, and appends `ship_editor_database.sqlite`. This ensures the database lives alongside the application's configuration, not in a temp directory.
