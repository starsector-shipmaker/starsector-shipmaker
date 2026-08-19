package shipeditor.menubar;

import org.kordamp.ikonli.boxicons.BoxiconsRegular;
import org.kordamp.ikonli.swing.FontIcon;
import shipeditor.communication.EventBus;
import shipeditor.components.viewer.control.ControlPredicates;
import shipeditor.components.viewer.ViewerEnums.PointSelectionMode;
import shipeditor.components.viewer.layers.LayerManager;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.undo.UndoOverseer;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.themes.Themes;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import shipeditor.communication.events.viewer.control.ControlEvents.RotationRoundingToggled;
import shipeditor.communication.events.viewer.control.ControlEvents.PointSelectionModeChange;
import shipeditor.communication.events.viewer.control.ControlEvents.CursorSnappingToggled;

class EditMenu extends JMenu {

    private JCheckBoxMenuItem toggleCursorSnap;
    private JCheckBoxMenuItem toggleRotationRounding;

    EditMenu() {
        super("Edit");
        this.setMnemonic(KeyEvent.VK_E);
    }

    void initialize() {
        JMenuItem undo = new JMenuItem("Undo");
        undo.setAction(UndoOverseer.getUndoAction());
        undo.setIcon(FontIcon.of(BoxiconsRegular.UNDO, 16, Themes.getIconColor()));
        KeyStroke keyStrokeToUndo = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK);
        undo.setAccelerator(keyStrokeToUndo);
        this.add(undo);

        JMenuItem redo = new JMenuItem("Redo");
        redo.setAction(UndoOverseer.getRedoAction());
        redo.setIcon(FontIcon.of(BoxiconsRegular.REDO, 16, Themes.getIconColor()));
        KeyStroke keyStrokeToRedo = KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK);
        redo.setAccelerator(keyStrokeToRedo);
        this.add(redo);

        this.addSeparator();

        JMenuItem flipShip = new JMenuItem("Flip Active Ship Horizontally");
        flipShip.setIcon(FontIcon.of(BoxiconsRegular.REFRESH, 16, Themes.getIconColor()));
        flipShip.addActionListener(e -> {
            LayerManager layerManager = StaticController.getLayerManager();
            if (layerManager != null && layerManager.getActiveLayer() instanceof ShipLayer shipLayer) {
                ShipPainter painter = shipLayer.getPainter();
                if (painter != null) {
                    painter.flipShipPointsHorizontally();
                }
            }
        });
        this.add(flipShip);

        this.addSeparator();

        JMenuItem pointSelectionMode = this.createPointSelectionModeOptions();
        this.add(pointSelectionMode);

        JMenu snappingMenu = new JMenu("Snapping & Rounding");
        snappingMenu.setIcon(FontIcon.of(BoxiconsRegular.MAGNET, 16, Themes.getIconColor()));

        JCheckBoxMenuItem toggleSelectionHold = new JCheckBoxMenuItem("Enable Selection Holding (Ctrl-Hold)");
        toggleSelectionHold.setSelected(true);
        toggleSelectionHold.setToolTipText("Enables CTRL-hold to prevent mouse motion from changing selection.");
        toggleSelectionHold.addActionListener(event ->
                ControlPredicates.setSelectionHoldingEnabled(toggleSelectionHold.isSelected())
        );
        snappingMenu.add(toggleSelectionHold);

        toggleCursorSnap = new JCheckBoxMenuItem("Enable Cursor Snapping");
        toggleCursorSnap.setSelected(true);
        toggleCursorSnap.addActionListener(event ->
                EventBus.publish(new CursorSnappingToggled(toggleCursorSnap.isSelected()))
        );
        EventBus.subscribe(this, event -> {
            if (event instanceof CursorSnappingToggled checked) {
                toggleCursorSnap.setSelected(checked.toggled());
            }
        });
        snappingMenu.add(toggleCursorSnap);

        toggleRotationRounding = new JCheckBoxMenuItem("Enable Rotation Rounding (15°)");
        toggleRotationRounding.setSelected(true);
        toggleRotationRounding.addActionListener(event ->
                EventBus.publish(new RotationRoundingToggled(toggleRotationRounding.isSelected()))
        );
        EventBus.subscribe(this, event -> {
            if (event instanceof RotationRoundingToggled checked) {
                toggleRotationRounding.setSelected(checked.toggled());
            }
        });
        snappingMenu.add(toggleRotationRounding);

        this.add(snappingMenu);

        this.addSeparator();

        JMenuItem slotDefaults = new JMenuItem("Weapon Slot Creation Defaults...");
        slotDefaults.setIcon(FontIcon.of(BoxiconsRegular.PLUS_CIRCLE, 16, Themes.getIconColor()));
        slotDefaults.addActionListener(e -> shipeditor.utility.components.dialog.DialogUtilities.showSlotCreationDialog());
        this.add(slotDefaults);
    }

    private JMenu createPointSelectionModeOptions() {
        JMenu newSubmenu = new JMenu("Point Selection Mode");
        newSubmenu.setIcon(FontIcon.of(BoxiconsRegular.POINTER, 16, Themes.getIconColor()));

        JMenuItem selectHovered = new JRadioButtonMenuItem("Select Clicked Point");
        selectHovered.addActionListener(e ->
                EventBus.publish(new PointSelectionModeChange(PointSelectionMode.STRICT)));
        newSubmenu.add(selectHovered);

        JMenuItem selectClosest = new JRadioButtonMenuItem("Select Closest Point");
        selectClosest.addActionListener(e ->
                EventBus.publish(new PointSelectionModeChange(PointSelectionMode.CLOSEST)));
        newSubmenu.add(selectClosest);
        selectClosest.setSelected(true);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(selectHovered);
        buttonGroup.add(selectClosest);

        EventBus.subscribe(this, event -> {
            if (event instanceof PointSelectionModeChange checked) {
                if (checked.newMode() == PointSelectionMode.STRICT && !selectHovered.isSelected()) {
                    selectHovered.setSelected(true);
                } else if (checked.newMode() == PointSelectionMode.CLOSEST && !selectClosest.isSelected()) {
                    selectClosest.setSelected(true);
                }
            }
        });

        return newSubmenu;
    }

}
