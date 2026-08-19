package shipeditor.utility.components.dialog;

import javax.swing.DefaultListCellRenderer;

import shipeditor.components.datafiles.entities.WeaponCSVEntry;
import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.layers.ship.data.ShipSkin;
import shipeditor.persistence.SettingsManager;
import shipeditor.representation.GameDataRepository;
import shipeditor.representation.weapon.WeaponEnums.WeaponType;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class PickWeaponDialog extends JPanel {

    private final WeaponSlotPoint slotPoint;
    private final Path shipPackage;
    
    private List<WeaponCSVEntry> allEligibleWeapons;
    private DefaultListModel<WeaponCSVEntry> listModel;
    private JList<WeaponCSVEntry> weaponList;
    
    private WeaponCSVEntry selectedWeapon;
    private final Runnable onDoubleClick;

    PickWeaponDialog(WeaponSlotPoint slotPoint, Runnable onDoubleClick) {
        this.slotPoint = slotPoint;
        this.onDoubleClick = onDoubleClick;
        this.shipPackage = resolveShipPackage(slotPoint);

        this.setLayout(new BorderLayout());
        this.initData();
        this.add(createTopPanel(), BorderLayout.PAGE_START);
        this.add(createListPanel(), BorderLayout.CENTER);
    }
    
    WeaponCSVEntry getSelectedWeapon() {
        return weaponList.getSelectedValue();
    }

    private Path resolveShipPackage(WeaponSlotPoint point) {
        if (point == null) return null;
        ShipPainter shipPainter = point.getParent();
        if (shipPainter != null) {
            ShipSkin activeSkin = shipPainter.getActiveSkin();
            if (activeSkin != null && !activeSkin.isBase()) {
                return activeSkin.getContainingPackage();
            } else {
                var shipLayer = shipPainter.getParentLayer();
                var shipHull = shipLayer.getHull();
                if (shipHull != null) {
                    var shipEntry = GameDataRepository.retrieveShipCSVEntryByID(shipHull.getHullID());
                    if (shipEntry != null) {
                        return shipEntry.getPackageFolderPath();
                    }
                }
            }
        }
        return null;
    }

    private void initData() {
        allEligibleWeapons = new ArrayList<>();
        Map<Path, List<WeaponCSVEntry>> weaponEntriesByPackage = SettingsManager.getGameData().getWeaponEntriesByPackage();
        
        if (weaponEntriesByPackage != null) {
            for (Map.Entry<Path, List<WeaponCSVEntry>> entry : weaponEntriesByPackage.entrySet()) {
                Path packagePath = entry.getKey();
                List<WeaponCSVEntry> weapons = entry.getValue();
                
                boolean isCore = SettingsManager.isCoreFolder(packagePath);
                boolean isShipMod = shipPackage != null && packagePath.equals(shipPackage);
                
                if (isCore || isShipMod || shipPackage == null) {
                    for (WeaponCSVEntry weapon : weapons) {
                        if (slotPoint == null || WeaponType.isWeaponFitting(slotPoint, weapon)) {
                            allEligibleWeapons.add(weapon);
                        }
                    }
                }
            }
        }
        
        allEligibleWeapons.sort((w1, w2) -> w1.getWeaponID().compareToIgnoreCase(w2.getWeaponID()));
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        JTextField searchField = new JTextField();
        searchField.setToolTipText("Search by weapon ID or name");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateList(searchField.getText()); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateList(searchField.getText()); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateList(searchField.getText()); }
        });
        
        topPanel.add(new JLabel("Search: "), BorderLayout.WEST);
        topPanel.add(searchField, BorderLayout.CENTER);
        
        return topPanel;
    }

    private JPanel createListPanel() {
        listModel = new DefaultListModel<>();
        allEligibleWeapons.forEach(listModel::addElement);
        
        weaponList = new JList<>(listModel);
        weaponList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        weaponList.setCellRenderer(new WeaponCellRenderer());
        
        weaponList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selectedWeapon = weaponList.getSelectedValue();
                    if (selectedWeapon != null && onDoubleClick != null) {
                        onDoubleClick.run();
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(weaponList);
        scrollPane.setPreferredSize(new Dimension(500, 500));
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void updateList(String filter) {
        listModel.clear();
        String lowerFilter = filter.toLowerCase(Locale.ROOT);
        for (WeaponCSVEntry weapon : allEligibleWeapons) {
            boolean matches = weapon.getWeaponID().toLowerCase(Locale.ROOT).contains(lowerFilter) ||
                              weapon.toString().toLowerCase(Locale.ROOT).contains(lowerFilter);
            if (matches) {
                listModel.addElement(weapon);
            }
        }
    }

    private static class WeaponCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof WeaponCSVEntry entry) {
                setText(entry.toString() + " (" + entry.getWeaponID() + ") - " + entry.getType().getDisplayedName() + " / " + entry.getSize().getDisplayedName());
                
                var sprite = entry.getWeaponImage();
                if (sprite != null && sprite.getImage() != null) {
                    Image scaled = ComponentUtilities.resizeImageToSquareLimit(sprite.getImage(), 32);
                    setIcon(new ImageIcon(scaled));
                } else {
                    setIcon(null);
                }
            }
            return this;
        }
    }
}
