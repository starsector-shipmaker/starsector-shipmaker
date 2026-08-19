package shipeditor.components.instrument.weapon;

import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.function.Supplier;

public class WeaponFiringLogicHandler {
    private final JCheckBox autochargeCheckbox;
    private final JCheckBox interruptibleBurstCheckbox;
    private final JCheckBox requiresFullChargeCheckbox;
    private final JCheckBox unaffectedBySpeedBonusesCheckbox;

    public WeaponFiringLogicHandler(Supplier<Boolean> readinessChecker, Runnable onChange, Supplier<WeaponSpecFile> specSupplier) {
        autochargeCheckbox = WeaponFirePanelUtilities.createCheckBox("Autocharge", "Weapon charges automatically when not firing", readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) spec.setAutocharge(value);
        }, onChange);

        interruptibleBurstCheckbox = WeaponFirePanelUtilities.createCheckBox("Interruptible Burst", "Burst can be interrupted", readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) spec.setInterruptibleBurst(value);
        }, onChange);

        requiresFullChargeCheckbox = WeaponFirePanelUtilities.createCheckBox("Requires Full Charge", "Weapon must be fully charged to fire", readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) spec.setRequiresFullCharge(value);
        }, onChange);

        unaffectedBySpeedBonusesCheckbox = WeaponFirePanelUtilities.createCheckBox("Unaffected By Projectile Speed Bonuses", "Projectile speed is not affected by hull mods/skills", readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) spec.setUnaffectedByProjectileSpeedBonuses(value);
        }, onChange);
    }

    public int populate(JPanel panel, int startRow) {
        int row = startRow;
        ComponentUtilities.addLabelAndComponent(panel, new JLabel(), autochargeCheckbox, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel(), interruptibleBurstCheckbox, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel(), requiresFullChargeCheckbox, row++);
        ComponentUtilities.addLabelAndComponent(panel, new JLabel(), unaffectedBySpeedBonusesCheckbox, row++);
        return row;
    }

    public void refresh(WeaponSpecFile spec) {
        autochargeCheckbox.setSelected(spec.isAutocharge());
        interruptibleBurstCheckbox.setSelected(spec.isInterruptibleBurst());
        requiresFullChargeCheckbox.setSelected(spec.isRequiresFullCharge());
        unaffectedBySpeedBonusesCheckbox.setSelected(spec.isUnaffectedByProjectileSpeedBonuses());
    }

    public void clear() {
        autochargeCheckbox.setSelected(false);
        interruptibleBurstCheckbox.setSelected(false);
        requiresFullChargeCheckbox.setSelected(false);
        unaffectedBySpeedBonusesCheckbox.setSelected(false);
    }
}
