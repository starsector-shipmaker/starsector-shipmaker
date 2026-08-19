# Building Ship-Editor from Source

If you wish to compile the project yourself, follow these instructions.

## Prerequisites
- **Java Development Kit (JDK)**: JDK 17.
- **Maven**: Ensure Maven is installed on your system.

## Compiling
To compile the project and generate the executable fat JAR, run:
```bash
mvn clean package -DskipTests
```
This builds the application and outputs the executable JAR file in the `target/` directory (e.g. `target/ship_editor-0.0.1f.jar`).

## Running Developer Build
To run the compiled JAR, execute:
```bash
java -jar target/ship_editor-*.jar
```

## Running from Source Code
To compile and run the application directly from source using Maven without building a JAR:
```bash
MAVEN_OPTS="-Xmx4g" mvn compile exec:java -Dexec.mainClass="shipeditor.Main"
```

## Managing Releases
To automate a new release locally without relying on GitHub:
```bash
python3 scripts/release.py
```
This script will:
1. Extract the current version from `pom.xml` and prompt you for the target release version.
2. Verify that `CHANGELOG.md` has an entry for the target version.
3. Automatically bump the version in `pom.xml` and Java source files (`Main.java`, `SettingsManager.java`).
4. Compile and package the application using Maven.
5. Create a standalone portable ZIP archive (e.g., `releases/ship-editor-0.0.1d.zip`) containing the fat JAR, launchers, `CHANGELOG.md`, `LICENSE`, and `README.md`.
6. Commit the version bump and create a local Git release tag (e.g., `v0.0.1d`).

**Available options:**
*   `--dry-run`: Performs compilation, packages the zip, but reverts all code modifications and skips Git operations. Useful for testing the release build.
*   `--no-git`: Bumps version and builds/packages, but skips Git commit and tag creation.
*   `--allow-dirty`: Allows running the script even if there are uncommitted changes in the working directory.
