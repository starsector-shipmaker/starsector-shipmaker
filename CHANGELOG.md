# Changelog

## [0.0.1f-hf] (Pre-release) - 2026-08-20

### Bug Fixes (Critical)
- **Fix fighters spawning from ship center**: Starsector interprets the first coordinate pair in a LAUNCH_BAY's `locations` array as the bay's logical center, subsequent pairs as spawn ports. Single-port bays wrote `[x, y]` which gave the engine 0 spawn ports, causing center-spawn fallback. Fix: duplicate the coordinate for single-port bays (`[x, y, x, y]`).
- **Fix Launch Bay ID prefix**: Changed default bay ID generation from `LB` prefix to standard `WS` prefix. The engine requires `WS` for all weapon slots including launch bays.
- **Fix default Launch Bay arc**: Changed default arc from `0.0` to `360.0` degrees, matching vanilla carrier standards.
- **Fix engine style serialization crash**: `SaveHullAction` now falls back to raw `styleID` strings when the resolved engine style object is unavailable.

### UI/UX Improvements
- **Launch Bay tree stability**: Tree no longer collapses during port drag (use `repaint()` instead of `reloadModel()`).
- **Port label cleanup**: Removed redundant `#` from port labels.
- **Weapon UI polish**: Added missing tooltips, fixed duplicate headers, resized awkward dialogs, added Clear Filters button.

### Features
- **Hull QA Report Dialog**: New validation tool checking for degenerate arcs (<=0), orphaned bays without ports, duplicate slot IDs, and ports positioned at ship center (0,0). Accessible from Data menu.
- **Launch Bay canvas visualization**: Draw connection lines between ports of the same bay. Arc cone and direction arrow rendering via SlotDrawer.
- **Slot Creation Dialog**: Refactored weapon slot creation defaults into a dedicated modal popup.
- **Showcase Generator**: Add export showcase image generation tool with mod filtering, responsive font scaling, label truncation, and optimized grid layout.
- **Weapon Offset QA**: Add a weapon offset QA report dialog to detect mismatched or asymmetrical barrel placements.
- **Sprite Outline Tracer**: Implement a sprite outline tracer for accurate UI highlighting along with comprehensive regression tests.
- **Export Unification**: Replace specialized export dialogs with a unified `ExportDialog` and extend `ImageExporter` to support configurable formats and background colors.
- **Linux Compatibility**: Add `DISPLAY` environment variable validation and injection for Linux relaunch sequences.
- **Persistent CSV Caching**: Implement persistent CSV file caching in SQLite via `CoreIndexManager` and `DatabaseQueryService`, storing parsed CSV rows with modification timestamps to skip redundant filesystem reads on subsequent startups. (`a20d0b19`)
- **Automatic Mod Registration**: Auto-register mod entries in the `mods` table before upserting `indexed_files` to satisfy foreign key constraints during core index persistence. Add `ensureModExists()` utility in `DatabaseQueryService`. (`93cd287d`)
- **Window & Dialog Sizing**: Set sensible default window dimensions (1440×900 clamped to screen) when no saved bounds exist, increase filter dialog from 300×350 to 400×500, preferences dialog from 400×300 to 600×450, and right instrument pane from 300px to 350px preferred width. (`2eaa3df6`)
- **Descriptive Tree Tooltips**: Resolve `ShipCSVEntry` and `WeaponCSVEntry` in `getTooltipForEntry()` to display human-readable ship/weapon names instead of raw entity IDs. (`1e163f18`)
- **Mod Selection & Persistence**: Add `ModSelectionDialog` for selecting active mods on startup and persisting mod selection state. Implement `ModInfo` metadata parsing to display rich mod cards, add a UI to configure custom mod blacklists in `PreferencesDialog`, and add refresh mechanisms. Revamp the mod selection dialog UI and add mod-aware filtering to database queries.
- **Unified Info Panel**: Introduce `InfoPanelBuilder` to standardize side-panel data presentation across all data tree components. Display descriptive entry names, ship designations, hull sizes, and weapon info in the data panel UI by resolving CSV data.
- **Canvas Export**: Add PNG canvas export functionality for ship/weapon viewer.
- **EventBus Exception Handler**: Add configurable `ExceptionHandler` to `EventBus` to handle uncaught event subscriber errors.
- **Lazy-Loaded Trees**: Implement lazy loading for CSV data trees with case-insensitive alphabetical sorting, recursive path expansion, and memory-optimized key/value string interning. Offload data tree population to background threads and pre-load CSV data during startup to improve UI responsiveness.
- **Data Loading Pipeline**: Implement asynchronous and lazy-loading for variants, hull styles, and engine styles in `GameDataRepository`. Update data loading flags and tree rendering logic.
- **First-Time Setup & Detection**: Enhance first-time setup game folder selection with candidate path drop-down (`JComboBox`), auto-population, macOS path support (`Contents/Resources`), and `starfarer.api.jar` core validation. Make library mod filtering configurable via settings.
- **Collision Bounds & Variant Editor**: Add auto-generation for ship collision bounds using convex hull calculation (`CollisionHullGenerator`) and weapon installation preview in variant editor.

