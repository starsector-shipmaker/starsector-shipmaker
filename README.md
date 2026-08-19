# Ship-Editor 
**Developed and maintained by:** thevolkflower

Visualizer and editor of object data in JSON and CSV format. Developed as utility tool for the purposes of working with data files of a game Starsector.

Clean repository based on ontheheaven's original repo: https://github.com/Ontheheavens/Ship-Editor/


## Stack:

 - Java 17
 - Swing
 - Maven
 - Jackson
 - Lombok
 - Log4j2

## Used libraries:

 - JavaGL: https://github.com/javagl/Viewer
 - Ikonli: https://github.com/kordamp/ikonli
 - Flatlaf: https://github.com/JFormDesigner/FlatLaf

## Installation & Running (For Modders)

To run the Ship Editor, choose the setup option that best fits your workflow:

### 🌟 Option 1: Zero-Config Setup (No Java Installation Required — Recommended)
If you place the Ship Editor inside your **Starsector installation directory** (for example, in the main `Starsector` folder or inside `Starsector/mods/`), the launcher scripts will **automatically detect and use Starsector's built-in Java JRE**.

**Zero configuration or installation needed!**
1. Extract the Ship Editor release files into your Starsector installation directory or `mods` folder.
2. Launch using the startup script for your operating system:
   *   **Windows**: Double-click **`ship_editor.bat`**.
   *   **Linux / macOS**: Run **`./ship_editor.sh`** or **`ship_editor.command`**.

*The launcher script automatically finds Starsector's bundled Java (`jre`, `jre_linux`, or `jre_mac`) and launches immediately.*

---

### Option 2: System Java 17 (For Standalone Setup Outside Game Directory)
If you prefer running the editor in a custom folder outside the Starsector installation, install a **Java Runtime Environment (JRE) version 17**:

1. **Download JRE 17**:
   * **[Eclipse Temurin (Adoptium) Java 17 Releases](https://adoptium.net/temurin/releases/?version=17)** — Select **JRE** package type (`.msi` for Windows, `.pkg` for macOS).
   * **[Microsoft Build of OpenJDK 17](https://learn.microsoft.com/en-us/java/openjdk/download#openjdk-17)**.
2. During installation, make sure **"Add to PATH"** or **"Set JAVA_HOME"** is checked.
3. Run **`ship_editor.bat`** (Windows) or **`./ship_editor.sh`** (Linux/macOS).

---

### Option 3: Portable Local JRE Setup
If you want to run the editor outside the Starsector directory without installing Java system-wide:
1. Download a Java 17 JRE `.zip` (Windows) or `.tar.gz` (Linux/macOS) archive from [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=17).
2. Extract the archive directly into the application folder alongside `ship_editor.jar`.
3. Rename the extracted folder to **`jre`**.
4. Launch via **`ship_editor.bat`** or **`./ship_editor.sh`**, which will automatically detect and run from the local `jre` folder.

---

## Building from Source (For Developers)

For detailed instructions on compiling, running from source, and managing releases, please refer to the [BUILD.md](BUILD.md) file.

### Troubleshooting & Platform Notes
- **Crash Errors & Logs**: Should there be any crash errors, check the `log` folder, which will have a file with stack trace lines.
  > [!NOTE]
  > File loading failures that do not have a modal popup and appear exclusively in log lines are generally to be expected; they are usually the result of inputs that do not conform to the Starsector spec JSON layouts.
- **Linux Wayland Rendering Issues**: If you are running Linux (such as Arch, Garuda, or Fedora) with a Wayland session, the editor may launch with a black, unrendered workspace and "not initialized" side panels. This is caused by a `Failed to query GLX version` OpenGL crash on Wayland's default display server. To fix this, run the application in X11/XWayland mode by launching the script via terminal with the GDK backend variable set:
  ```bash
  GDK_BACKEND=x11 ./ship_editor.sh
  ```
  Alternatively, you may switch your desktop environment session from Wayland to X11/Xorg from your login screen.
- **macOS Startup Issues**: If the editor fails to launch on macOS, try the following steps (see [PR 52](https://github.com/Ontheheavens/Ship-Editor/pull/52)):
  1. Open a Terminal window.
  2. Type: `chmod +x ` (make sure to include the trailing space).
  3. Drag the `.command` file onto the Terminal window.
  4. Press `Enter` to execute.
