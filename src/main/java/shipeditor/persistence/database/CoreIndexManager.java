package shipeditor.persistence.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import shipeditor.parsing.FileUtilities;
import shipeditor.parsing.loading.IndexScannerTask;
import shipeditor.persistence.SettingsManager;
import shipeditor.utility.text.StringConstants;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages the in-memory index of starsector-core files.
 * Guarantees that the core files are always loaded regardless of SQLite database status.
 */
@Log4j2
public class CoreIndexManager {
    private static final Map<String, List<IndexedFile>> coreFilesByType = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, IndexedFile>> coreFilesByEntityId = new ConcurrentHashMap<>();
    private static final Map<String, IndexedFile> coreFilesByPath = new ConcurrentHashMap<>();
    private static final Object LOCK = new Object();
    private static volatile boolean isLoaded = false;

    public static void reset() {
        synchronized (LOCK) {
            coreFilesByType.clear();
            coreFilesByEntityId.clear();
            coreFilesByPath.clear();
            isLoaded = false;
        }
    }

    public static void loadCoreData() {
        synchronized (LOCK) {
            if (isLoaded) return;
            if (SettingsManager.getSettings() == null) {
                log.warn("Settings not initialized. Cannot load core index.");
                return;
            }
        
        Path coreFolder = SettingsManager.getCoreFolderPath();
        if (coreFolder == null || !Files.exists(coreFolder)) {
            log.warn("Core folder path is null or does not exist. Cannot load core index.");
            return;
        }

            coreFilesByType.clear();
            coreFilesByEntityId.clear();
            coreFilesByPath.clear();

            long startTime = System.currentTimeMillis();

            // Check if we can load from the database instead of re-scanning.
            if (tryLoadFromDatabase(coreFolder)) {
                isLoaded = true;
                log.info("Core memory index loaded from database cache in {}ms", System.currentTimeMillis() - startTime);
                return;
            }

        log.info("Scanning starsector-core into memory index...");

        Map<String, String> extensions = Map.of(
                "ship", StringConstants.SHIP_TYPE,
                "skin", StringConstants.SKIN_TYPE,
                "wpn", StringConstants.WEAPON_TYPE,
                "variant", StringConstants.VARIANT_TYPE,
                "proj", StringConstants.PROJECTILE_TYPE
        );

        // ObjectMapper is thread-safe for read operations.
        ObjectMapper mapper = FileUtilities.getConfigured();
        Map<String, List<File>> allFiles = IndexScannerTask.fetchFilesWithExtensions(coreFolder, extensions.keySet());

        List<IndexedFile> allIndexedFiles = new ArrayList<>();

        for (Map.Entry<String, String> entry : extensions.entrySet()) {
            String ext = entry.getKey();
            String type = entry.getValue();
            List<File> files = allFiles.getOrDefault(ext, Collections.emptyList());

            List<IndexedFile> typeList = files.parallelStream()
                    .map(file -> {
                        try {
                            IndexScannerTask.EntityMetadata metadata = IndexScannerTask.extractEntityMetadata(file, type, mapper);
                            String entityId = metadata.id();
                            String entityName = file.getName().replace("." + ext, "");

                            return IndexedFile.builder()
                                    .uuid(UUID.randomUUID())
                                    .modId("starsector-core")
                                    .entityId(entityId != null ? entityId : entityName)
                                    .entityName(entityName)
                                    .entityType(type)
                                    .fileName(file.getName())
                                    .filePath(file.toPath())
                                    .lastModified(file.lastModified())
                                    .spritePath(metadata.spritePath())
                                    .designation(metadata.designation())
                                    .build();
                        } catch (Exception e) {
                            log.error("Failed to parse core file for memory index: {}", file, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            coreFilesByType.put(type, typeList);
            allIndexedFiles.addAll(typeList);

            Map<String, IndexedFile> entityIdMap = new ConcurrentHashMap<>();
            for (IndexedFile file : typeList) {
                entityIdMap.put(file.getEntityId(), file);
                coreFilesByPath.put(file.getFilePath().toString(), file);
            }
            coreFilesByEntityId.put(type, entityIdMap);
        }

        // Persist to database for faster subsequent startups.
        persistToDatabase(coreFolder, allIndexedFiles);

        isLoaded = true;
        log.info("Core memory index loaded in {}ms", System.currentTimeMillis() - startTime);
        }
    }

    private static boolean tryLoadFromDatabase(Path coreFolder) {
        try {
            shipeditor.persistence.database.DatabaseQueryService.ModInfo modInfo =
                    shipeditor.persistence.database.DatabaseQueryService.getScannedModsMap().get("starsector-core");
            if (modInfo == null) {
                return false;
            }

            // Check if any relevant file in the core folder has been modified since the last scan.
            long dbLastScanned = modInfo.lastScanned();
            Path dataPath = coreFolder.resolve("data");
            Path walkTarget = Files.exists(dataPath) && Files.isDirectory(dataPath) ? dataPath : coreFolder;
            try (java.util.stream.Stream<Path> pathStream = Files.walk(walkTarget)) {
                boolean hasNewerFile = pathStream.anyMatch(path -> {
                    if (!Files.isRegularFile(path)) return false;
                    Path fileNamePath = path.getFileName();
                    if (fileNamePath == null) return false;
                    String fileName = fileNamePath.toString();
                    int dotIndex = fileName.lastIndexOf('.');
                    if (dotIndex > 0) {
                        String ext = fileName.substring(dotIndex + 1).toLowerCase(java.util.Locale.ROOT);
                        if (Set.of("ship", "skin", "wpn", "variant", "proj").contains(ext)) {
                            return path.toFile().lastModified() > dbLastScanned;
                        }
                    }
                    return false;
                });
                if (hasNewerFile) {
                    return false;
                }
            }

            // Load from database: query indexed_files for mod_id = 'starsector-core'.
            List<IndexedFile> allCoreFiles = shipeditor.persistence.database.DatabaseQueryService.getFilesByModId("starsector-core");
            if (allCoreFiles.isEmpty()) {
                return false;
            }

            for (IndexedFile file : allCoreFiles) {
                String type = file.getEntityType();
                coreFilesByType.computeIfAbsent(type, k -> new ArrayList<>()).add(file);
                
                String entityId = file.getEntityId();
                if (entityId != null) {
                    coreFilesByEntityId.computeIfAbsent(type, k -> new ConcurrentHashMap<>()).put(entityId, file);
                }
                
                coreFilesByPath.put(file.getFilePath().toString(), file);
            }
            return true;
        } catch (Exception e) {
            log.warn("Failed to load core index from database, will re-scan.", e);
            return false;
        }
    }

    private static void persistToDatabase(Path coreFolder, List<IndexedFile> files) {
        try {
            // Delete existing records to prevent UNIQUE constraint failures on file_path since we generate new UUIDs
            try (java.sql.Connection conn = shipeditor.persistence.database.DatabaseManager.getConnection();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement("DELETE FROM indexed_files WHERE mod_id = ?;")) {
                pstmt.setString(1, "starsector-core");
                pstmt.executeUpdate();
            }

            // Update the mods table with a starsector-core entry first to satisfy foreign key constraint.
            try (java.sql.Connection conn = shipeditor.persistence.database.DatabaseManager.getConnection();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement("""
                         INSERT INTO mods (id, name, folder_path, last_scanned)
                         VALUES (?, ?, ?, ?)
                         ON CONFLICT(id) DO UPDATE SET
                             name = excluded.name,
                             folder_path = excluded.folder_path,
                             last_scanned = excluded.last_scanned;
                         """)) {
                pstmt.setString(1, "starsector-core");
                pstmt.setString(2, "Starsector Core");
                pstmt.setString(3, coreFolder.toAbsolutePath().toString());
                pstmt.setLong(4, System.currentTimeMillis());
                pstmt.executeUpdate();
            }

            shipeditor.persistence.database.DatabaseQueryService.upsertFiles(files);
        } catch (Exception e) {
            log.warn("Failed to persist core index to database. Next startup will re-scan.", e);
        }
    }

    public static List<IndexedFile> getFilesByType(String type) {
        if (!isLoaded) {
            loadCoreData();
        }
        return coreFilesByType.getOrDefault(type, Collections.emptyList());
    }

    public static IndexedFile getFileByEntityId(String entityId, String type) {
        if (!isLoaded) {
            loadCoreData();
        }
        Map<String, IndexedFile> entityMap = coreFilesByEntityId.get(type);
        return entityMap != null ? entityMap.get(entityId) : null;
    }

    public static IndexedFile getFileByPath(String pathStr) {
        if (!isLoaded) {
            loadCoreData();
        }
        return coreFilesByPath.get(pathStr);
    }
}