### Refactoring & Performance
- **Lifecycle Management**: Enhance process lifecycle management, implement single-instance locking, and add clean shutdown hooks.
- **Concave Hull Generation**: Implement concave hull generation with morphological dilation and RDP (Ramer-Douglas-Peucker) simplification.
- **OpenGL Line-Loop Rendering**: Expose rotation anchor methods and implement efficient OpenGL line-loop rendering for precise module outlines.
- **Event-Driven Selection**: Replace static `FeaturesOverseer` state with event-driven selection and make `CoreIndexManager` loading thread-safe.
- **Menu Bar Refactoring**: Eliminate menu duplicates, resolve shortcut conflicts, and rework wording across the menubar.
- **UI/UX Improvements**: Fix SpotBugs issues and improve general UI/UX.
- **JTree Event Handling**: Update `JTree` event handling to use selection path fallback and add debugging logs for data loading in tree panels.
- **ArticleTreePanel Decoupling**: Decouple `ArticleTreePanel` from `DataTreePanel` to improve architecture.
- **Weapon Sprite Center**: Simplify weapon sprite center calculation logic.
- **Code Cleanliness**: Resolve IDE warnings regarding nullability, unused imports, and method references. Change `TRACE_ENABLED` visibility to package-private. Cleanup root scratch files, remove debug logs, fix dialog initialization, and correct path joining.
- **Template Method Tree Architecture**: Centralize the `CompletableFuture.supplyAsync` background threading, `batchGeneration` concurrency guard, and single-atomic-EDT-update pattern into `DataTreePanel.reload()`. Subclasses (`HullsTreePanel`, `WeaponsTreePanel`, `ProjectilesTreePanel`, `CSVDataTreePanel`) now only implement `buildTreeNodesBackground()`. Removed ~200 lines of duplicated lazy-loading infrastructure (`lazyLoadMap`, `TreeWillExpandListener`) across 7 files. (`f29ac9a0`)
- **Keystroke EventBus Migration**: Remove legacy `InputMap`/`ActionMap` key bindings from `LayerViewerControls` and `WeaponSlotList`. Subscribe to `ViewerRawKeyPressed` for unified hotkey handling via the event bus. (`614b0c5a`)
- **Dead Code Removal**: Delete unused `HullTreeEntryCleared` event record from `FileEvents.java` and its orphaned listener in `HullsTreePanel`. Remove auto-expanding side panel behavior from `TripleSplitContainer` that forcibly resized the left pane on entity selection. (`1e163f18`, `2eaa3df6`)
- **Single-Pass JSON Processor**: Rewrote `JsonProcessor` to use a single-pass O(N) linear sweep tokenizer, completely eliminating the regex engine and intermediate `String` allocations, reducing peak parsing memory footprint by ~65-75%. Improve parsing robustness with malformed character cleanup utilities.
- **Database & Data Indexing Pipeline**:
  - Remove MD5 file hashing from `IndexScannerTask` in favor of fast modification timestamp checking.
  - Refactor `IndexScannerTask` to use per-file `try-catch` blocks, preventing a single unreadable/locked file from aborting the entire mod's database transaction.
  - Optimize file change detection using extension whitelist pre-checks and streamlined background indexing.
  - Implement Phase 3 and Phase 4 of the Data Pipeline Rework, eradicating legacy eager-loading caches and introducing `IndexedFile` lazy loading for all data tree panels.
  - Offload `starsector-core` indexing to a persistent in-memory `CoreIndexManager` to decouple core assets from the SQLite database, using O(1) lookups and composite indexes.
  - Parallelize file parsing operations to vastly speed up metadata extraction.
  - Centralize mod activation logic and improve database initialization and data resetting flows.
  - Remove `loadAllCsvEntries` and replace with iterative population from package-based maps, optimize CSV caching logic, and include core folders in CSV entry resolution.
  - Standardize core package name and filter MagicLib files during processing.
- **Threading & EDT Responsiveness**: Move data loading state mutations off the Swing Event Dispatch Thread (EDT) to eliminate progress bar freezes, optimize thread synchronization, replace `Collections.synchronizedList` lock contention in parallel streams with collector aggregation, and add volatility to repository maps.
- **Input Handling & Navigation**:
  - Migrate `KeyEventDispatcher` to an `EventBus`-based input handling architecture.
  - Reorganize main menu bar, add support for layer creation and ship flipping, and remove redundant `WindowMenu`.
- **UI Filtering & Controls**:
  - Replace abstract filter panel with `JComboBox`-based selection for ship tech/manufacturer and size in `ShipFilterPanel`.
  - Remove manual filter application buttons in favor of real-time instant filtering. Refactor filter panels to use SQLite metadata and direct CSV entry lookups instead of legacy in-memory JSON parsing.
  - Expose lazy weapon type retrieval to optimize tree rendering performance and simplify `InfoConsolePanel` layout by removing redundant `JScrollPane` wrappers.
  - Update data tree item interaction behavior to double-click loading.
  - Prevent filter dropdown change events during UI updates and revert experimental `CSVDataTreePanel` refactoring in favor of stabilized implementation.
  - Remove redundant tab mnemonics and improve tree table edit behavior on focus loss.
  - Improve layout management for variant panels.
  - Simplify `UndoOverseer` static access and improve SpotBugs compliance and locale-safe string operations.
- **Rendering & Visuals**:
  - Remove bake centerline functionality from sprite printing utilities and UI.
  - Remove guide axes overlay from ship viewer.
  - Remove redundant affine transformations from weapon painter position calculations.
  - Optimize projectile rendering using instance-based painting, standardize missile render logic, and encapsulate `LayerPainter` fields.

