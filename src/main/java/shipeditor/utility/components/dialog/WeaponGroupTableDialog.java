package shipeditor.utility.components.dialog;

import shipeditor.components.viewer.layers.ship.data.ShipVariant;
import shipeditor.components.viewer.ViewerEnums.FireMode;
import shipeditor.components.viewer.painters.points.ship.features.FittedWeaponGroup;
import shipeditor.components.viewer.painters.points.ship.features.InstalledFeature;
import shipeditor.components.viewer.painters.points.ship.features.InstalledFeatureComparator;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class WeaponGroupTableDialog extends JPanel {

    private final ShipVariant variant;

    private TableModel model;

    private static final int MAX_WEAPON_GROUPS = 7;

    private final boolean[] groupAutofire = new boolean[MAX_WEAPON_GROUPS];

    private final FireMode[] groupModes = new FireMode[MAX_WEAPON_GROUPS];

    WeaponGroupTableDialog(ShipVariant shipVariant) {
        this.variant = shipVariant;

        List<FittedWeaponGroup> weaponGroups = variant.getWeaponGroups();
        int oldListSize = weaponGroups.size();
        for (int i = 0; i < MAX_WEAPON_GROUPS; i++) {
            if (i < oldListSize) {
                FittedWeaponGroup group = weaponGroups.get(i);
                groupAutofire[i] = group.isAutofire();
                groupModes[i] = group.getMode() != null ? group.getMode() : FireMode.LINKED;
            } else {
                groupAutofire[i] = false;
                groupModes[i] = FireMode.LINKED;
            }
        }

        this.setLayout(new BorderLayout(5, 5));
        this.add(createPropertiesPanel(), BorderLayout.NORTH);
        this.add(createTablePanel(), BorderLayout.CENTER);
    }

    private JPanel createPropertiesPanel() {
        JPanel propertiesPanel = new JPanel(new GridBagLayout());
        propertiesPanel.setBorder(BorderFactory.createTitledBorder("Group Properties"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 5, 2, 5);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0.2;
        propertiesPanel.add(new JLabel("Group"), c);
        c.gridx = 1;
        c.weightx = 0.4;
        propertiesPanel.add(new JLabel("Autofire"), c);
        c.gridx = 2;
        c.weightx = 0.4;
        propertiesPanel.add(new JLabel("Fire Mode"), c);

        for (int i = 0; i < MAX_WEAPON_GROUPS; i++) {
            c.gridy = i + 1;

            c.gridx = 0;
            c.weightx = 0.2;
            propertiesPanel.add(new JLabel("Group " + (i + 1) + ":"), c);

            c.gridx = 1;
            c.weightx = 0.4;
            JCheckBox autofireCb = new JCheckBox("Autofire");
            autofireCb.setSelected(groupAutofire[i]);
            final int index = i;
            autofireCb.addActionListener(e -> groupAutofire[index] = autofireCb.isSelected());
            propertiesPanel.add(autofireCb, c);

            c.gridx = 2;
            c.weightx = 0.4;
            JComboBox<FireMode> modeCombo = new JComboBox<>(FireMode.values());
            modeCombo.setSelectedItem(groupModes[i]);
            modeCombo.addActionListener(e -> groupModes[index] = (FireMode) modeCombo.getSelectedItem());
            propertiesPanel.add(modeCombo, c);
        }

        return propertiesPanel;
    }

    private JPanel createTablePanel() {
        this.model = createModel();
        JTable table = new GroupTable(this.model);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(BorderFactory.createTitledBorder("Weapon Assignments"));
        container.add(scrollPane, BorderLayout.CENTER);

        return container;
    }

    private DefaultTableModel createModel() {
        List<InstalledFeature> weapons = variant.getAllFittedWeaponsList();
        List<FittedWeaponGroup> weaponGroups = variant.getWeaponGroups();

        weapons.sort(new InstalledFeatureComparator());
        Collections.reverse(weapons);

        int oldListSize = weaponGroups.size();

        Object[][] data = new Object[weapons.size()][MAX_WEAPON_GROUPS + 1];

        for (int r = 0; r < weapons.size(); r++) {
            InstalledFeature feature = weapons.get(r);
            data[r][0] = feature;

            for (int i = 0; i < MAX_WEAPON_GROUPS; i++) {
                if (oldListSize > i) {
                    FittedWeaponGroup group = weaponGroups.get(i);
                    data[r][i + 1] = group.containsFitting(feature) ? Boolean.TRUE : Boolean.FALSE;
                } else {
                    data[r][i + 1] = Boolean.FALSE;
                }
            }
        }

        Object[] columnNames = new Object[MAX_WEAPON_GROUPS + 1];
        columnNames[0] = "Weapon";
        for (int i = 0; i < MAX_WEAPON_GROUPS; i++) {
            columnNames[i + 1] = String.valueOf(i + 1);
        }

        return new GroupTableModel(data, columnNames);
    }

    List<FittedWeaponGroup> getUpdatedGroups() {
        List<FittedWeaponGroup> updated = new ArrayList<>();

        int rowCount = model.getRowCount();

        for (int i = 1; i < MAX_WEAPON_GROUPS + 1; i++) {
            FittedWeaponGroup group = null;

            for (int r = 0; r < rowCount; r++) {
                boolean isInGroup = (boolean) model.getValueAt(r, i);
                if (isInGroup) {
                    if (group == null) {
                        group = new FittedWeaponGroup(variant, groupAutofire[i - 1], groupModes[i - 1]);
                    }
                    InstalledFeature rowWeapon = (InstalledFeature) model.getValueAt(r, 0);

                    group.addFitting(rowWeapon.getSlotID(), rowWeapon);
                }
            }

            if (group != null) {
                updated.add(group);
            }
        }

        return updated;
    }

    private static final class GroupTableModel extends DefaultTableModel {

        private GroupTableModel(Object[][] data, Object[] columnNames) {
            super(data, columnNames);
        }

        public Class<?> getColumnClass(int columnIndex) {
            Object value = getValueAt(0, columnIndex);
            return value.getClass();
        }

        public boolean isCellEditable(int row, int column) {
            return column >= 1;
        }

        public void setValueAt(Object aValue, int row, int column) {
            for (int i = 1; i < getColumnCount(); i++) {
                super.setValueAt(Boolean.FALSE, row, i);
            }
            super.setValueAt(Boolean.TRUE, row, column);
            fireTableDataChanged();
        }

    }

    private static class FeatureNameRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof InstalledFeature feature) {
                setText(feature.getName());
            }
            return this;
        }

    }

    private static final class GroupTable extends JTable {

        private GroupTable(TableModel model) {
            super(model);

            Dimension preferredSize = this.getPreferredSize();
            int width = Math.max(preferredSize.width, 600);
            int height = Math.max(preferredSize.height, 400);

            this.setPreferredScrollableViewportSize(new Dimension(width, height));

            JTableHeader header = this.getTableHeader();
            header.setReorderingAllowed(false);

            TableColumnModel tableColumnModel = this.getColumnModel();
            int columnCount = tableColumnModel.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                TableColumn column = tableColumnModel.getColumn(i);
                if (i > 0) {
                    column.setMinWidth(25);
                    column.setPreferredWidth(25);
                } else {
                    column.setMinWidth(100);
                    column.setPreferredWidth(150);
                }
            }
        }

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            if (column == 0) {
                return new FeatureNameRenderer();
            }
            return super.getCellRenderer(row, column);
        }

    }

}
