package shipeditor.components.instrument.weapon;

import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.representation.weapon.animation.MuzzleFlashSpec;
import shipeditor.utility.components.ComponentUtilities;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.function.Supplier;

public class WeaponMuzzleFlashHandler {
    private final JTextField mfLengthEditor;
    private final JTextField mfSpreadEditor;
    private final JTextField mfParticleSizeMinEditor;
    private final JTextField mfParticleSizeRangeEditor;
    private final JTextField mfParticleDurationEditor;
    private final JTextField mfParticleCountEditor;
    private final JLabel mfParticleColorValue;
    private final JLabel colorLabel;

    private MuzzleFlashSpec getOrCreate(WeaponSpecFile spec) {
        if (spec.getMuzzleFlashSpec() == null) {
            spec.setMuzzleFlashSpec(new MuzzleFlashSpec());
        }
        return spec.getMuzzleFlashSpec();
    }

    public WeaponMuzzleFlashHandler(Supplier<Boolean> readinessChecker, Runnable onChange, Supplier<WeaponSpecFile> specSupplier) {
        mfLengthEditor = WeaponFirePanelUtilities.createDoubleField(null, readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setLength(value);
        }, onChange);

        mfSpreadEditor = WeaponFirePanelUtilities.createDoubleField(null, readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setSpread(value);
        }, onChange);

        mfParticleSizeMinEditor = WeaponFirePanelUtilities.createDoubleField(null, readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setParticleSizeMin(value);
        }, onChange);

        mfParticleSizeRangeEditor = WeaponFirePanelUtilities.createDoubleField(null, readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setParticleSizeRange(value);
        }, onChange);

        mfParticleDurationEditor = WeaponFirePanelUtilities.createDoubleField(null, readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setParticleDuration(value);
        }, onChange);

        mfParticleCountEditor = WeaponFirePanelUtilities.createIntField(null, readinessChecker, value -> {
            WeaponSpecFile spec = specSupplier.get();
            if (spec != null) getOrCreate(spec).setParticleCount(value);
        }, onChange);

        mfParticleColorValue = new JLabel();
        colorLabel = WeaponFirePanelUtilities.createColorLabel("Particle Color:", mfParticleColorValue,
                () -> {
                    WeaponSpecFile spec = specSupplier.get();
                    return (spec != null && spec.getMuzzleFlashSpec() != null) ? spec.getMuzzleFlashSpec().getParticleColor() : null;
                },
                color -> {
                    WeaponSpecFile spec = specSupplier.get();
                    if (spec != null) {
                        getOrCreate(spec).setParticleColor(color);
                        onChange.run();
                    }
                });
    }

    public int populate(JPanel panel, int startRow) {
        int row = startRow;
        mfLengthEditor.setToolTipText("Length of the muzzle flash");
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Length:"), mfLengthEditor, row++);
        mfSpreadEditor.setToolTipText("Spread of the muzzle flash in pixels/degrees");
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Spread:"), mfSpreadEditor, row++);
        mfParticleSizeMinEditor.setToolTipText("Minimum size of muzzle flash particles");
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Particle Size Min:"), mfParticleSizeMinEditor, row++);
        mfParticleSizeRangeEditor.setToolTipText("Random size added to min size for particles");
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Particle Size Range:"), mfParticleSizeRangeEditor, row++);
        mfParticleDurationEditor.setToolTipText("How long the particles last");
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Particle Duration:"), mfParticleDurationEditor, row++);
        mfParticleCountEditor.setToolTipText("Number of particles spawned per shot");
        ComponentUtilities.addLabelAndComponent(panel, new JLabel("Particle Count:"), mfParticleCountEditor, row++);
        ComponentUtilities.addLabelAndComponent(panel, colorLabel, mfParticleColorValue, row++);
        return row;
    }

    public void refresh(WeaponSpecFile spec) {
        MuzzleFlashSpec mfSpec = spec.getMuzzleFlashSpec();
        if (mfSpec != null) {
            mfLengthEditor.setText(String.valueOf(mfSpec.getLength()));
            mfSpreadEditor.setText(String.valueOf(mfSpec.getSpread()));
            mfParticleSizeMinEditor.setText(String.valueOf(mfSpec.getParticleSizeMin()));
            mfParticleSizeRangeEditor.setText(String.valueOf(mfSpec.getParticleSizeRange()));
            mfParticleDurationEditor.setText(String.valueOf(mfSpec.getParticleDuration()));
            mfParticleCountEditor.setText(String.valueOf(mfSpec.getParticleCount()));
            WeaponFirePanelUtilities.updateColorLabel(mfParticleColorValue, mfSpec.getParticleColor());
        } else {
            clear();
        }
    }

    public void clear() {
        mfLengthEditor.setText("");
        mfSpreadEditor.setText("");
        mfParticleSizeMinEditor.setText("");
        mfParticleSizeRangeEditor.setText("");
        mfParticleDurationEditor.setText("");
        mfParticleCountEditor.setText("");
        WeaponFirePanelUtilities.updateColorLabel(mfParticleColorValue, null);
    }
}
