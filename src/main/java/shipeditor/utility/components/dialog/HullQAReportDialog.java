package shipeditor.utility.components.dialog;

import shipeditor.parsing.loading.FileLoading;
import shipeditor.persistence.database.DatabaseQueryService;
import shipeditor.persistence.database.IndexedFile;
import shipeditor.representation.ship.HullSpecFile;
import shipeditor.representation.weapon.WeaponSlot;
import shipeditor.utility.text.StringConstants;
import shipeditor.components.datafiles.entities.ShipCSVEntry;
import shipeditor.representation.GameDataRepository;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HullQAReportDialog extends JDialog {

    private final JTable table;
    private final QATableModel model;
    private final JLabel statusLabel;

    public HullQAReportDialog() {
        super(shipeditor.PrimaryWindow.getInstance(), "Hull QA Report", false);
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setSize(700, 500);
        this.setLocationRelativeTo(shipeditor.PrimaryWindow.getInstance());

        model = new QATableModel();
        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(150); // Hull ID
        table.getColumnModel().getColumn(1).setPreferredWidth(100); // Issue Type
        table.getColumnModel().getColumn(2).setPreferredWidth(450); // Details

        JScrollPane scrollPane = new JScrollPane(table);
        this.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel(" Scanning hulls...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        bottomPanel.add(statusLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> this.dispose());
        buttonPanel.add(closeButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        this.add(bottomPanel, BorderLayout.SOUTH);

        runQAAnalysis();
    }

    private void runQAAnalysis() {
        SwingWorker<List<QAIssue>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<QAIssue> doInBackground() {
                List<QAIssue> issues = new ArrayList<>();
                List<IndexedFile> hullFiles = DatabaseQueryService.getFilesByType(StringConstants.SHIP_TYPE);

                for (IndexedFile f : hullFiles) {
                    try {
                        HullSpecFile spec = FileLoading.loadHullFile(f.getFilePath().toFile());
                        if (spec == null) continue;

                        WeaponSlot[] slots = spec.getWeaponSlots();
                        int bayCount = 0;
                        if (slots != null) {
                            List<String> seenIds = new ArrayList<>();
                            for (WeaponSlot slot : slots) {
                                // Duplicate ID check
                                if (seenIds.contains(slot.getId())) {
                                    issues.add(new QAIssue(spec.getHullId(), "Duplicate Slot ID", "Duplicate ID found: " + slot.getId()));
                                } else {
                                    seenIds.add(slot.getId());
                                }

                                if (StringConstants.LAUNCH_BAY.equals(slot.getType())) {
                                    bayCount++;
                                    // Degenerate arcs
                                    if (slot.getArc() <= 0) {
                                        issues.add(new QAIssue(spec.getHullId(), "Degenerate Arc", "Bay " + slot.getId() + " has arc <= 0."));
                                    }
                                    // Orphaned bays
                                    if (slot.getLocations() == null || slot.getLocations().length == 0) {
                                        issues.add(new QAIssue(spec.getHullId(), "Orphaned Bay", "Bay " + slot.getId() + " has no port points."));
                                    } else {
                                        for (int i = 0; i < slot.getLocations().length; i++) {
                                            java.awt.geom.Point2D.Double loc = slot.getLocations()[i];
                                            if (loc != null && loc.x == 0 && loc.y == 0) {
                                                issues.add(new QAIssue(spec.getHullId(), "Center Port", "Bay " + slot.getId() + " port " + i + " is at (0, 0)."));
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Check ships with fewer bays than ship_data.csv fighter bays
                        ShipCSVEntry csvEntry = GameDataRepository.retrieveShipCSVEntryByID(spec.getHullId());
                        if (csvEntry != null) {
                            int csvBays = csvEntry.getBayCount();
                            if (csvBays > bayCount) {
                                issues.add(new QAIssue(spec.getHullId(), "Missing Bays", "Hull has " + bayCount + " bays, but ship_data.csv specifies " + csvBays + "."));
                            }
                        }

                    } catch (Exception e) {
                        issues.add(new QAIssue(f.getEntityId(), "Parse Error", "Failed to parse hull file."));
                    }
                }
                return issues;
            }

            @Override
            protected void done() {
                try {
                    List<QAIssue> issues = get();
                    model.setIssues(issues);
                    if (issues.isEmpty()) {
                        statusLabel.setText(" ✅ All hulls passed QA!");
                    } else {
                        statusLabel.setText(" ⚠️ Found " + issues.size() + " potential issues.");
                    }
                } catch (Exception e) {
                    statusLabel.setText(" ❌ Error during QA analysis.");
                }
            }
        };
        worker.execute();
    }

    private record QAIssue(String hullId, String issueType, String details) {}

    private static class QATableModel extends AbstractTableModel {
        private final String[] COLUMNS = {"Hull ID", "Issue", "Details"};
        private List<QAIssue> issues = new ArrayList<>();

        public void setIssues(List<QAIssue> issues) {
            this.issues = issues;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() { return issues.size(); }
        @Override
        public int getColumnCount() { return COLUMNS.length; }
        @Override
        public String getColumnName(int column) { return COLUMNS[column]; }
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) { return false; }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            QAIssue issue = issues.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> issue.hullId;
                case 1 -> issue.issueType;
                case 2 -> issue.details;
                default -> "";
            };
        }
    }
}
