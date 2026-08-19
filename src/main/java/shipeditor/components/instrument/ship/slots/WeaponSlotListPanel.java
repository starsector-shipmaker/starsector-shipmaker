package shipeditor.components.instrument.ship.slots;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import shipeditor.communication.EventBus;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.instrument.ship.AbstractShipPropertiesPanel;
import shipeditor.components.viewer.entities.weapon.WeaponSlotPoint;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ship.ShipPainter;
import shipeditor.components.viewer.ViewerEnums.PainterVisibility;
import shipeditor.components.viewer.painters.points.AbstractPointPainter;
import shipeditor.components.viewer.painters.points.ship.WeaponSlotPainter;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.objects.Pair;
import shipeditor.utility.text.StringValues;

import org.kordamp.ikonli.boxicons.BoxiconsRegular;
import org.kordamp.ikonli.swing.FontIcon;
import shipeditor.utility.components.dialog.DialogUtilities;
import shipeditor.utility.themes.Themes;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Map;
import java.util.function.Function;
import shipeditor.communication.events.components.ComponentEvents.InstrumentRepaintQueued;
import shipeditor.communication.events.viewer.points.PointEvents.PointAddConfirmed;
import shipeditor.communication.events.viewer.points.PointEvents.WeaponSlotInsertedConfirmed;
import shipeditor.communication.events.viewer.points.PointEvents.PointRemovedConfirmed;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class WeaponSlotListPanel extends AbstractShipPropertiesPanel {

    @Getter
    private WeaponSlotList slotPointContainer;

    private SlotDataControlPane slotDataPane;

    private JCheckBox reorderCheckbox;

    private DefaultListModel<WeaponSlotPoint> model;

    WeaponSlotListPanel() {
        this.initPointListener();
    }

    private void refreshPointDataPane(WeaponSlotPoint slotPoint) {
        ShipPainter painter = (ShipPainter) getCachedLayerPainter();
        if (slotPoint != null) {
            painter = slotPoint.getParent();
            this.slotDataPane.refreshWithSelectedPoint(painter, slotPoint);
        } else {
            this.slotDataPane.refresh(painter);
        }
    }

    @Override
    public void refreshContent(LayerPainter layerPainter) {
        DefaultListModel<WeaponSlotPoint> newModel = new DefaultListModel<>();

        if (!(layerPainter instanceof ShipPainter shipPainter) || shipPainter.isUninitialized()) {
            this.model = newModel;
            this.slotPointContainer.setModel(newModel);

            fireClearingListeners(layerPainter);
            refreshPointDataPane(null);

            this.slotPointContainer.setEnabled(false);
            this.reorderCheckbox.setEnabled(false);
            return;
        }

        WeaponSlotPainter weaponSlotPainter = shipPainter.getWeaponSlotPainter();
        newModel.addAll(weaponSlotPainter.getPointsIndex());

        this.model = newModel;
        this.slotPointContainer.setModel(newModel);
        this.slotPointContainer.setEnabled(true);
        this.reorderCheckbox.setEnabled(true);

        fireRefresherListeners(layerPainter);
        refreshPointDataPane(weaponSlotPainter.getSelected());
    }

    @Override
    protected void populateContent() {
        this.setLayout(new BorderLayout());

        this.model = new DefaultListModel<>();
        this.slotPointContainer = new WeaponSlotList(model, this::refreshPointDataPane);
        this.slotDataPane = new SlotDataControlPane(slotPointContainer);

        JPanel northContainer = new JPanel(new BorderLayout());
        var visibilityWidget = createSlotsVisibilityWidget();
        Map<JLabel, JComponent> visibilityWidgetMap = Map.of(visibilityWidget.getFirst(), visibilityWidget.getSecond());
        JPanel visibilityWidgetContainer = this.createWidgetsPanel(visibilityWidgetMap);
        visibilityWidgetContainer.setBorder(new EmptyBorder(4, 0, 3, 0));
        northContainer.add(visibilityWidgetContainer, BorderLayout.PAGE_START);

        ComponentUtilities.outfitPanelWithTitle(slotDataPane, "Slot Data");
        northContainer.add(slotDataPane, BorderLayout.CENTER);

        this.refreshPointDataPane(null);

        JScrollPane scrollableContainer = new JScrollPane(slotPointContainer);

        Pair<JPanel, JCheckBox> reorderWidget = ComponentUtilities.createReorderCheckboxPanel(slotPointContainer);
        reorderCheckbox = reorderWidget.getSecond();

        JButton addSlotDefaultsBtn = new JButton("New Slot Defaults...");
        addSlotDefaultsBtn.setIcon(FontIcon.of(BoxiconsRegular.PLUS_CIRCLE, 14, Themes.getIconColor()));
        addSlotDefaultsBtn.setToolTipText("Configure default properties and mode for newly created weapon slots");
        addSlotDefaultsBtn.addActionListener(e -> DialogUtilities.showSlotCreationDialog());
        reorderWidget.getFirst().add(addSlotDefaultsBtn);
        reorderWidget.getFirst().add(Box.createRigidArea(new Dimension(6, 0)));

        northContainer.add(reorderWidget.getFirst(), BorderLayout.PAGE_END);

        this.add(northContainer, BorderLayout.PAGE_START);

        this.add(scrollableContainer, BorderLayout.CENTER);
    }

    @Override
    protected void initLayerListeners() {
        super.initLayerListeners();
        EventBus.subscribe(this, event -> {
            if (event instanceof InstrumentRepaintQueued checked) {
                if (checked.editorMode() != EditorInstrument.WEAPON_SLOTS) {
                    return;
                }
                WeaponSlotPainter cachedSlotPainter = getCachedSlotPainter();
                if (cachedSlotPainter != null) {
                    java.util.List<WeaponSlotPoint> currentPoints = cachedSlotPainter.getPointsIndex();
                    boolean modelsEqual = true;
                    if (this.model.getSize() == currentPoints.size()) {
                        for (int i = 0; i < this.model.getSize(); i++) {
                            if (this.model.getElementAt(i) != currentPoints.get(i)) {
                                modelsEqual = false;
                                break;
                            }
                        }
                    } else {
                        modelsEqual = false;
                    }

                    if (!modelsEqual) {
                        int[] cachedSelected = this.slotPointContainer.getSelectedIndices();
                        DefaultListModel<WeaponSlotPoint> newModel = new DefaultListModel<>();
                        newModel.addAll(currentPoints);

                        this.model = newModel;
                        this.slotPointContainer.setModel(newModel);
                        this.slotPointContainer.setSelectedIndices(cachedSelected);
                        if (!this.model.isEmpty() && cachedSelected.length > 0) {
                            this.slotPointContainer.ensureIndexIsVisible(cachedSelected[0]);
                        }
                    } else {
                        this.slotPointContainer.repaint();
                    }
                }

                this.refreshPointDataPane(null);
            }
        });
    }

    private void initPointListener() {
        EventBus.subscribe(this, event -> {
            if (event instanceof PointAddConfirmed checked && checked.point() instanceof WeaponSlotPoint point) {
                model.addElement(point);
                slotPointContainer.setSelectedIndex(model.indexOf(point));
                this.refreshPointDataPane(null);
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof WeaponSlotInsertedConfirmed checked) {
                model.insertElementAt(checked.toInsert(), checked.precedingIndex());
                slotPointContainer.setSelectedIndex(model.indexOf(checked.toInsert()));
                this.refreshPointDataPane(null);
            }
        });
        EventBus.subscribe(this, event -> {
            if (event instanceof PointRemovedConfirmed checked && checked.point() instanceof WeaponSlotPoint point) {
                model.removeElement(point);
                this.refreshPointDataPane(null);
            }
        });
    }

    private WeaponSlotPainter getCachedSlotPainter() {
        LayerPainter cachedLayerPainter = getCachedLayerPainter();
        if (cachedLayerPainter instanceof ShipPainter shipPainter && !shipPainter.isUninitialized()) {
            return shipPainter.getWeaponSlotPainter();
        }
        return null;
    }

    private Pair<JLabel, JComboBox<PainterVisibility>> createSlotsVisibilityWidget() {
        Function<LayerPainter, AbstractPointPainter> painterGetter = layerPainter -> {
            if (layerPainter instanceof ShipPainter shipPainter) {
                return shipPainter.getWeaponSlotPainter();
            }
            return null;
        };

        var opacityWidget = createVisibilityWidget(painterGetter);

        JLabel opacityLabel = opacityWidget.getFirst();
        opacityLabel.setText(StringValues.SLOTS_VIEW);

        return opacityWidget;
    }

}