### Bug Fixes & Null Safety
- **Serialization Robustness**: Fix critical hull serialization crash (`Engine misconfiguration at hull serialization`) caused by strict engine style lookups. Ensure `SaveHullAction` safely writes raw `styleID` strings back to `.ship` files even when custom styles are unindexed or missing from memory.
- **Weapon UI Polish**: Enhance weapon instrument panels with refined dimensions, improved button disabling states, descriptive tooltips, and a "Clear Filters" button in the tree view.
- **Slot Creation Workflow**: Refactor the slot creation tab into a standalone pop-up modal (`SlotCreationDialog`) for configuring default weapon slot values, accessible via the slot panel and the `Edit` menu.
- **Double-Click & Drag-and-Drop Loading**: Fix broken double-click layer loading by switching from `mouseClicked` to `mousePressed` in `JTree` listeners. Fix drag-and-drop by unwrapping `IndexedFile` in `TreeDataGestureListener`. (`474fbc8f`)
- **Weapon Spec Null Safety**: Add null-safety guards in `WeaponCSVEntry.getSpecFile()` and `getLazyType()` to prevent NPEs when weapon spec files are missing or unparseable. (`ced1d1c8`)
- **Automated Tree Reloading**: Add `ComponentListener` to `DataTreePanel` that triggers `queueReload()` when the panel first becomes visible, ensuring trees populate without manual user action. (`ced1d1c8`)
- **Weapon-to-Slot Compatibility**: Validate weapon-to-slot size compatibility during variant loading and update system weapon size logic.
- **Type Safety & Exception Handling**: Strengthen type safety in tree cell renderers and fix `EventBus` subscription leaks. Add null checks to prevent NPEs in data tree sorting, repository updates, and `TwinSpinnerPanel`.
- **Error Handling**: Add error handling for weapon offset inputs and tree cell rendering errors. Include `starsector-core` in validation checks, fix forced reindexing logic, and prevent adding invalid mod directories when `mod_info.json` is missing.
- **Null Safety & Concurrency**: Fix silent data loading crash by preventing `null` entity IDs in `CoreIndexManager`'s concurrent maps, and add missing `synchronized` blocks to `loadCoreData()` to prevent race conditions during parallel execution.
- **Variant Loading**: Fix empty variants tab by removing premature constructor initialization in `GameDataRepository` that bypassed lazy loading, and resolve `null` collection iteration crashes in `ShipVariant` by adding Jackson collection null-guards. Add a fallback in `JsonSpecLoader` to use filenames for variants missing the `variantId` JSON field.
- **Data Deduplication**: Prevent ship data duplication in data trees by explicitly excluding `starsector-core` from active mod database queries in `DatabaseQueryService`.

### Build, Testing & Documentation
- **JSON Database Tests**: Add JSON database tests and resolve test suite flakiness.
- **Drag-and-Drop Testing**: Implement drag-and-drop and context menu testing in `MouseEventTestUI`.
- **Documentation Automation**: Automate documentation deployment by adding Maven resource synchronization and updating help file content. Configure documentation submodule.
- **Logging**: Improve log4j2 console formatting.
- **Unit Testing**: Add unit tests for data panel instantiation and ensure layout integrity. Add property-based testing suites using `jqwik` for utility, parsing, serialization modules, data repository validation, `UndoOverseer`, `ShapeUtilities`, and `ColorUtilities`. Remove unused CSV loading tests.
- **Mutation Testing**: Add PIT mutation testing support.
- **Test Infrastructure**: Expose `fetchWorker` to allow synchronous awaiting of mod loading states in integration tests. Add diagnostic load testing and debugging utilities.
- **Build Output**: Configure build to output an executable JAR directly to the project root directory.
- **Documentation**: Simplify installation instructions prioritizing zero-config setup using Starsector's bundled JRE. Add comprehensive Javadoc comments across core system interfaces.

## [0.0.1f] - 2026-08-02

### Features
- **File Hashing**: Add MD5 file hashing to `IndexedFile` and optimize index scanning by skipping re-parsing for content-identical files.
- **Module Anchoring**: Set default module anchor position to the centroid of ship boundary points.
- **Testing Support**: Add `ColorArrayRGBASerializer` to `SkinSpecFile` and enable coversColor round-trip testing.
- **Weapon Recoil**: Implement per-barrel weapon recoil rendering with support for individual offset points.
- **Weapon UI Integration**: Move weapon visuals panel into the weapons instruments tabbed view and remove redundant event handling.
- **Module Features**: Register and activate new layer upon loading module feature.
- **Module Features**: Implement rotation transformation for ship points and update module anchoring logic while adding fallback variant retrieval for features.
- **Module Features**: Highlight station module features when their associated weapon slot is selected.
- **Variant Panels**: Add support for empty slot visualization and module sprite display in variant panels.
- **Real-time Search**: Implement debounced real-time searching in data trees and remove redundant search buttons.
- **Rendering**: Implement partial circle rendering and update weapon slot geometry to honor rotation angles.
- **Skin Overrides**: Allow null values for ship system and hull style in ShipSkin builder.
- **Weapon Slots**: Implement copy-paste functionality for weapon slots using keyboard shortcuts and context menus.
- **Skin Overrides**: Implement skin engine and removal override panels with corresponding undo/redo support.
- **Game Folder Detection**: Improve game folder detection logic by checking for `mod_info.json` and add verification tests.

