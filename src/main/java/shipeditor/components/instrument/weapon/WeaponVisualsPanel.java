package shipeditor.components.instrument.weapon;

import com.formdev.flatlaf.ui.FlatLineBorder;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.components.ComponentEvents.LayerTabUpdated;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.weapon.WeaponLayer;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.utility.components.CollapsibleSection;
import shipeditor.utility.components.ComponentUtilities;
import shipeditor.utility.components.MouseoverLabelListener;
import shipeditor.utility.graphics.ColorUtilities;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.text.StringValues;
import shipeditor.utility.themes.Themes;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Sprites and rendering panel with collapsible sections.
 * Includes turret/hardpoint sprites, render flags, colors, animation,
 * muzzle flash, smoke, and misc rendering fields.
 */
public class WeaponVisualsPanel extends AbstractWeaponPropertiesPanel {

    private WeaponLayer cachedLayer;
    private boolean readyForInput;

    // Turret sprites
    private JTextField turretSpriteEditor;
    private JTextField turretUnderSpriteEditor;
    private JTextField turretGunSpriteEditor;
    private JTextField turretGlowSpriteEditor;

    // Hardpoint sprites
    private JTextField hardpointSpriteEditor;
    private JTextField hardpointUnderSpriteEditor;
    private JTextField hardpointGunSpriteEditor;
    private JTextField hardpointGlowSpriteEditor;

    // Render flags
    private JCheckBox renderBelowWeaponsCheckbox;
    private JCheckBox renderAboveWeaponsCheckbox;
    private JCheckBox renderAdditiveCheckbox;

    // Colors
    private JLabel fringeColorValue;
    private JLabel coreColorValue;
    private JLabel glowColorValue;

    // Animation
    private JTextField numFramesEditor;
    private JTextField frameRateEditor;
    private JCheckBox alwaysAnimateCheckbox;

    // Misc
    private JTextField renderHintsEditor;
    private JTextField displayArcRadiusEditor;

    // Handlers (kept as handler objects for clean separation)
    private WeaponMuzzleFlashHandler muzzleFlashHandler;
    private WeaponSmokeHandler smokeHandler;

    public WeaponVisualsPanel() {
        super();
    }

    @Override
    protected void populateContent() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Supplier<Boolean> readinessChecker = () -> readyForInput;
        Runnable onChange = this::processChange;
        Supplier<WeaponSpecFile> specSupplier = () -> cachedLayer != null ? cachedLayer.getSpecFile() : null;

        this.add(createTurretSpritesSection());
        this.add(createHardpointSpritesSection());
        this.add(createRenderFlagsSection());
        this.add(createColorsSection());
        this.add(createAnimationSection());
        this.add(createMuzzleFlashSection(readinessChecker, onChange, specSupplier));
        this.add(createSmokeSection(readinessChecker, onChange, specSupplier));
        this.add(createMiscSection());

