# Starsector Shipmaker - Agent Guidelines

## Project Overview
- **Tech Stack:** Java 17 / Swing / Maven / Jackson / Lombok / Log4j2 / LWJGL 3 (OpenGL)
- **Description:** A visual editor for Starsector `.ship`, `.variant`, `.skin`, and `.wpn` files
- **Location:** `/run/media/lechibang/work/starsector/starsector-shipmaker`

## Critical Starsector Engine Knowledge
1. **Launch Bays are weapon slots**: They live in the `weaponSlots` array with `type: LAUNCH_BAY`. They share the same ID namespace as weapons (e.g., `WS xxx`).
2. **`locations` array format**: Flat `[x1, y1, x2, y2, ...]` pairs. First pair = bay center. Subsequent pairs = spawn ports. A single-port bay MUST duplicate its coordinate.
3. **ID prefix**: ALL weapon slots including launch bays MUST use the `WS` prefix (e.g., `WS 001`). Never use `LB`.
4. **Default values**: `arc` should default to 360, `mount` to HIDDEN, `size` to LARGE for launch bays.
5. **Coordinate system**: `locations` are relative to the ship `center`. The `center` field is `[Y_from_bottom, X_from_left]`.
6. **Engine style serialization**: Always use raw `styleID` strings, never rely on resolved style objects.

## Build & Test
- **CRITICAL BUILD RULE:** Always use `mvn package -DskipTests` instead of `mvn compile` for builds and verification to ensure complete shader/dependency packaging.
- `mvn test` - Run full test suite
- `mvn package -DskipTests` - Create release JAR

## GitHub Operations
- Use `gh` CLI for GitHub interactions.
- Central skills repository: `starsector-mods/skills`.
- If auth fails, use `env -u GITHUB_TOKEN git push`.

## Architecture Quick Reference
- **EventBus**: Used for decoupled communication between components.
- **Undo/Redo**: Handled via `EditDispatch`.
- **Coordinate transforms**: `rotatePointByCenter` (load) ↔ `SHIP_CENTER` mode (save).
- **Initialization & Saving**: `ShipPainterInitialization` loads hull data, `SaveHullAction` serializes it back.
- **OpenGL Weapon & Missile Rendering**:
  - Weapon recoil direction: In editor space (0° pointing UP), recoil vector is `(-Math.sin(rotRads), Math.cos(rotRads)) * recoilOffset` (recoiling DOWN into the hardpoint/mount).
  - Hardpoint sprite pivot: Engine uses `setCenter(width/2, height/4)` for hardpoints vs `(width/2, height/2)` for turrets. In editor top-down space, hardpoint Y-ratio is `0.75` (not `0.5`).
  - Missile sprite scaling: Engine calls `sprite.setSize(size.x, size.y)` using the `.proj` `"size"` field, then `sprite.setCenter(center.x, center.y)` using `.proj` `"center"`. The center is relative to the `"size"` dimensions (bottom-left origin), NOT the raw image pixel dimensions. Use `spriteDimensions` (from `.proj` size) for both draw size and Y-flip pivot in `ProjectilePainter`.
  - Loaded missile animation: When rendering missiles inside launchers in `WeaponPainter.paintLoadedMissilesGL`, add the launcher's `recoilVector` to the missile's `paintAnchor` so missiles move synchronously with the recoil.
- **Database & Memory Indexing**:
  - `CoreIndexManager`: In-memory index of `starsector-core` data. Collections must be cleared at the start of `loadCoreData()` to prevent memory cache accumulation.
  - `DatabaseQueryService`: SQLite layer using HikariCP (`SQLite-Pool`) and WAL mode. `getFilesByType` deduplicates entries by canonical file path.
  - `HullsTreePanel`: Tree cell renderer prefixes hull size `[Frigate]`, `[Destroyer]`, `[Cruiser]`, `[Capital]` and displays entity ID/name for duplicate base names. Empty names fall back to `hullID`.

## Mod Data Integrity Pitfalls
1. **Duplicate Slot IDs**: Always check for duplicate slot IDs in arrays. The engine uses strict parsing, whereas scripting parsers (like Python's `json`) may silently overwrite duplicates, causing unexpected serialization drops in nested objects (like `builtInWeapons`).
2. **.skin File Dependencies**: Changes to a `.ship` file's slot IDs break any `.skin` file that references them in `weaponSlotChanges`.
3. **.variant Hull IDs**: A `.variant` file's `hullId` can point to either a base `.ship` ID or a `.skin` ID.
4. **Java 17 Bytecode Requirement**: Starsector 0.98a runs OpenJDK 17 LTS. All mod JARs must target bytecode version 61.0 (`--release 17`). Ignore misleading "Java 7" error messages caused by JDK 21+ bytecode.
5. **Weapon Barrel Balance**: Burst sizes in `weapon_data.csv` must not exceed physical launch tube / barrel counts from `.wpn` offsets.
6. **Skin OP Standardization**: Buffed faction skins add +5/+10/+15/+20 OP by hull size; pirate/scavenged skins apply a flat -10% OP delta.

