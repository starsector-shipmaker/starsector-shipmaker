package shipeditor.components.viewer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;
import shipeditor.components.viewer.layers.LayerManager;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.painters.DraggedObjectsPainter;
import shipeditor.components.viewer.painters.GuidesPainters;
import shipeditor.components.viewer.painters.HotkeyHelpPainter;
import shipeditor.components.viewer.painters.points.AbstractPointPainter;
import shipeditor.components.viewer.control.LayerViewerControls;
import shipeditor.components.viewer.painters.points.ship.MarkPointsPainter;
import shipeditor.utility.graphics.opengl.OpenGLPainter;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import shipeditor.utility.graphics.opengl.SpriteRenderer;

import javax.swing.Timer;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class PaintOrderController implements OpenGLPainter {

    private static final Matrix4f IDENTITY_MATRIX = new Matrix4f();
    private static final Vector4f BACKGROUND_COLOR = new Vector4f(1.0f);

    private final PrimaryViewer parent;

    @Getter
    private final MarkPointsPainter miscPointsPainter;

    @Getter
    private final GuidesPainters guidesPainters;

    @Getter
    private final HotkeyHelpPainter hotkeyPainter;

    @Getter @Setter
    private static boolean showBackgroundImage = true;

    @Getter @Setter
    private static boolean hideNonBuiltInWeapons = false;

    @SuppressWarnings("TypeMayBeWeakened")
    private final DraggedObjectsPainter draggedObjectsPainter = new DraggedObjectsPainter();

    @Setter
    private boolean repaintQueued;

    private Timer repaintTimer;

    PaintOrderController(PrimaryViewer viewer) {
        this.parent = viewer;

        this.miscPointsPainter = MarkPointsPainter.create();
        this.guidesPainters = new GuidesPainters(viewer);
        this.hotkeyPainter = new HotkeyHelpPainter();

        repaintTimer = new Timer(16, e -> {
            if (repaintQueued) {
                repaintViewer();
            } else {
                repaintTimer.stop();
            }
        });
        repaintTimer.setRepeats(true);
    }

    /**
     * Queues a repaint and ensures the timer is running to process it.
     */
    public void queueRepaint() {
        this.repaintQueued = true;
        if (!repaintTimer.isRunning()) {
            repaintTimer.start();
        }
    }

    private void repaintViewer() {
        this.repaintQueued = false;
        if (parent.getGlCanvas() != null && parent.getGlCanvas().isDisplayable()) {
            parent.getGlCanvas().render();
        } else {
            parent.repaint();
        }
    }

    // We pass w and h because some painters need to know the screen size
    public void paint(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view, int w, int h) {
        if (showBackgroundImage) {
            paintBackgroundImage(shapeRenderer, projection, view, w, h);
        }

        LayerManager layerManager = parent.getLayerManager();
        List<ViewerLayer> layers = layerManager.getLayers();
        for (ViewerLayer layer : layers) {
            PaintOrderController.paintLayer(spriteRenderer, shapeRenderer, projection, view, layer);
        }

        this.paintLayerDependentGuides(spriteRenderer, shapeRenderer, projection, view);

        PaintOrderController.paintIfPresent(spriteRenderer, shapeRenderer, projection, view, miscPointsPainter);

        if (ViewerDropReceiver.isDragToViewerInProgress() && parent.isCursorInViewer()) {
            draggedObjectsPainter.paint(spriteRenderer, shapeRenderer, projection, view);
        }

        PaintOrderController.paintIfPresent(spriteRenderer, shapeRenderer, projection, view, hotkeyPainter);
    }

    @Override
    public void paint(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        paint(spriteRenderer, shapeRenderer, projection, view, parent.getWidth(), parent.getHeight());
    }

    private void paintBackgroundImage(ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view, int w, int h) {
        // Checkerboard using optimized checkerboard fragment shader inside ShapeRenderer
        shapeRenderer.begin(projection, IDENTITY_MATRIX); // identity view for static screen background
        shapeRenderer.setUseCheckerboard(true);
        shapeRenderer.drawRect(0.0f, 0.0f, w, h, BACKGROUND_COLOR, true);
        shapeRenderer.setUseCheckerboard(false);
        shapeRenderer.end();
    }

    private static void paintIfPresent(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view, OpenGLPainter painter) {
        if (painter != null) {
            painter.paint(spriteRenderer, shapeRenderer, projection, view);
        }
    }

    private static boolean isGraphicsOnlyRender = false;

    public static boolean isGraphicsOnlyRender() {
        return isGraphicsOnlyRender;
    }
    public static void paintLayerGraphicsOnly(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view, ViewerLayer layer) {
        LayerPainter layerPainter = layer.getPainter();
        if (layerPainter == null) return;
        
        isGraphicsOnlyRender = true;
        try {
            layerPainter.paint(spriteRenderer, shapeRenderer, projection, view);
        } finally {
            isGraphicsOnlyRender = false;
        }
    }

    public static void paintLayer(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view, ViewerLayer layer) {
        LayerPainter layerPainter = layer.getPainter();
        if (layerPainter == null) return;

        // Apply rotation to the view matrix
        // We get the world-to-screen matrix with rotation
        // Actually, we should just let LayerPainter handle its own transformation logic using its anchor.
        
        layerPainter.paint(spriteRenderer, shapeRenderer, projection, view);

        List<AbstractPointPainter> allPainters = layerPainter.getAllPainters();
        for (AbstractPointPainter pointPainter : allPainters) {
            pointPainter.paint(spriteRenderer, shapeRenderer, projection, view);
        }
    }

    private void paintLayerDependentGuides(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        // If selected layer is rotated, guides might rotate too. We can pass a modified view matrix to guides if needed,
        // but for now we just pass the standard view matrix.
        
        PaintOrderController.paintIfPresent(spriteRenderer, shapeRenderer, projection, view, guidesPainters.getBordersPaint());
        PaintOrderController.paintIfPresent(spriteRenderer, shapeRenderer, projection, view, guidesPainters.getCenterPaint());
        if (!ViewerDropReceiver.isDragToViewerInProgress() && parent.isCursorInViewer()) {
            PaintOrderController.paintIfPresent(spriteRenderer, shapeRenderer, projection, view, guidesPainters.getGuidesPaint());
        }

        if (parent.getViewerControls() instanceof LayerViewerControls layerControls) {
            if (layerControls.isMarqueeSelectionActive()) {
                java.awt.Point start = layerControls.getMarqueeStartPoint();
                java.awt.Point end = layerControls.getMarqueeEndPoint();
                if (start != null && end != null) {
                    float x = Math.min(start.x, end.x);
                    float y = Math.min(start.y, end.y);
                    float w = Math.abs(start.x - end.x);
                    float h = Math.abs(start.y - end.y);
                    shapeRenderer.begin(projection, IDENTITY_MATRIX);
                    // Blue filled semi-transparent box
                    shapeRenderer.drawRect(x, y, w, h, new Vector4f(0.1f, 0.4f, 0.8f, 0.15f), true);
                    // Blue border outline
                    org.lwjgl.opengl.GL11.glLineWidth(1.0f);
                    shapeRenderer.drawRect(x, y, w, h, new Vector4f(0.2f, 0.5f, 0.9f, 0.8f), false);
                    shapeRenderer.end();
                }
            }
        }
    }
}
