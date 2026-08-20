package shipeditor.menubar;

import lombok.extern.log4j.Log4j2;
import org.kordamp.ikonli.boxicons.BoxiconsRegular;
import org.kordamp.ikonli.swing.FontIcon;
import shipeditor.parsing.FileUtilities;
import shipeditor.parsing.JsonProcessor;
import shipeditor.parsing.loading.FileLoading;
import shipeditor.utility.Utility;
import shipeditor.utility.themes.Themes;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@Log4j2
class DataMenu extends JMenu {

    DataMenu() {
        super("Data");
        this.setMnemonic(KeyEvent.VK_D);
    }

    void initialize() {
        JMenuItem reloadAllGameData = new JMenuItem("Reload Game Data");
        reloadAllGameData.setIcon(FontIcon.of(BoxiconsRegular.REFRESH, 16, Themes.getIconColor()));
        reloadAllGameData.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        reloadAllGameData.addActionListener(event -> FileLoading.loadGameData());
        if (FileLoading.isLoadingInProgress()) {
            reloadAllGameData.setEnabled(false);
        }
        this.add(reloadAllGameData);

        this.addSeparator();

        JMenuItem reindexData = new JMenuItem("Re-index Mod Folders & Reload");
        reindexData.setIcon(FontIcon.of(BoxiconsRegular.REFRESH, 16, Themes.getIconColor()));
        reindexData.addActionListener(event -> {
            if (FileLoading.isLoadingInProgress()) {
                JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                        "Cannot re-index while data is currently loading.",
                        "Loading in Progress", JOptionPane.WARNING_MESSAGE);
                return;
            }
            FileLoading.forceReindexAndLoadGameData();
        });
        if (FileLoading.isLoadingInProgress()) {
            reindexData.setEnabled(false);
        }
        this.add(reindexData);

        this.addSeparator();

        JMenuItem selectMods = new JMenuItem("Select Mods to Load...");
        selectMods.setIcon(FontIcon.of(BoxiconsRegular.LIST_CHECK, 16, Themes.getIconColor()));
        selectMods.addActionListener(event -> {
            if (FileLoading.isLoadingInProgress()) {
                JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                        "Cannot modify mod selection while data is currently loading.",
                        "Loading in Progress", JOptionPane.WARNING_MESSAGE);
                return;
            }
            shipeditor.components.dialogs.ModSelectionDialog modDialog = new shipeditor.components.dialogs.ModSelectionDialog(shipeditor.PrimaryWindow.getInstance());
            boolean shouldLoad = modDialog.showDialog();
            if (shouldLoad) {
                FileLoading.forceReindexAndLoadGameData();
            }
        });
        this.add(selectMods);

        JMenuItem dataFilters = new JMenuItem("Data Filters...");
        dataFilters.setIcon(FontIcon.of(BoxiconsRegular.FILTER, 16, Themes.getIconColor()));
        dataFilters.addActionListener(event -> shipeditor.components.datafiles.trees.FilterDialogs.showCombinedFilters(null));
        this.add(dataFilters);

        this.addSeparator();

        JMenuItem jsonCorrector = DataMenu.getJSONCorrector();
        this.add(jsonCorrector);

        this.addSeparator();

        JMenuItem qaReport = new JMenuItem("Weapon Offset QA Report...");
        qaReport.setIcon(FontIcon.of(BoxiconsRegular.SHIELD_QUARTER, 16, Themes.getIconColor()));
        qaReport.addActionListener(event -> {
            shipeditor.utility.components.dialog.WeaponQAReportDialog dialog = new shipeditor.utility.components.dialog.WeaponQAReportDialog();
            dialog.setVisible(true);
        });
        this.add(qaReport);

        JMenuItem hullQaReport = new JMenuItem("Hull QA Report...");
        hullQaReport.setIcon(FontIcon.of(BoxiconsRegular.SHIELD_QUARTER, 16, Themes.getIconColor()));
        hullQaReport.addActionListener(event -> {
            shipeditor.utility.components.dialog.HullQAReportDialog dialog = new shipeditor.utility.components.dialog.HullQAReportDialog();
            dialog.setVisible(true);
        });
        this.add(hullQaReport);
    }

    private static JMenuItem getJSONCorrector() {
        JMenuItem jsonCorrector = new JMenuItem("Repair Malformed JSON...");
        jsonCorrector.setIcon(FontIcon.of(BoxiconsRegular.WRENCH, 16, Themes.getIconColor()));
        jsonCorrector.setToolTipText("Fixes semantically incorrect JSON, then saves a corrected copy to the same location");
        jsonCorrector.addActionListener(e -> {
            JFileChooser fileChooser = FileUtilities.getFileChooser();

            File directory = FileUtilities.getLastGeneralDirectory();
            if (directory != null) {
                fileChooser.setCurrentDirectory(directory);
            }

            int returnVal = fileChooser.showOpenDialog(shipeditor.PrimaryWindow.getInstance());
            File currentDirectory = fileChooser.getCurrentDirectory();
            FileUtilities.setLastGeneralDirectory(currentDirectory);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                DataMenu.correctJSON(fileChooser, currentDirectory);
            }
        });
        return jsonCorrector;
    }

    private static void correctJSON(JFileChooser fileChooser, File currentDirectory) {
        File file = fileChooser.getSelectedFile();
        if (file == null) return;

        try {
            String result = JsonProcessor.straightenMalformed(file);
            String fixedFileName = Utility.getFilenameWithoutExtension(file.getName()) + "_corrected.json";
            String targetFilePath = new File(currentDirectory, fixedFileName).getPath();
            try (PrintWriter out = new PrintWriter(targetFilePath, StandardCharsets.UTF_8)) {
                out.println(result);
            }
            JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                    "Corrected file saved as:\n" + targetFilePath,
                    "JSON Corrected", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            log.error("Failed to correct malformed JSON file: " + file.getPath(), ex);
            JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                    "Failed to process JSON file:\n" + ex.getMessage(),
                    "Error Correcting JSON", JOptionPane.ERROR_MESSAGE);
        }
    }

}
