package shipeditor.components.instrument.ship.slots;
import shipeditor.components.ComponentEnums.SlotCreationMode;


import lombok.Getter;
import lombok.Setter;
import shipeditor.representation.weapon.WeaponEnums.WeaponMount;
import shipeditor.representation.weapon.WeaponEnums.WeaponSize;
import shipeditor.representation.weapon.WeaponEnums.WeaponType;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.components.widgets.Spinners;
import shipeditor.utility.objects.Pair;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import shipeditor.utility.components.UIConstants;
import shipeditor.utility.themes.Themes;

public class SlotCreationPane extends JPanel {

    @Getter @Setter
    private static WeaponType defaultType = WeaponType.BALLISTIC;

    @Getter @Setter
    private static WeaponMount defaultMount = WeaponMount.TURRET;

    @Getter @Setter
    private static WeaponSize defaultSize = WeaponSize.SMALL;

    @Getter @Setter
    private static double defaultAngle;

    @Getter @Setter
    private static double defaultArc;

    @Getter @Setter
    private static SlotCreationMode mode = SlotCreationMode.BY_DEFAULT;

    public SlotCreationPane() {
        this.setLayout(new BorderLayout());

        this.add(SlotCreationPane.createModePanel(), BorderLayout.PAGE_START);

        JPanel selectorsPane = new JPanel();
        selectorsPane.setLayout(new BoxLayout(selectorsPane, BoxLayout.PAGE_AXIS));
        selectorsPane.setAlignmentY(0);
        selectorsPane.add(Box.createRigidArea(UIConstants.PADDING_10_4));
        selectorsPane.add(SlotCreationPane.createDefaultValueSpinners());
        selectorsPane.add(Box.createRigidArea(UIConstants.PADDING_10_4));
        selectorsPane.add(SlotCreationPane.createSlotTypeSelectors());
        selectorsPane.add(SlotCreationPane.createSlotMountSelectors());
        selectorsPane.add(SlotCreationPane.createSlotSizeSelectors());
        selectorsPane.add(Box.createVerticalGlue());

        JScrollPane scrollContainer = new JScrollPane(selectorsPane);
        scrollContainer.setBorder(new EmptyBorder(0, 0, 0, 0));
        JScrollBar verticalScrollBar = scrollContainer.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(12);

        this.add(scrollContainer, BorderLayout.CENTER);
        this.setPreferredSize(this.getMinimumSize());
    }

    private static JPanel createModePanel() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
        container.setAlignmentX(0.5f);
        container.setAlignmentY(0);

        MatteBorder matteLine = new MatteBorder(new Insets(1, 0, 0, 0),
                Themes.getBorderColor());
        Border titledBorder = new TitledBorder(matteLine, "New slot values",
                TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION);
        container.setBorder(titledBorder);

        ButtonGroup selectorButtons = new ButtonGroup();

        JRadioButton fromClosestSlot = new JRadioButton("From closest slot");
        fromClosestSlot.setToolTipText("New slots inherit type, mount, size, angle, and arc from the nearest slot");
        fromClosestSlot.addActionListener(e -> mode = SlotCreationMode.BY_CLOSEST);
        container.add(SlotCreationPane.createSlotKindPane(selectorButtons, fromClosestSlot));

        JRadioButton fromPanelDefaults = new JRadioButton("From panel defaults");
        fromPanelDefaults.setToolTipText("New slots use the configured default parameters below");
        fromPanelDefaults.addActionListener(e -> mode = SlotCreationMode.BY_DEFAULT);
        container.add(SlotCreationPane.createSlotKindPane(selectorButtons, fromPanelDefaults));
        
        if (mode == SlotCreationMode.BY_CLOSEST) {
            fromClosestSlot.setSelected(true);
        } else {
            fromPanelDefaults.setSelected(true);
        }