### Bug Fixes
- **Rendering**: Fixed jitter and stuttering in ship and shield center points during camera panning by rounding screen coordinates.
- **Rendering**: Enhanced zoom input with precise trackpad support and smooth logarithmic interpolation.
- **Weapon Mirroring**: Fixed an issue where perfectly centered single weapon slots would mistakenly identify themselves as their own mirrored counterparts, causing angle rotation bugs.
- **Documentation**: Updated README with instructions to compile and run the editor directly from source using Maven.
- **Linux Compatibility**: Improve Linux rendering compatibility by adding AWT and DirectDraw JVM flags.
- **Null Safety**: Allow null values for pad parameter in `ArticleTextBlock` constructor with default fallback to zero.
- **Linux Compatibility**: Improve Linux compatibility by adjusting rendering flags, window bounds, startup error handling, and documentation.
- **Null Safety**: Resolve null pointer safety, optimize path logic, refactor document listener, and clean up exception handling.
- **Linux Compatibility**: Added a troubleshooting guide to the README addressing GLX rendering crashes and black workspaces on Wayland display servers (Arch, Garuda, Fedora).
- **Database Caching**: Fixed a critical bug in `IndexScannerTask` where external file modifications were ignored during startup due to missing timestamp checks, ensuring the SQLite database cache correctly invalidates and prompts for updates.

### Security
- **Dependencies**: Upgrade `jackson-databind` & `jackson-core` to `2.18.9`, `log4j` to `2.25.3`, `sqlite-jdbc` to `3.49.1.0`, `flatlaf` to `3.5.4`, and `spotbugs-annotations` to `4.8.6` to resolve Dependabot security advisories.

### Refactoring & Performance
- **Build & Documentation**: Migrate build documentation to `BUILD.md` and update Maven output directory configuration.
- **Release Automation**: Migrate release automation script from Python to Java.
- **Code Modernization**: Replace method references with lambda expressions for consistent functional interface usage.
- **Code Modernization**: Remove redundant null suppression annotations across codebase.
- **Code Modernization**: Suppress null warnings and replace method references with lambda expressions throughout the project.
- **UI & Navigation**: Update JSON deserializers for robustness, improve `ProjectilesTreePanel` layout, and increase scroll bar unit increment in `WeaponOffsetsPanel`.
- **UI & Navigation**: Migrate reference data panel to a floating window and integrate wing/visuals tabs into the main game data panel.
- **UI & Navigation**: Replace `GameDataReferenceWindow` with an integrated tab in `GameDataPanel` to improve navigation and workflow uniformity.
- **UI & Navigation**: Change `HelpArticle` and `ArticleTreePanel` access modifiers to public.
- **Utilities**: Encapsulate `DocumentListener` logic into a reusable class and modernize path-to-string conversion utilities.
- **Point Alignment**: Improve point alignment, linkage logic, and simplify `WeaponSlotList` usage.

### Chores, Docs & Build
- **Repository Guidelines**: Add `AGENTS.md` guidelines for using GitHub CLI in workspace operations.
- **CI & Maintenance**: Add Dependabot configuration (`.github/dependabot.yml`).
- **JVM Flags**: Add Java AWT and DirectDraw compatibility flags to JVM_OPTS.
- **Documentation**: Clarify X11 display environment variable usage for IDE previewers versus physical monitors.
- **Dependencies**: Upgrade lwjgl-bom to version 3.4.0 and lwjgl3-awt to version 0.2.4.
- **Documentation**: Document coordinate system differences and rotation requirements for OpenGL porting.
- **Cleanup**: Remove obsolete refactoring scripts and temporary test files.

## [0.0.1e-hotfix] - 2026-06-17

### Features & Testing
- **Data Directory**: Make `GAME_DATA_DIR` dynamic by loading the game folder path from `ship_editor_settings.json` rather than using a hardcoded path.
- **Property-based Testing**: Integrated `jqwik` property-based tests in `DataLoadingPropertiesTest` to fuzz data loaders and verify null safety of ID-resolution pipelines under 1,000 iterations each.

### Bug Fixes
- **Data Loading NPEs**: Implemented defensive null validation for `dbFile.getEntityId()` and deserialized Variant/Projectile/Weapon IDs inside parallel loading streams (`LoadShipDataAction` and `LoadWeaponsDataAction`), resolving startup hangs (infinite blue progress bar) caused by malformed or empty database cache entries.
- **Spec Cache Registration**: Fixed cache population by ensuring newly parsed/loaded hull and skin specs from the database cache are registered via `GameDataRepository.putSpec()`.
- **Lombok Shadowing**: Replaced direct field accesses on `anchor`, `spriteOpacity`, `selected`, and `paintOpacity` in base painter classes (`LayerPainter` and `AbstractPointPainter`) with polymorphic getters (`this.get...()`) to resolve null reference errors in subclass extensions.
- **UI & Null Safety**: Handle null file paths gracefully in UI components.
- **Ignore AI Directories**: Ignore AI-related directories in git/file search.
- **Undo/Redo**: Fix undo/redo dirty marking logic.

### Refactoring & Performance
- **Rendering Optimization**: Optimize rendering calculations by using constant matrices.
- **Layer Selection**: Improve layer selection logic in the viewer.

### Chores
- **Dependencies**: Upgrade `lwjgl-bom` to version 3.3.4.

## [0.0.1e] - 2026-06-11

