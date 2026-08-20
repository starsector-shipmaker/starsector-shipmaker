---
name: starsector-data-parsing
description: Teaches agents how Starsector data files work and how the editor parses/serializes them.
---

# Starsector Data Parsing

This skill teaches agents how Starsector data files work and how the editor parses/serializes them.

## .ship File Format
- JSON with comments (`#` style), parsed by custom `JsonProcessor`
- Key fields: `hullId`, `hullName`, `hullSize`, `center` [Y,X], `height`, `width`, `spriteName`, `style`, `bounds`, `weaponSlots`, `engineSlots`, `builtInWeapons`, `builtInWings`, `builtInMods`
- `weaponSlots` array contains ALL slots including LAUNCH_BAY type
- `locations` is a flat array of X,Y pairs (NOT nested arrays)
- For LAUNCH_BAY: first pair = bay center, subsequent = spawn ports. Single-port bays need duplicate coordinates.
- Weapon slot IDs must use `WS` prefix with space and 3-digit padding: `WS 001`

## .variant File Format
- JSON with `variantId`, `hullId`, `weaponGroups` (array of groups with slot-to-weapon mappings), `wings` (array of wing IDs), `hullMods`
- Slot IDs in variants must match the `.ship` file's `weaponSlots` IDs exactly

## .skin File Format
- JSON overriding hull properties: sprite, weapon slot overrides, engine style overrides

## Serialization Pipeline
- Jackson ObjectMapper with custom serializers/deserializers
- `Point2DArrayDeserializer`: reads flat number arrays as Point2D pairs
- `SlotLocationsSerializer` / `Point2DArraySerializer`: writes Point2D[] back as flat arrays
- `SaveHullAction.transformSlotsFromBays()`: converts LaunchBay port positions using `SHIP_CENTER` coordinate mode
- `SaveHullAction` single-port duplication fix: `if (locations.length == 1) { locations = new Point2D.Double[]{locations[0], locations[0]}; }`

## Coordinate System
- Ship JSON `center` field: `[Y_offset_from_bottom, X_offset_from_left]`
- All `locations` in weapon slots: relative to `center` point
- Editor load transform: `rotatePointByCenter()` — 90° CCW rotation
- Editor save transform: `SHIP_CENTER` mode — exact inverse
- Round-trip is mathematically lossless

## Common Pitfalls
- Never use `LB` prefix for launch bay IDs — engine silently ignores them
- Never save launch bays with arc=0 — use 360 as default
- Never save single-port bays without duplicating the coordinate
- Always preserve raw string IDs for engine styles, don't resolve to objects
- Odd number of values in `locations` array crashes the engine