        return container;
    }

    private static JPanel createDefaultValueSpinners() {
        JPanel container = new JPanel();
        container.setLayout(new GridBagLayout());

        Spinners.addLabelWithDegreeSpinner(container, "Default angle:",
                defaultAngle, aDouble -> defaultAngle = aDouble, 0);

        Spinners.addLabelWithDegreeSpinner(container, "Default arc:",
                defaultArc, aDouble -> defaultArc = aDouble, 1);

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BorderLayout());
        wrapper.setAlignmentX(0.5f);
        wrapper.setAlignmentY(0);
        Dimension containerPreferredSize = container.getPreferredSize();
        wrapper.setMaximumSize(new Dimension(container.getMaximumSize().width, containerPreferredSize.height));
        wrapper.add(container, BorderLayout.CENTER);
        return wrapper;
    }

    private static JPanel createSlotTypeSelectors() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
        container.setAlignmentX(0.5f);
        container.setAlignmentY(0);

        ComponentUtilities.outfitPanelWithTitle(container,
                new Insets(1, 0, 0, 0), "Default type");

        Collection<WeaponType> types = new ArrayList<>(List.of(WeaponType.values()));
        types.remove(WeaponType.LAUNCH_BAY);

        ButtonGroup selectorButtons = new ButtonGroup();

        for (WeaponType type : types) {
            Pair<JPanel, JRadioButton> containedButton = SlotCreationPane.createSlotTypeButton(type);
            container.add(containedButton.getFirst());
            JRadioButton radioButton = containedButton.getSecond();
            selectorButtons.add(radioButton);

            if (type == defaultType) {
                radioButton.setSelected(true);
            }
        }
        container.add(Box.createRigidArea(UIConstants.PADDING_10_4));
        return container;
    }

    private static JPanel createSlotMountSelectors() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
        container.setAlignmentX(0.5f);
        container.setAlignmentY(0);

        ComponentUtilities.outfitPanelWithTitle(container,
                new Insets(1, 0, 0, 0), "Default mount");

        Iterable<WeaponMount> mounts = new ArrayList<>(List.of(WeaponMount.values()));

        ButtonGroup selectorButtons = new ButtonGroup();

        for (WeaponMount mount : mounts) {
            JRadioButton button = new JRadioButton(mount.getDisplayName());
            button.addActionListener(e -> defaultMount = mount);

            container.add(SlotCreationPane.createSlotKindPane(selectorButtons, button));

            if (mount == defaultMount) {
                button.setSelected(true);
            }
        }

        return container;
    }

    private static JPanel createSlotSizeSelectors() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
        container.setAlignmentX(0.5f);
        container.setAlignmentY(0);

        ComponentUtilities.outfitPanelWithTitle(container,
                new Insets(1, 0, 0, 0), "Default size");

        Iterable<WeaponSize> sizes = new ArrayList<>(List.of(WeaponSize.values()));

        ButtonGroup selectorButtons = new ButtonGroup();

        for (WeaponSize size : sizes) {
            JRadioButton button = new JRadioButton(size.getDisplayedName());
            button.addActionListener(e -> defaultSize = size);

            container.add(SlotCreationPane.createSlotKindPane(selectorButtons, button));

            if (size == defaultSize) {
                button.setSelected(true);
            }
        }

        return container;
    }

    private static JPanel createSlotKindPane(ButtonGroup selectorButtons, JRadioButton button) {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(new EmptyBorder(4, 4, 0, 6));

        ComponentUtilities.layoutAsOpposites(buttonPanel, button, new JLabel(""), 0);

        selectorButtons.add(button);
        return buttonPanel;
    }

    private static Pair<JPanel, JRadioButton> createSlotTypeButton(WeaponType type) {
        JRadioButton button = new JRadioButton(type.getDisplayedName());
        button.addActionListener(e -> defaultType = type);

        JPanel panel = ComponentUtilities.createColorPropertyPanel(button, type.getColor(), 0, null);
        panel.setBorder(new EmptyBorder(4, 4, 0, 6));
        return new Pair<>(panel, button);
    }

}
