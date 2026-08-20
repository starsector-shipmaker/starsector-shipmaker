package shipeditor.components.viewer.entities.bays;

import shipeditor.utility.graphics.opengl.SpriteRenderer;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import org.joml.Matrix4f;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.ViewerRepaintQueued;
import shipeditor.components.ComponentEnums.EditorInstrument;
import shipeditor.components.viewer.entities.BaseWorldPoint;
import shipeditor.components.viewer.entities.weapon.SlotDrawer;
import shipeditor.components.viewer.entities.weapon.SlotPoint;
import shipeditor.components.viewer.entities.weapon.WeaponSlotOverride;
import shipeditor.components.viewer.layers.ship.ShipPainter;

import shipeditor.representation.weapon.WeaponEnums.WeaponMount;
import shipeditor.representation.weapon.WeaponEnums.WeaponSize;
import shipeditor.representation.weapon.WeaponEnums.WeaponType;


import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.List;
import shipeditor.communication.events.components.ComponentEvents.InstrumentRepaintQueued;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class LaunchPortPoint extends BaseWorldPoint implements SlotPoint {

    @Getter @Setter
    private LaunchBay parentBay;

    private SlotDrawer slotDrawer;

    public LaunchPortPoint(Point2D pointPosition, ShipPainter layer, LaunchBay bay) {
        super(pointPosition, layer);
        this.parentBay = bay;
        this.initHelper();
    }

    public String getId() {
        return parentBay.getId();
    }

    @Override
    public ShipPainter getParent() {
        return (ShipPainter) super.getParent();
    }

    @Override
    public void changeSlotID(String newId) {
        ShipPainter parent = this.getParent();
        if (!parent.isGeneratedIDUnassigned(newId)) {
            shipeditor.utility.components.dialog.DialogHelper.showDuplicateIDError();
            EventBus.publish(new ViewerRepaintQueued());
            EventBus.publish(new InstrumentRepaintQueued(EditorInstrument.LAUNCH_BAYS));
            return;
        }
        parentBay.setId(newId);
        EventBus.publish(new ViewerRepaintQueued());
        EventBus.publish(new InstrumentRepaintQueued(EditorInstrument.LAUNCH_BAYS));
    }

    @Override
    public WeaponType getWeaponType() {
        return parentBay.getWeaponType();
    }

    @Override
    public void setWeaponType(WeaponType newType) {
        throw new UnsupportedOperationException("Type change is not relevant for launch bays!");
    }

    @Override
    public WeaponMount getWeaponMount() {
        return parentBay.getWeaponMount();
    }

    @Override
    public void setWeaponMount(WeaponMount newMount) {
        parentBay.setWeaponMount(newMount);
    }

    @Override
    public WeaponSize getWeaponSize() {
        return parentBay.getWeaponSize();
    }

    @Override
    public void setWeaponSize(WeaponSize newSize) {
        parentBay.setWeaponSize(newSize);
    }

    @Override
    public double getArc() {
        return parentBay.getArc();
    }

    @Override
    public void setArc(double degrees) {
        parentBay.setArc(degrees);
    }

    @Override
    public double getAngle() {
        return parentBay.getAngle();
    }

    @Override
    public void setAngle(double degrees) {
        parentBay.setAngle(degrees);
    }

    @Override
    public int getRenderOrderMod() {
        return parentBay.getRenderOrderMod();
    }

    @Override
    public void setRenderOrderMod(int orderMod) {
        parentBay.setRenderOrderMod(orderMod);
    }

    @Override
    public WeaponSlotOverride getSkinOverride() {
        return null;
    }

    private void initHelper() {
        this.slotDrawer = new SlotDrawer(this);
        slotDrawer.setDrawAngle(false);
        slotDrawer.setDrawArc(false);
    }

    @Override
    public EditorInstrument getAssociatedMode() {
        return EditorInstrument.LAUNCH_BAYS;
    }

    @Override
    protected Color createBaseColor() {
        WeaponType type = this.parentBay.getWeaponType();
        return type.getColor();
    }

    @Override
    protected Color createSelectColor() {
        return new Color(255, 120, 0); // Orange
    }

    public String getNameForLabel() {
        return this.parentBay.getId();
    }

    @Override
    public void paint(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        slotDrawer.setPointPosition(this.getPosition());
        slotDrawer.setType(this.parentBay.getWeaponType());
        slotDrawer.setMount(this.parentBay.getWeaponMount());
        slotDrawer.setSize(this.parentBay.getWeaponSize());
        slotDrawer.setAngle(this.parentBay.getAngle());
        slotDrawer.setArc(this.parentBay.getArc());
        slotDrawer.paintSlotVisuals(spriteRenderer, shapeRenderer, projection, view);
    }


    public String getIndexToDisplay() {
        List<LaunchPortPoint> portPoints = parentBay.getPortPoints();
        return String.valueOf(portPoints.indexOf(this));
    }

}
