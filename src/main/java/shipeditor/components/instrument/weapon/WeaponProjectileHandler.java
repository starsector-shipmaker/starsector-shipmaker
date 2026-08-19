package shipeditor.components.instrument.weapon;

import shipeditor.representation.weapon.WeaponEnums.AnimationType;
import shipeditor.representation.weapon.WeaponEnums.BarrelMode;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.function.Supplier;

public class WeaponProjectileHandler {
    private final JTextField projectileSpecIdEditor;
    private final JComboBox<BarrelMode> barrelModeSelector;
    private final JComboBox<AnimationType> animationTypeSelector;

    public WeaponProjectileHandler(Supplier<Boolean> readinessChecker, Runnable onChange, Supplier<WeaponSpecFile> specSupplier) {
        projectileSpecIdEditor = WeaponFirePanelUtilities.createTextField("ID of the projectile or beam spec (defined in projectile/beam files).", readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) spec.setProjectileSpecId(value);
        }, onChange);

        barrelModeSelector = new JComboBox<>(BarrelMode.values());
        barrelModeSelector.setToolTipText("Determines how the weapon fires from multiple barrels: ALTERNATING, LINKED, etc.");
        barrelModeSelector.addActionListener(e -> {
            if (readinessChecker.get()) {
                WeaponSpecFile spec = specSupplier.get();
                if (spec != null) {
                    BarrelMode selected = (BarrelMode) barrelModeSelector.getSelectedItem();
                    if (spec.getBarrelMode() != selected) {
                        spec.setBarrelMode(selected);
                        onChange.run();
                    }
                }
            }
        });

        animationTypeSelector = new JComboBox<>(AnimationType.values());
        animationTypeSelector.setToolTipText("Determines the firing animation type: MUZZLE_FLASH, SMOKE, NONE, etc.");
        animationTypeSelector.addActionListener(e -> {
            if (readinessChecker.get()) {
                WeaponSpecFile spec = specSupplier.get();
                if (spec != null) {
                    AnimationType selected = (AnimationType) animationTypeSelector.getSelectedItem();
                    if (spec.getAnimationType() != selected) {
                        spec.setAnimationType(selected);
                        onChange.run();
                    }
                }
            }
        });
    }

    public int populate(JPanel panel, int startRow) {
        int row = startRow;
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Projectile Spec ID:"), projectileSpecIdEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Barrel Mode:"), barrelModeSelector, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Animation Type:"), animationTypeSelector, row++);
        return row;
    }

    public void refresh(WeaponSpecFile spec) {
        projectileSpecIdEditor.setText(spec.getProjectileSpecId() != null ? spec.getProjectileSpecId() : "");
        barrelModeSelector.setSelectedItem(spec.getBarrelMode());
        animationTypeSelector.setSelectedItem(spec.getAnimationType());
    }

    public void clear() {
        projectileSpecIdEditor.setText("");
        barrelModeSelector.setSelectedItem(null);
        animationTypeSelector.setSelectedItem(null);
    }
}
