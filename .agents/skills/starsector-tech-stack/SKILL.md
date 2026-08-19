---
name: starsector-tech-stack
description: Information about the required technology stack, environment JVM flags, and build plugins.
---

# Starsector Ship Editor — Technology Stack

## Skill Directory Structure

This skill is organized as follows:
- **`SKILL.md`**: Main instructions (this file).
- **`resources/`**: Configurations and templates.
  - [maven-toolchains.xml](file:///media/lechibang/WORK1/projects/starsector-shipmaker/.agents/skills/starsector-tech-stack/resources/maven-toolchains.xml): Configuration showing how to bind Maven to Java 17/21 compiler paths.
- **`examples/`**: Code references.
  - [pom-compilation-plugins.xml](file:///media/lechibang/WORK1/projects/starsector-shipmaker/.agents/skills/starsector-tech-stack/examples/pom-compilation-plugins.xml): Reference Maven plugin config for Java compiler and Lombok annotation processor.
- **`scripts/`**: Tooling.
  - [check_java_home.sh](file:///media/lechibang/WORK1/projects/starsector-shipmaker/.agents/skills/starsector-tech-stack/scripts/check_java_home.sh): Script to verify environment compatibility.

## Core Language & Runtime

| Component | Version | Notes |
|---|---|---|
| **Java** | Source target 17 (`maven.compiler.release=17`) | Requires JDK 17–21 to compile. Lombok 1.18.36 crashes on JDK 25 with `TypeTag :: UNKNOWN ExceptionInInitializerError`. |
| **Maven** | 3.x | Build tool. **Fedora quirk**: Fedora's `mvn` wrapper ignores `update-alternatives` and reads `/etc/java/maven.conf`. Set `JAVA_HOME=/usr/lib/jvm/java-17-temurin-jdk` in `~/.mavenrc` to force Java 17. |

### JVM Flags (Production)
```
-Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication
-XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20
```
Configured via `exec-maven-plugin` for development and the launcher scripts for production.

### JVM Self-Relaunch (Quirk)
[Main.java](file:///media/lechibang/WORK1/projects/starsector-shipmaker/src/main/java/shipeditor/Main.java) contains `checkAndRelaunch()` which detects if the JVM was launched with >1.1 GB max heap (e.g., by a system default) and relaunches itself with `-Xmx1g` to prevent excessive memory usage. The relaunched process is marked with `-Dshipeditor.relaunched=true` to prevent infinite relaunch loops.

### Java2D Suppression
```java
System.setProperty("sun.java2d.opengl", "false");
System.setProperty("sun.java2d.d3d", "false");
System.setProperty("sun.java2d.noddraw", "true");
```
Java2D's OpenGL and Direct3D pipelines are explicitly disabled because they conflict with the LWJGL GL context sharing through `AWTGLCanvas`. Without these flags, some drivers produce black canvases or segfaults.

---

## GUI Framework

| Component | Version | Purpose |
|---|---|---|
| **Swing/AWT** | JDK built-in | Core UI framework |
| **FlatLaf** | 3.1.1 | Modern look-and-feel with dark mode, scalable DPI, and IntelliJ themes |
| **Ikonli** | 12.3.1 | Vector icon packs: FontAwesome5, FluentUI, Boxicons |

### FlatLaf Configuration (Quirks)
- `JPopupMenu.setDefaultLightWeightPopupEnabled(false)` — Required because lightweight popups render behind the `AWTGLCanvas` heavyweight component.
- `ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false)` — Same issue for tooltips.
- `UIManager.put("FileChooser.readOnly", true)` — Prevents users from creating/deleting folders in the file chooser.
- **Early JToolTip initialization**: `new JToolTip().updateUI()` is called during startup to force the classloader to eagerly load ToolTip UI classes. Without this, the first tooltip display on EDT can trigger lazy classloading that causes a visible UI freeze.
- **Shell Folder Filtering**: A custom function removes the `::` pseudo-path (`SHELL_FOLDER_0_X_12`) from the file chooser shortcuts on Windows.

---

## Graphics & Rendering Pipeline

| Component | Version | Purpose |
|---|---|---|
| **LWJGL 3** (BOM) | 3.3.3 | Core library, OpenGL bindings, GLFW, JAWT |
| **lwjgl3-awt** | 0.2.3 | `AWTGLCanvas` bridge between Swing and OpenGL |
| **JOML** | 1.10.8 | `Matrix4f`, `Vector2f`, `Vector3f`, `Vector4f` math |

### Native Classifiers
Both Linux and Windows natives are included in the POM:
- `natives-linux` for `lwjgl`, `lwjgl-opengl`, `lwjgl-glfw`
- `natives-windows` for the same

macOS is not supported (no native classifiers declared).

---

## Data Handling & Persistence

| Component | Version | Purpose |
|---|---|---|
| **Jackson Core** | 2.18.7 | JSON parsing with extensive relaxed-mode features |
| **Jackson Databind** | 2.18.7 | Object mapping with custom coercion rules |
| **Jackson CSV** | 2.18.7 | CSV parsing/writing with custom serializers |
| **Jackson Annotations** | 2.18.7 | `@JsonProperty`, `@JsonIgnoreProperties`, etc. |
| **SQLite JDBC** | 3.45.2.0 | Embedded database for file indexing |

---

## Utilities

| Component | Version | Purpose |
|---|---|---|
| **Lombok** | 1.18.36 | `@Getter`, `@Setter`, `@Builder`, `@Log4j2`, `@ToString` |
| **Log4j2** | 2.25.4 | `log4j-api`, `log4j-core`, `log4j-slf4j-impl` |
| **Apache Commons Collections 4** | 4.4 | Advanced collection utilities |
| **JHLabs Filters** | 2.0.235-1 | Image manipulation filters |

### Lombok Inheritance Trap (Quirk)
When extending classes that use Lombok `@Getter` (e.g., `LayerPainter`), be careful of field shadowing. Always use polymorphic getter methods (`getSprite()`) in the base class rather than direct field access (`this.sprite`). Subclasses that override the getter will function correctly; direct field access bypasses the override and hits uninitialized base-class fields, causing `NullPointerException`s.

---

## Build Plugins

| Plugin | Version | Purpose |
|---|---|---|
| `maven-compiler-plugin` | 3.11.0 | Java 17 target, Lombok + Log4j2 annotation processors |
| `maven-surefire-plugin` | 3.2.5 | Test execution (JUnit Jupiter + jqwik) |
| `spotbugs-maven-plugin` | 4.8.3.1 | Static analysis: Max effort, Low threshold |
| `maven-jar-plugin` | 3.4.2 | JAR packaging |
| `exec-maven-plugin` | 3.1.0 | Launch via `mvn exec:exec` with custom JVM flags |
| `maven-shade-plugin` | 3.6.0 | Uber-JAR generation |

### Shade Plugin Configuration (Quirks)
- **`Log4j2PluginCacheFileTransformer`**: Merges Log4j2 plugin caches from multiple JARs. Without this, Log4j2 fails to discover its appenders at runtime.
- **`ServicesResourceTransformer`**: Merges `META-INF/services` files. Required for JDBC driver auto-discovery and Ikonli icon pack loading.
- **Exclusion Filters**: Strips `module-info.class`, signature files (`*.SF`, `*.DSA`, `*.RSA`), and duplicate metadata to prevent `SecurityException` and classloading conflicts.
- **Output**: The shaded JAR is written to `${project.basedir}/ship_editor.jar` (project root), not `target/`.

---

## Module System (JPMS)

[module-info.java](file:///media/lechibang/WORK1/projects/starsector-shipmaker/src/main/java/module-info.java) declares a full JPMS module named `shipeditor`.

### Key Observations
- **`requires static lombok`**: Lombok is compile-time only (no runtime dependency).
- **`requires static com.github.spotbugs.annotations`**: SpotBugs annotations are retained in source but not needed at runtime.
- **Extensive `opens` directives**: Many packages are opened to `com.fasterxml.jackson.databind` for reflection-based deserialization. Without these, Jackson fails with `InaccessibleObjectException` on Java 17+.
- **`requires transitive java.desktop`**: Exposes AWT/Swing to downstream modules.
- **Compiler warnings suppressed**: `@SuppressWarnings("module")` on the module declaration, and `-Xlint:-requires-automatic -Xlint:-module` compiler args suppress warnings about automatic modules (LWJGL, Ikonli, etc. that lack proper module-info).
- **IDE Access Diagnostics**: Be sure to `exports` any internal packages (like `shipeditor.utility.graphics.opengl`) and use `requires transitive` for external libraries (like `org.joml`) if their classes are exposed in public method signatures. Without these, strict IDEs (like Eclipse/VSCode) will mass-report "not exported from this module" or "may not be accessible to clients" errors even if Maven compiles fine.