### Features
- **Variant Data**: Implement new UI components and controllers for variant data management and tree navigation. Decoupled the monolithic `DataTreePanel` and `VariantMainPanel` into smaller controllers and builders (`VariantChooserPanel`, `VariantOrdnancePanel`, `DataTreeContextMenuController`, `DataTreeSearchController`, `DataTreeTableBuilder`, `DataTreeVariantPanelBuilder`, `WeaponTreeContextMenuController`, `WeaponsTreeCellRenderer`).
- **UI Enhancements**: Display installable slot compatibility for weapons in the `WeaponsTreePanel`. Improved tree panel interactivity with double-click loading, updated context menu layouts, standardized search field handling, and search debouncing. Consolidated toolbar buttons into a hover-activated dropdown menu.
- **Data Caching & Filters**: Implement persistent parsed data caching in SQLite database using a new `parsed_data` column on the `indexed_files` table, speeding up subsequent startups by bypassing filesystem reads. Updated filter UI to require a manual "Apply filters" button click. Added `TestDB.java` for SQLite DB record verification.
- **CSV & Enum Hardening**: Added try-catch blocks to silently ignore unrecognized values from unofficial mods for several enums (`ShipTypeHints`, `WeaponRenderHints`, `HullSize`, `FireMode`, `WeaponMount`, `WeaponSize`, `WeaponType`), defaulting to fallback values. Added custom table cell editors (comboxboxes/dropdowns) for CSV data properties (`shield type`, `tech/manufacturer`, `formation`, `role`, `type`, `system id`) and improved parsing robustness for colors.
- **Launch Bays**: Rewrote coordinate translation logic in `LaunchBayPainter` to translate world coordinates into screen coordinates, bypassing affine transform scaling and zoom bugs. Implemented a direct raw mouse event listener for selection interactions. Updated `LaunchPortPoint` selection highlight color to orange.
- **Agent Skills**: Formally structured the `starsector-architecture` skill directory. Added reference examples for EventBus subscriptions, templates for event creation, and improved the `find_leaking_subscribers.sh` regex pattern.

### Bug Fixes
- **Race Conditions & Initialization**: Resolve data loading race conditions and variant initialization errors:
  - Set loading flags directly in `DataLoadingAction` subclasses instead of relying on UI panel visibility.
  - Remove hard-abort guard in `ShipPainter.installVariant()` that blocked variant selection.
  - Replace blocking `JOptionPane` popup spam in `ShipVariant.initialize()` with `log.error()` for missing weapons, hullmods, and wings, allowing variants to load with whatever data is available.
  - Remove false `isShipDataLoaded()` dependency in `WingsTreePanel.populateEntries()`.
  - Add null-safe color fallbacks in `Themes.java` for headless mode.
  - Add null-safe fallbacks in `ComponentUtilities.createIconFromImage()`, `ImageCache.loadImage()`, and `WingCSVEntry.retrieveSpec()` to prevent NPEs during icon loading and sprite resolution.
- **Concurrency & Thread Safety**: Diagnosed and patched `HashMap` corruption across the repository and `EventBus`:
  - Wrapped `EventBus` map compound operations (`computeIfAbsent` and `remove` on `lifecycleSubscribers`) in explicit `synchronized` blocks.
  - Converted in-memory repository lookup maps to thread-safe `ConcurrentHashMap` instances and declared them `volatile`.
  - Replaced dual-cache lookup maps with a single atomic `csvCacheByPath` map storing `CachedCSVData` records.
  - Implemented double-buffering for loading repository maps by instantiating new map instances in background threads and swapping references on the EDT.

### Refactoring & Performance
- **Codebase Cleanup**: Pruned unused methods and stripped out unused imports (`AffineTransform`, `SkinSpecFile`, `FileLoading`, `BusEvent`) across representation objects and UI panels. Deleted unused `DynamicMenuListener.java`. Removed `module-info.java` to migrate to classpath-based dependency management. Modularized `WeaponFirePanel` into specialized handler classes to improve maintainability. Added Jackson polymorphic deserialization to `ArticlePart` and improved `JsonProcessor` robustness. Implemented async icon loading with caching for CSV entries (`HullmodCSVEntry`, `ShipCSVEntry`, `WingCSVEntry`) to prevent UI blocking during tree panel rendering.
- **Event & Enum Consolidation**: 
  - Consolidated over 40 granular communication events into nested records inside category-based classes (`LayerEvents.java`, `PointEvents.java`, `ComponentEvents.java`, `ControlEvents.java`, `FileEvents.java`).
  - Merged separate serializer and deserializer classes into `CustomSerializers.java` and `CustomDeserializers.java`.
  - Consolidated representation enums into `RepresentationEnums.java` and `WeaponEnums.java`, and utility enums into `UtilityEnums.java`.
- **UI & Menu Reorganization**: Rewrote the UI layout for `AbstractFilterPanel` shifting from a flat layout to a `JTabbedPane` for distinct filter categories. Replaced legacy `VariantDataPanel` with specific sub-panels in the UI routing and added FlatLaf `JTabbedPane` custom client properties for the ship instrument tabs. Reorganized menus to include a Data menu, moving preferences and reset options to the File menu. Moved ship and weapon filter panels into dedicated tabs within the `GameDataPanel`, removing them from the menu bar and toolbar. Standardized `JFileChooser` parent windows. Consolidated secondary data panels (Hullmods, Shipsystems, Wings, Projectiles, and styles) into a dropdown selector within a unified 'Data' tab inside the `GameDataPanel`. Implemented utility helper `wrapTextWithHtml` to automatically wrap long tab titles in `ViewerLayersPanel` and multi-line tooltips.
- **Data Loading Robustness**: Reordered the execution of data loading actions in `FileLoading.java` so `loadWeapons` and `loadHullmods` finish before `loadShips`. Configured the SpotBugs Maven plugin via `spotbugs-exclude.xml` and fixed a system environment string comparison bug by explicitly using `Locale.ROOT`. Swapped CSV loading fallback order from UTF-8 to ISO-8859-1 for better compatibility.
- **CSV Serialization**: Detailed the character-by-character comment stripping algorithm designed to avoid catastrophic regex backtracking.
- **Developer Logging**: Introduced a `developerMode` settings option and wrapped trace/verbose loader log statements to prevent output pollution in production.

