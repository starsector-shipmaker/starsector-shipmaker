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
- `mvn compile` - Verify changes (compilation)
- `mvn test` - Run full test suite
- `mvn package -DskipTests` - Create release JAR

## GitHub Operations
- Use `gh` CLI for GitHub interactions.
- If auth fails, use `env -u GITHUB_TOKEN git push`.

## Architecture Quick Reference
- **EventBus**: Used for decoupled communication between components.
- **Undo/Redo**: Handled via `EditDispatch`.
- **Coordinate transforms**: `rotatePointByCenter` (load) ↔ `SHIP_CENTER` mode (save).
- **Initialization & Saving**: `ShipPainterInitialization` loads hull data, `SaveHullAction` serializes it back.
