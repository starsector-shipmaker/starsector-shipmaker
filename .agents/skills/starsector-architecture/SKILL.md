---
name: starsector-architecture
description: Teaches agents the starsector-shipmaker editor's internal architecture
---

# Starsector Shipmaker Architecture

## Core Architecture
- **EventBus**: Decoupled pub/sub communication. Events are records in `communication/events/`. Subscribe via `EventBus.subscribe(owner, listener)`. Always clean up with `cleanupListeners()`.
- **Undo/Redo**: `EditDispatch` posts edits. `PointEdits`, `SlotEdits` handle undoable actions. All mutations go through EditDispatch.
- **Layer System**: `ViewerLayer` → `ShipLayer` / `WeaponLayer`. Each has a `LayerPainter` for canvas rendering.
- **Painters**: `ShipPainter` owns sub-painters: `WeaponSlotPainter`, `LaunchBayPainter`, `EngineSlotPainter`, `BoundPointsPainter`, `CenterPointPainter`, `ShieldPointPainter`.

## Launch Bay Architecture
- `LaunchBay`: Data model (id, arc, angle, size, mount, renderOrderMod, portPoints list)
- `LaunchPortPoint`: Physical spawn coordinate. Extends `BaseWorldPoint`, implements `SlotPoint`. Delegates properties to parent `LaunchBay`.
- `LaunchBayPainter`: Manages both `baysList` (LaunchBay objects) and `portsIndex` (LaunchPortPoint objects). Hotkeys: Shift=add port to selected bay, Ctrl=create new bay.
- `BayDataControlPane`: UI panel for editing bay properties (angle, arc, size, mount, ID).
- `LaunchBaysTree`: Tree view where bays are parent nodes, ports are draggable children.

## Data Loading Pipeline
1. `JsonSpecLoader.loadHullFile()` → Jackson deserializes to `HullSpecFile`
2. `ShipPainterInitialization.loadHullData()` → converts JSON data to canvas entities
3. `initSlots()` → iterates `weaponSlots[]`, creates `WeaponSlotPoint` or `LaunchBay`+`LaunchPortPoint`
4. `rotatePointByCenter()` → transforms JSON coords to canvas coords (90° CCW rotation)

## Data Saving Pipeline
1. `SaveHullAction.rebuildHullFile()` → collects all editor state
2. `rebuildWeaponSlots()` → merges weapon slots + launch bays
3. `transformSlotsFromBays()` → converts `LaunchPortPoint` canvas positions back to JSON coords via `SHIP_CENTER` mode
4. `SlotLocationsSerializer` → writes `Point2D.Double[]` as flat `[x1, y1, x2, y2]` array
5. Single-port bays auto-duplicate their coordinate

## Coordinate Transforms
| Direction | Method | Formula |
|---|---|---|
| Load (JSON→Canvas) | `rotatePointByCenter(input, tc)` | `canvasX = -input.Y + tc.X`, `canvasY = -input.X + tc.Y` |
| Save (Canvas→JSON) | `SHIP_CENTER` mode | `jsonX = -(canvasY - centerY)`, `jsonY = -(canvasX - centerX)` |

These are exact mathematical inverses.

## Additional Resources
Check the `resources/` and `examples/` directories in this skill folder for supplementary materials, reference files, or example implementations (if they exist).