### Chores, Docs & Build
- **Documentation**: Bootstrapped initial technical documents outlining the database schema and Jackson processor logic. Massive restructuring of legacy documentation by migrating standard markdown files from the `doc/` directory into dedicated skill directories with YAML frontmatter. Added strict guidelines on thread safety, volatile collection visibility, and EventBus map synchronization to the architecture skill. Documented all findings in `known-issues.md`.
- **Testing**: Added `jqwik` and `junit-jupiter` dependencies to `pom.xml` for property-based testing. Wrote `CliLoadingTest.java` and `DatabaseLoadingIntegrationTest.java` for headless data pipeline verification. Added integration tests for parsing Starsector game files.
- **Release Tooling**: Created an interactive GUI-based release script (`scripts/release.py`) that automates version bumping in source files, prompts for `CHANGELOG.md` entries, builds the application, and handles Git tagging.
- **Build**: Increased JVM memory limit to 4GB.


## [0.0.1d] - 2026-06-11

### Features
- **First-Time Setup & Reset**: Added `FirstTimeSetupDialog` for game directory selection with `/run/media` auto-detection, and provided an option to clear app data via the Tools menu.
- **Weapon Configuration & Animation**: Added hidden mount support, introduced weapon fire, visual, and beam configuration panels. Added playback and stepping controls for weapon animations.
- **Visual Recoil**: Implemented weapon visual recoil animation and synchronized projectile offsets to weapon spec file during save.
- **IDE-style Upper Menu Redesign**: Consolidated settings and application menus into a new Preferences Dialog. 
- **Global Toolbar**: Introduced a unified main toolbar for quick access to Undo, Redo, Add/Remove Layers, Filters, and Game Data reloading.
- **Improved Hover Effects**: Reworked toolbar button styling with full hover support (highlight colors, hand cursor, robust FlatLaf integration).
- **OpenGL Rendering Migration**: Migrate the rendering system from Java2D/AWT to a custom high-performance OpenGL implementation using `SpriteRenderer`, `ShapeRenderer`, and `TextRenderer`.
- **Weapon and Projectile Specification Saving**: Implement saving functionality for weapon and projectile specification files with dedicated UI actions and coordinators.
- **Weapon and Projectile Data Panels**: Implement modular weapon and projectile data panels.
- **Menu and Filter Additions**: Add filter and data menus, implement ship filtering, and simplify panel layouts.
- **Ship Filter Enhancements**: Updated ship filtering logic to comprehensively search through skin names and file paths, improving asset discoverability.

### Bug Fixes
- **EventBus**: Implemented recursive depth support for EventBus dispatching to handle nested event calls safely.
- **Null Safety**: Improved null safety and optimized object cloning across multiple control models.
- **ShapeRenderer Mismatch**: Fix `ShapeRenderer` begin/end mismatch in `GuidesPainters`.
- **Instrument Panel Sync**: Improve instrument panel synchronization by adding null checks, active layer updates, and event bus lifecycle management.
- **Rendering Stabilization**: Improve rendering synchronization, stabilize slot scaling, and resolve UI pop-up issues via Java2D property adjustments.
- **UI Stability**: Improve UI stability by staggering load tasks, pre-initializing tooltips, and adding fallback logic to global error handling.

### Refactoring & Performance
- **Package Migration**: Massively migrated the project package namespace from `oth.shipeditor` to `shipeditor`.
- **Dependency Management**: Centralized dependency versions in `pom.xml`.
- **Layer & Utility Optimization**: Decoupled layer and module creation into a new `LayerFactory`, pre-allocated rendering vectors for performance, and improved utility bounds logic.
- **Undo Edit Consolidation**: Consolidated related undo edit classes into categorized managers and grouped files, and removed redundant package-info files.
- **Dialog & Repaint Optimization**: Centralized dialog management, optimized repaint scheduling, and cleaned up unused events and logic.
- **Weapon Animation Rework**: Moved weapon animation controls from the ship variant panel to the weapon visuals panel, supported multi-part weapon sprite animation frames, and later removed deprecated weapon animation support.
- **Cleanup**: Removed obsolete launcher scripts and purged emojis from CHANGELOG headers.
- **OpenGL Adoption**: Wide-spread migration of painter components to use GPU-accelerated drawing primitives.
- **EventBus Management**: Optimize EventBus subscription management.
- **UI Optimizations**: Implement deferred UI reloads for data tree panels using a queueing mechanism, standardize UI element scaling, update UI layouts with menu icons, and improve layer initialization logic.
- **Rendering Tweaks**: Implement manual zoom-based alpha scaling for painter text, reduce line widths and outline alpha. Optimize circle rendering and refine zoom speed and collision visualization. Introduce `FramebufferUtilities` for layer image printing. Improve CSV ID validation logic.
- **Database Initialization**: Replace external CLI database initialization with in-process indexing and add robust database validation checks.
- **Menu Architecture Overhaul**: Deleted numerous obsolete menu classes (`DataMenu`, `FilterMenu`, `LayersMenu`, `ApplicationMenu`, `SettingsMenu`) to massively streamline the top-level UI architecture and reduce codebase clutter.
- **Action Consolidation**: Centralized and relocated straggling utility actions (`JSON Corrector`, `Reset Transform`) into more cohesive namespaces (`ToolsMenu`, `WindowMenu`).
- **Context Menu Decoupling**: Updated `ViewerLayersPanel` to dynamically source right-click context menu options from `WindowMenu` instead of hardcoded legacy menu references, reducing inter-package dependencies.
- **Robust Toolbar UI Styling**: Implemented a reusable `styleToolbarButton` utility that correctly applies FlatLaf `toolBarButton` client properties, `HAND_CURSOR` overrides, and proper `setHideActionText(true)` encapsulation to prevent Swing layout disruption for action-backed buttons.
- **Camera Smoothness**: Reduced zoom interpolation speed in `LayerViewerControls` to provide significantly smoother camera transitions during canvas zoom operations.

