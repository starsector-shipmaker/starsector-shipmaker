package shipeditor.parsing.saving;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import shipeditor.components.viewer.layers.weapon.WeaponLayer;
import shipeditor.parsing.FileUtilities;
import shipeditor.representation.weapon.WeaponSpecFile;
import shipeditor.utility.overseers.StaticController;
import shipeditor.utility.text.StringValues;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Log4j2
final class SaveWeaponAction {

    private SaveWeaponAction() {
    }

    static void saveWeaponFromLayer(WeaponLayer weaponLayer) {
        JFileChooser fileChooser = FileUtilities.getFileChooser();

        File currentDirectory = fileChooser.getCurrentDirectory();
        WeaponSpecFile weaponSpecFile = weaponLayer.getSpecFile();
        if (weaponSpecFile == null) return;
        
        File initial = new File(currentDirectory, weaponSpecFile.getId() != null ? weaponSpecFile.getId() + ".wpn" : "new_weapon.wpn");
        fileChooser.setSelectedFile(initial);

        Path specFilePath = weaponSpecFile.getWeaponSpecFilePath();
        if (specFilePath != null) {
            File originalPath = specFilePath.toFile();
            if (originalPath.isFile()) {
                fileChooser.setSelectedFile(originalPath);
            }
        }

        int returnVal = fileChooser.showSaveDialog(shipeditor.PrimaryWindow.getInstance());
        File lastDirectory = fileChooser.getCurrentDirectory();
        FileUtilities.setLastGeneralDirectory(lastDirectory);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File result = fileChooser.getSelectedFile();
            if (!result.getName().endsWith(".wpn")) {
                result = new File(result.getParentFile(), result.getName() + ".wpn");
            }

            log.info("Commencing weapon saving: {}", result);

            ObjectMapper objectMapper = FileUtilities.getConfigured();
            String errorMessage = "Weapon file saving failed: {}";
            
            try {
                weaponSpecFile.setWeaponSpecFilePath(result.toPath());
                
                SaveWeaponAction.syncOffsetsToSpec(weaponLayer);

                objectMapper.writeValue(result, weaponSpecFile);
                
                StaticController.getViewer().getLayerManager().markSaved(weaponLayer, "weapon");
            } catch (IOException e) {
                log.error(errorMessage, result.getName(), e);
                JOptionPane.showMessageDialog(shipeditor.PrimaryWindow.getInstance(),
                        "Weapon file saving failed, exception thrown at: " + result,
                        StringValues.FILE_SAVING_ERROR,
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void syncOffsetsToSpec(WeaponLayer weaponLayer) {
        WeaponSpecFile specFile = weaponLayer.getSpecFile();
        shipeditor.components.viewer.layers.weapon.WeaponPainter painter = weaponLayer.getPainter();
        if (specFile == null || painter == null) return;

        shipeditor.components.viewer.painters.points.weapon.WeaponOffsetPainter turretPainter = null;
        shipeditor.components.viewer.painters.points.weapon.WeaponOffsetPainter hardpointPainter = null;
        for (shipeditor.components.viewer.painters.points.AbstractPointPainter pointPainter : painter.getAllPainters()) {
            if (pointPainter instanceof shipeditor.components.viewer.painters.points.weapon.WeaponOffsetPainter wop) {
                if (wop.getDesignatedType() == shipeditor.representation.weapon.WeaponEnums.WeaponMount.TURRET) turretPainter = wop;
                if (wop.getDesignatedType() == shipeditor.representation.weapon.WeaponEnums.WeaponMount.HARDPOINT) hardpointPainter = wop;
            }
        }

        if (turretPainter != null) {
            java.util.List<shipeditor.components.viewer.entities.weapon.OffsetPoint> points = turretPainter.getOffsetPoints();
            java.awt.geom.Point2D.Double[] offsets = new java.awt.geom.Point2D.Double[points.size()];
            double[] angles = new double[points.size()];
            for (int i = 0; i < points.size(); i++) {
                shipeditor.components.viewer.entities.weapon.OffsetPoint p = points.get(i);
                java.awt.geom.Point2D derotated = shipeditor.components.viewer.layers.ship.ShipPainterInitialization.derotatePointByCenter(p.getPosition(), painter.getSpecificRotationAnchor(shipeditor.representation.weapon.WeaponEnums.WeaponMount.TURRET));
                offsets[i] = new java.awt.geom.Point2D.Double(derotated.getX(), derotated.getY());
                angles[i] = p.getAngle();
            }
            specFile.setTurretOffsets(offsets);
            specFile.setTurretAngleOffsets(angles);
        }

        if (hardpointPainter != null) {
            java.util.List<shipeditor.components.viewer.entities.weapon.OffsetPoint> points = hardpointPainter.getOffsetPoints();
            java.awt.geom.Point2D.Double[] offsets = new java.awt.geom.Point2D.Double[points.size()];
            double[] angles = new double[points.size()];
            for (int i = 0; i < points.size(); i++) {
                shipeditor.components.viewer.entities.weapon.OffsetPoint p = points.get(i);
                java.awt.geom.Point2D derotated = shipeditor.components.viewer.layers.ship.ShipPainterInitialization.derotatePointByCenter(p.getPosition(), painter.getSpecificRotationAnchor(shipeditor.representation.weapon.WeaponEnums.WeaponMount.HARDPOINT));
                offsets[i] = new java.awt.geom.Point2D.Double(derotated.getX(), derotated.getY());
                angles[i] = p.getAngle();
            }
            specFile.setHardpointOffsets(offsets);
            specFile.setHardpointAngleOffsets(angles);
        }
    }
}