        clearData();
    }

    // ========== Turret Sprites ==========

    private CollapsibleSection createTurretSpritesSection() {
        JPanel content = new JPanel(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(content, new Insets(1, 0, 0, 0), "");
        int row = 0;

        turretSpriteEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null)
                cachedLayer.getSpecFile().setTurretSprite(value);
        });
        ComponentUtilities.addLabelAndComponent(content, new JLabel("Sprite:"), turretSpriteEditor, row++);

        turretUnderSpriteEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null)
                cachedLayer.getSpecFile().setTurretUnderSprite(value);
        });
        ComponentUtilities.addLabelAndComponent(content, new JLabel("Under Sprite:"), turretUnderSpriteEditor, row++);

        turretGunSpriteEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null)
                cachedLayer.getSpecFile().setTurretGunSprite(value);
        });
        ComponentUtilities.addLabelAndComponent(content, new JLabel("Gun Sprite:"), turretGunSpriteEditor, row++);

        turretGlowSpriteEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null)
                cachedLayer.getSpecFile().setTurretGlowSprite(value);
        });
        ComponentUtilities.addLabelAndComponent(content, new JLabel("Glow Sprite:"), turretGlowSpriteEditor, row++);

        return new CollapsibleSection("Turret Sprites", content);
    }

    // ========== Hardpoint Sprites ==========

    private CollapsibleSection createHardpointSpritesSection() {
        JPanel content = new JPanel(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(content, new Insets(1, 0, 0, 0), "");
        int row = 0;

        hardpointSpriteEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null)
                cachedLayer.getSpecFile().setHardpointSprite(value);
        });
        ComponentUtilities.addLabelAndComponent(content, new JLabel("Sprite:"), hardpointSpriteEditor, row++);

        hardpointUnderSpriteEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null)
                cachedLayer.getSpecFile().setHardpointUnderSprite(value);
        });
        ComponentUtilities.addLabelAndComponent(content, new JLabel("Under Sprite:"), hardpointUnderSpriteEditor, row++);

        hardpointGunSpriteEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null)
                cachedLayer.getSpecFile().setHardpointGunSprite(value);
        });
        ComponentUtilities.addLabelAndComponent(content, new JLabel("Gun Sprite:"), hardpointGunSpriteEditor, row++);

        hardpointGlowSpriteEditor = createTextField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null)
                cachedLayer.getSpecFile().setHardpointGlowSprite(value);
        });
        ComponentUtilities.addLabelAndComponent(content, new JLabel("Glow Sprite:"), hardpointGlowSpriteEditor, row++);

        return new CollapsibleSection("Hardpoint Sprites", content);
    }

    // ========== Render Flags ==========

    private CollapsibleSection createRenderFlagsSection() {
        JPanel content = new JPanel(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(content, new Insets(1, 0, 0, 0), "");
        int row = 0;

        renderBelowWeaponsCheckbox = createCheckBox("Render Below All Weapons", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null)
                cachedLayer.getSpecFile().setRenderBelowAllWeapons(value);
        });
        renderBelowWeaponsCheckbox.setToolTipText("Render this weapon below all other weapons (useful for decorative parts)");
        ComponentUtilities.addLabelAndComponent(content, new JLabel(), renderBelowWeaponsCheckbox, row++);

        renderAboveWeaponsCheckbox = createCheckBox("Render Above All Weapons", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null)
                cachedLayer.getSpecFile().setRenderAboveAllWeapons(value);
        });
        renderAboveWeaponsCheckbox.setToolTipText("Render this weapon above all other weapons");
        ComponentUtilities.addLabelAndComponent(content, new JLabel(), renderAboveWeaponsCheckbox, row++);

        renderAdditiveCheckbox = createCheckBox("Render Additive", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null)
                cachedLayer.getSpecFile().setRenderAdditive(value);
        });
        renderAdditiveCheckbox.setToolTipText("Render this weapon with additive blending (useful for energy weapons and glows)");
        ComponentUtilities.addLabelAndComponent(content, new JLabel(), renderAdditiveCheckbox, row++);

        return new CollapsibleSection("Render Flags", content);
    }

    // ========== Colors ==========

    private CollapsibleSection createColorsSection() {
        JPanel content = new JPanel(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(content, new Insets(1, 0, 0, 0), "");
        int row = 0;

        fringeColorValue = new JLabel();
        JLabel fringeColorLabel = createColorLabel("Fringe Color:", fringeColorValue,
                () -> cachedLayer != null ? cachedLayer.getSpecFile().getFringeColor() : null,
                color -> {
                    if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                        cachedLayer.getSpecFile().setFringeColor(color);
                        processChange();
                    }
                });
        ComponentUtilities.addLabelAndComponent(content, fringeColorLabel, fringeColorValue, row++);

        coreColorValue = new JLabel();
        JLabel coreColorLabel = createColorLabel("Core Color:", coreColorValue,
                () -> cachedLayer != null ? cachedLayer.getSpecFile().getCoreColor() : null,
                color -> {
                    if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                        cachedLayer.getSpecFile().setCoreColor(color);
                        processChange();
                    }
                });
        ComponentUtilities.addLabelAndComponent(content, coreColorLabel, coreColorValue, row++);

        glowColorValue = new JLabel();
        JLabel glowColorLabel = createColorLabel("Glow Color:", glowColorValue,
                () -> cachedLayer != null ? cachedLayer.getSpecFile().getGlowColor() : null,
                color -> {
                    if (cachedLayer != null && cachedLayer.getSpecFile() != null) {
                        cachedLayer.getSpecFile().setGlowColor(color);
                        processChange();
                    }
                });
        ComponentUtilities.addLabelAndComponent(content, glowColorLabel, glowColorValue, row++);

        return new CollapsibleSection("Colors", content);
    }

    // ========== Animation ==========

    private CollapsibleSection createAnimationSection() {
        JPanel content = new JPanel(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(content, new Insets(1, 0, 0, 0), "");
        int row = 0;

        numFramesEditor = createIntField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null)
                cachedLayer.getSpecFile().setNumFrames(value);
        });
        ComponentUtilities.addLabelAndComponent(content, new JLabel("Num Frames:"), numFramesEditor, row++);

        frameRateEditor = createIntField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null)
                cachedLayer.getSpecFile().setFrameRate(value);
        });
        ComponentUtilities.addLabelAndComponent(content, new JLabel("Frame Rate:"), frameRateEditor, row++);

        alwaysAnimateCheckbox = createCheckBox("Always Animate", value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null)
                cachedLayer.getSpecFile().setAlwaysAnimate(value);
        });
        alwaysAnimateCheckbox.setToolTipText("Always play the animation, even when not firing");
        ComponentUtilities.addLabelAndComponent(content, new JLabel(), alwaysAnimateCheckbox, row++);

        return new CollapsibleSection("Animation", content);
    }

    // ========== Muzzle Flash ==========

    private CollapsibleSection createMuzzleFlashSection(Supplier<Boolean> readinessChecker, Runnable onChange, Supplier<WeaponSpecFile> specSupplier) {
        JPanel content = new JPanel(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(content, new Insets(1, 0, 0, 0), "");

        muzzleFlashHandler = new WeaponMuzzleFlashHandler(readinessChecker, onChange, specSupplier);
        // Populate without the bold header label (the collapsible section header replaces it).
        // The handler's populate() adds its own "<b>Muzzle Flash</b>" label at row 0, so we skip it.
        // We re-populate manually to avoid the header label.
        muzzleFlashHandler.populate(content, 0);

        return new CollapsibleSection("Muzzle Flash", content, true);
    }

    // ========== Smoke ==========

    private CollapsibleSection createSmokeSection(Supplier<Boolean> readinessChecker, Runnable onChange, Supplier<WeaponSpecFile> specSupplier) {
        JPanel content = new JPanel(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(content, new Insets(1, 0, 0, 0), "");

        smokeHandler = new WeaponSmokeHandler(readinessChecker, onChange, specSupplier);
        smokeHandler.populate(content, 0);

        return new CollapsibleSection("Smoke", content, true);
    }

    // ========== Misc ==========

    private CollapsibleSection createMiscSection() {
        JPanel content = new JPanel(new GridBagLayout());
        ComponentUtilities.outfitPanelWithTitle(content, new Insets(1, 0, 0, 0), "");
        int row = 0;

        renderHintsEditor = createListField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null)
                cachedLayer.getSpecFile().setRenderHints(value);
        });
        renderHintsEditor.setToolTipText("Comma-separated render hints (e.g., RENDER_BARREL_BELOW, SUSPEND_RECOIL)");
        ComponentUtilities.addLabelAndComponent(content, new JLabel("Render Hints:"), renderHintsEditor, row++);

        displayArcRadiusEditor = createDoubleField(value -> {
            if (cachedLayer != null && cachedLayer.getSpecFile() != null)
                cachedLayer.getSpecFile().setDisplayArcRadius(value);
        });
        displayArcRadiusEditor.setToolTipText("Radius of the firing arc display (0 = default)");
        ComponentUtilities.addLabelAndComponent(content, new JLabel("Display Arc Radius:"), displayArcRadiusEditor, row++);

        return new CollapsibleSection("Misc", content, true);
    }

    // ========== Field Factory Methods ==========

    private JTextField createTextField(Consumer<String> setter) {
        JTextField textField = new JTextField();
        textField.setColumns(10);
        textField.addActionListener(e -> {
            if (readyForInput) {
                setter.accept(textField.getText());
                processChange();
            }
        });
        return textField;
    }

    private JTextField createDoubleField(Consumer<Double> setter) {
        JTextField textField = new JTextField();
        textField.setColumns(10);
        textField.addActionListener(e -> {
            if (readyForInput) {
                try {
                    setter.accept(Double.parseDouble(textField.getText()));
                    processChange();
                } catch (NumberFormatException ex) {
                    // Ignore invalid input
                }
            }
        });
        return textField;
    }

    private JTextField createIntField(Consumer<Integer> setter) {
        JTextField textField = new JTextField();
        textField.setColumns(10);
        textField.addActionListener(e -> {
            if (readyForInput) {
                try {
                    setter.accept(Integer.parseInt(textField.getText()));
                    processChange();
                } catch (NumberFormatException ex) {
                    // Ignore invalid input
                }
            }
        });
        return textField;
    }

    private JTextField createListField(Consumer<List<String>> setter) {
        JTextField textField = new JTextField();
        textField.setColumns(10);
        textField.addActionListener(e -> {
            if (readyForInput) {
                String text = textField.getText();
                if (text.isEmpty()) {
                    setter.accept(null);
                } else {
                    setter.accept(Arrays.asList(text.split("\\s*,\\s*")));
                }
                processChange();
            }
        });
        return textField;
    }

    private JCheckBox createCheckBox(String text, Consumer<Boolean> setter) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.addActionListener(e -> {
            if (readyForInput) {
                setter.accept(checkBox.isSelected());
                processChange();
            }
        });
        return checkBox;
    }

    private JLabel createColorLabel(String labelText, JLabel valueLabel, Supplier<Color> getter, Consumer<Color> setter) {
        JLabel label = new JLabel(labelText);
        label.setToolTipText(StringValues.RIGHT_CLICK_TO_CHANGE_COLOR);

        JPopupMenu colorChooserMenu = new JPopupMenu();
        JMenuItem adjustColor = new JMenuItem(StringValues.ADJUST_VALUE);
        adjustColor.addActionListener(event -> {
            Color current = getter.get();
            Color chosen = current != null ? ColorUtilities.showColorChooser(current) : ColorUtilities.showColorChooser();
            if (chosen != null) {
                setter.accept(chosen);
            }
        });
        colorChooserMenu.add(adjustColor);

        JMenuItem removeColor = new JMenuItem("Clear value");
        removeColor.addActionListener(event -> setter.accept(null));
        colorChooserMenu.add(removeColor);

        label.addMouseListener(new MouseoverLabelListener(colorChooserMenu, label));
        valueLabel.addMouseListener(new MouseoverLabelListener(colorChooserMenu, valueLabel));
        Insets insets = ComponentUtilities.createLabelInsets();
        insets.top = 1;
        label.setBorder(ComponentUtilities.createLabelSimpleBorder(insets));

        return label;
    }

    private void updateColorLabel(JLabel valueLabel, Color color) {
        if (color != null) {
            valueLabel.setIcon(ComponentUtilities.createIconFromColor(color, 10, 10));
            valueLabel.setOpaque(true);
            valueLabel.setBorder(new FlatLineBorder(new Insets(2, 2, 2, 2), Themes.getBorderColor()));
            valueLabel.setBackground(Themes.getPanelHighlightColor());
            valueLabel.setToolTipText(ColorUtilities.getColorBreakdown(color));
            valueLabel.setText(null);
        } else {
            valueLabel.setIcon(null);
            valueLabel.setOpaque(false);
            valueLabel.setBorder(new EmptyBorder(0, 2, 0, 2));
            valueLabel.setBackground(null);
            valueLabel.setToolTipText(null);
            valueLabel.setText("Not defined");
        }
        valueLabel.setForeground(Themes.getTextColor());
    }

    // ========== processChange ==========

    @Override
    protected void processChange() {
        if (cachedLayer != null) {
            EventBus.publish(new LayerTabUpdated(cachedLayer));
            StaticController.getScheduler().queueViewerRepaint();
        }
    }

    // ========== Refresh / Clear ==========

    @Override
    public void refreshContent(LayerPainter layerPainter) {
        if (layerPainter == null || !(layerPainter.getParentLayer() instanceof WeaponLayer weaponLayer)) {
            clearData();
            return;
        }
        cachedLayer = weaponLayer;
        WeaponSpecFile spec = cachedLayer.getSpecFile();
        if (spec == null) {
            clearData();
            return;
        }

        readyForInput = false;

        // Turret sprites
        turretSpriteEditor.setText(spec.getTurretSprite() != null ? spec.getTurretSprite() : "");
        turretUnderSpriteEditor.setText(spec.getTurretUnderSprite() != null ? spec.getTurretUnderSprite() : "");
        turretGunSpriteEditor.setText(spec.getTurretGunSprite() != null ? spec.getTurretGunSprite() : "");
        turretGlowSpriteEditor.setText(spec.getTurretGlowSprite() != null ? spec.getTurretGlowSprite() : "");

        // Hardpoint sprites
        hardpointSpriteEditor.setText(spec.getHardpointSprite() != null ? spec.getHardpointSprite() : "");
        hardpointUnderSpriteEditor.setText(spec.getHardpointUnderSprite() != null ? spec.getHardpointUnderSprite() : "");
        hardpointGunSpriteEditor.setText(spec.getHardpointGunSprite() != null ? spec.getHardpointGunSprite() : "");
        hardpointGlowSpriteEditor.setText(spec.getHardpointGlowSprite() != null ? spec.getHardpointGlowSprite() : "");

        // Render flags
        renderBelowWeaponsCheckbox.setSelected(spec.isRenderBelowAllWeapons());
        renderAboveWeaponsCheckbox.setSelected(spec.isRenderAboveAllWeapons());
        renderAdditiveCheckbox.setSelected(spec.isRenderAdditive());

        // Colors
        updateColorLabel(fringeColorValue, spec.getFringeColor());
        updateColorLabel(coreColorValue, spec.getCoreColor());
        updateColorLabel(glowColorValue, spec.getGlowColor());

        // Animation
        numFramesEditor.setText(String.valueOf(spec.getNumFrames()));
        frameRateEditor.setText(String.valueOf(spec.getFrameRate()));
        alwaysAnimateCheckbox.setSelected(spec.isAlwaysAnimate());

        // Muzzle flash & smoke
        muzzleFlashHandler.refresh(spec);
        smokeHandler.refresh(spec);

        // Misc
        renderHintsEditor.setText(spec.getRenderHints() != null ? String.join(", ", spec.getRenderHints()) : "");
        displayArcRadiusEditor.setText(String.valueOf(spec.getDisplayArcRadius()));

        readyForInput = true;
    }

    private void clearData() {
        readyForInput = false;

        if (turretSpriteEditor != null) turretSpriteEditor.setText("");
        if (turretUnderSpriteEditor != null) turretUnderSpriteEditor.setText("");
        if (turretGunSpriteEditor != null) turretGunSpriteEditor.setText("");
        if (turretGlowSpriteEditor != null) turretGlowSpriteEditor.setText("");

        if (hardpointSpriteEditor != null) hardpointSpriteEditor.setText("");
        if (hardpointUnderSpriteEditor != null) hardpointUnderSpriteEditor.setText("");
        if (hardpointGunSpriteEditor != null) hardpointGunSpriteEditor.setText("");
        if (hardpointGlowSpriteEditor != null) hardpointGlowSpriteEditor.setText("");

        if (renderBelowWeaponsCheckbox != null) renderBelowWeaponsCheckbox.setSelected(false);
        if (renderAboveWeaponsCheckbox != null) renderAboveWeaponsCheckbox.setSelected(false);
        if (renderAdditiveCheckbox != null) renderAdditiveCheckbox.setSelected(false);

        if (fringeColorValue != null) updateColorLabel(fringeColorValue, null);
        if (coreColorValue != null) updateColorLabel(coreColorValue, null);
        if (glowColorValue != null) updateColorLabel(glowColorValue, null);

        if (numFramesEditor != null) numFramesEditor.setText("");
        if (frameRateEditor != null) frameRateEditor.setText("");
        if (alwaysAnimateCheckbox != null) alwaysAnimateCheckbox.setSelected(false);

        if (muzzleFlashHandler != null) muzzleFlashHandler.clear();
        if (smokeHandler != null) smokeHandler.clear();

        if (renderHintsEditor != null) renderHintsEditor.setText("");
        if (displayArcRadiusEditor != null) displayArcRadiusEditor.setText("");

        cachedLayer = null;
    }
}