### Chores, Docs & Build
- **OpenGL Architecture Guide**: Establish OpenGL rendering architecture, performance best practices, and robust state recovery patterns in documentation.


## [0.0.1c] - 2026-06-08

### Features
- **UI rework and Projectile Support** (v0.0.1c)
- **Data Loading & Validation**: Implement data loading progress, dirty state tracking, repository cache updates, and CSV validation.
- **Weapon UI**: Add weapon installation UI and context menu support; implement weapon offset and module UI updates; add pick weapon dialog.
- **Background File Indexing**: Implement SQLite-based background file indexing for Starsector assets with differential scanning and batch processing.
- **Mod Management**: Implement automatic purging of obsolete mods from database index and add validation for mod folder path resolution.
- **CSV Data Editing**: Implement synchronized CSV ID editing, automated re-indexing, and comprehensive CSV dataset save infrastructure for game data exports.
- **Hullmods & Modules**: Add suppressed hullmods management, module installation controls, and weapon offset editing instrument.
- **Skin Slot Overrides**: Implement skin slot overrides editor panel in `ShipInstrumentsPane`.
- **Tooltips**: Implement multi-line hover tooltips for editor points with custom detail formatting.
- **Startup**: Add splash screen for game data loading and implement library mod filtering to exclude non-data dependencies.
- **UI Enhancements**: Update UI tabs to use icons, adjust tab placement, and replace graphical icons with text labels.
- **Cross-Platform Scripts**: Add Windows launch script and improve cross-platform game folder detection.

### Bug Fixes
- **Parsing Robustness**: Improve robustness by handling and ignoring empty or contentless data files during parsing.
- **Null Checks**: Add defensive null checks to repository retrieval methods, file loading, and data processing components to prevent runtime exceptions.
- **UI Container**: Fix skin data panel UI container update after layout changes.

### Refactoring & Performance
- **EventBus Optimization**: Optimize EventBus dispatching with thread-local buffers and migrate EventBus subscriptions to include owner objects for improved lifecycle management.
- **Data Loading Perf**: Optimize data loading with parallel streams and implement mod folder caching; refactor JSON parsing to a linear scan to prevent regex engine stalls.
- **Architecture**: Reorganize project package structure, clean up event-driven architecture modules, and implement global `PrimaryWindow` instance.
- **Database Init**: Migrate database initialization to CLI-driven process and remove splash screen dependency.
- **UI Layout**: Remove `QuickButtonsPanel`, perform minor UI layout cleanup, standardize tooltips, and implement sprite-bounded cursor detection for point interaction and selection logic.
- **General Cleanups**: Remove unused imports, extract CSV validation logic, and clean up technical debt.

### Chores, Docs & Build
- **Java 21 Support**: Update Java 21 installation, setup, and build instructions in `README.md` (including Microsoft Build of OpenJDK).
- **Maven Configuration**: Update maven compiler configuration, add surefire plugin, and configure Maven Shade plugin for fat JAR distribution.
- **Dependencies**: Upgrade `log4j` and `jackson` dependencies to latest versions.
- **Cleanup**: Remove old comments, grievances, and design notes; update build file to exclude meta-inf manifest, licence, notice, and dependencies.
- **Warnings**: Suppress unstable module warnings in `module-info`.

## [0.0.1b]

### New Features & Editor Enhancements:
- **Synchronized CSV ID Editing and Automated Re-indexing**: Implemented automated synchronized CSV ID modifications for all five core entity types (ships, weapons, hullmods, fighter wings, and ship systems). When a user changes an asset's ID—either via the text field in the Hull Data control panel or by editing the "id" column directly in any CSV right-panel spreadsheet table—the application catches the change, updates the model, re-indexes the repository maps in `GameDataRepository` under the new ID, and modifies the raw cached CSV data row.
- **Save Prompts for CSV Updates**: Integrated a modal prompt (`JOptionPane.YES_NO_OPTION`) when a CSV ID is updated. The user is asked whether they want to save the modified CSV file to disk immediately, matching document-editor state behaviors where files are preserved until explicit user confirmation.
- **Detailed Multi-Line Tooltips**: Added rich, multi-line tooltips for editor canvas points. These tooltips format detail parameters, presenting precise coordinates and custom metadata on hover over active interactive points.
- **UI Tab Placements & Adjustments**: Restructured and optimized the main editor workspace tabs by reorganizing the tab components, adjusting layouts, and cleansing redundant UI build artifacts.

### Performance & Optimizations:
- **Workspace Layout Decluttering**: Removed the legacy `QuickButtonsPanel` from the UI layout, maximizing the screen real estate available for the main canvas, and optimized list handling during repository entry updates.

### Architecture, Bug Fixes & Refactoring:
- **Ikonli Icon Framework Purge**: Completely removed the heavy external Ikonli graphical icon dependency from the codebase, refactoring the Swing components, buttons, and tab headers to use lightweight, high-visibility native text-based labels. This speeds up compilation times, reduces final binary size, and removes UI rendering overhead.

### Dependencies:
- **Upgraded Log4j & Jackson Frameworks**: Upgraded the project's logging engine (`log4j`) and JSON/CSV processing libraries (`jackson`) to their latest stable releases, ensuring up-to-date security, enhanced serialization performance, and better resilience during robust data parsing.

## [0.0.1a]

### New Features & Editor Enhancements:
- **Weapon Offset Point Editing ("Offsets" Tab)**: Restored the placeholder offsets tab under `WeaponInstrumentsPane` with a new fully-featured `WeaponOffsetsPanel`. It features an interactive coordinates and angles spreadsheet table with direct addition and deletion of offset points, linked with the custom `WeaponOffsetPainter`.
- **Built-in Modules Installation UI**: Extended `ModuleControlPanel` to feature an intuitive dropdown/variant picker utilizing the available variants in `GameDataRepository`. Allows easy "Install" and "Clear" operations on module slots, fully hooked up to the Undo/Redo system (`EditDispatch`).
- **Suppressed Hullmods Management**: Added a new list editor panel (`SuppressedModsPanel`) inside the `VariantHullmodsPanel` for managing suppressed hullmod string IDs on active ship variants. Supports complete round-trip JSON serialization and deserialization via `SaveVariantAction` and `VariantFileSerializer`.
- **Skin Slot Overrides**: Designed and implemented the dedicated `SkinSlotOverridesPanel` within `ShipInstrumentsPane` to provide seamless editing of skin-specific weapon slot attribute overrides.
- **Project Authorship & Metadata Handover**: Updated authorship metadata across the codebase (including `Readme.txt` and `module-info.java`) to transition the primary maintainer status from `Ontheheavens` to `thevolkflower`. The application's `ApplicationMenu` Swing UI was also updated to accurately reflect the authors (`thevolkflower` & `Xenoargh`).
- **Development Milestone Completion**: Formally closed out the remaining development pipeline checklist items in `checklist.md`. All Tier 3 milestones (interactive Weapon Offsets point editing, Variant Wings UI, and Built-in Module slot handling) and Tier 4 UI cleanups are officially marked as completed and stable.
- **Swing Tooltip and Layout Standardizations**: Standardized blank or empty tooltips across panels to `null` to align with Swing guidelines and resolve rendering glitches, with minor UI cleanups in `AbstractSlotValuesPanel`, `VariantWingsPanel`, and cell renderers.

### Performance & Optimizations:
- **UI List Rebuild Prevention**: Instrument panels (Engines, Weapon Slots, Variant Modules) now manually compare their existing `ListModel` content against incoming data arrays before updating. If the data hasn't changed, the panels merely repaint instead of wastefully discarding and rebuilding the UI components. This dramatically reduces EDT overhead and prevents selection loss during frequent state updates.
- **Targeted Repaints via EventBus**: Replaced indiscriminate layer reselection during undo/redo actions (like sorting weapon groups or launch ports) with specific queued repaints. This eliminates broad, performance-heavy UI invalidation.
- **Repaint Timer Adjusted**: Lowered the global event scheduler's repaint frequency from ~125Hz (8ms) to ~60Hz (16ms) to save CPU cycles without sacrificing visual smoothness.

### Architecture, Bug Fixes & Refactoring:
- **UI Data Refresh Rendering Fixes**: Addressed a critical Swing rendering issue where dynamic panels would fail to visually update after state or layout changes. Added appropriate `.revalidate()` and `.repaint()` calls during data refreshes in multiple critical containers, including `EngineStylesPanel`, `HullStylesPanel`, `EngineDataPanel`, `ShipLayerInfoPanel`, `SkinDataPanel`, `VariantMainPanel`, and `VariantWeaponsPanel`.
- **EventBus Memory Leak Fix**: Migrated the core `EventBus` to a `WeakHashMap`-backed lifecycle architecture. Over 80 lambda expressions were refactored to bind strictly to component lifecycles, completely eliminating the historically documented listener pile-up.
- **Viewer Input Decoupling**: Dismantled the opinionated 1-to-1 event routing inside `LayerViewerControls`. It now dispatches generic raw mouse events (`ViewerRawMouseDragged`, etc.), allowing individual rendering painters to evaluate `ControlPredicates` internally and significantly improving the codebase's scalability for new UI interactions.
- **Unstable Module Warning Suppressions**: Added `@SuppressWarnings("module")` in `module-info.java` to suppress Java compiler warnings regarding unstable/filename-based automodules (such as `viewer-core`, `geom`, and `filters`), ensuring a clean Maven build process.
- **Technical Debt & Warning Sweep**: Conducted a thorough codebase sweep to eliminate redundant imports, remove obsolete `@SuppressWarnings("unused")` annotations, and resolve compiler/linter warnings across painters, models, and Swing panels.
- **Thread-safe Image Caching**: Upgraded the `ImageCache` backing map to a `ConcurrentHashMap`, preventing `ConcurrentModificationException` during multi-threaded file walking and sprite loading.
- **Robust CSV & Locale Handling**: Enforced a global US locale at startup to prevent comma/period decimal separator mismatches during parsing. Added utility methods for safe integer/double string parsing and improved CSV charset decoding robustness.
- **Instance-based Refactors**: Transitioned `WeaponFilterPanel` to an instance-based implementation, avoiding problematic static state.
- **Maven Shade Packaging & Executable Build**: Configured the `maven-shade-plugin` in `pom.xml` with `Log4j2PluginCacheFileTransformer` and services resources packaging. STANDALONE fat jar deployment is fully streamlined.
