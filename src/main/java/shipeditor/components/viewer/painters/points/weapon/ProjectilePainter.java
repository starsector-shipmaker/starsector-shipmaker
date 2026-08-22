package shipeditor.components.viewer.painters.points.weapon;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import shipeditor.utility.graphics.opengl.OpenGLPainter;
import shipeditor.utility.graphics.opengl.SpriteRenderer;
import shipeditor.utility.graphics.opengl.ShapeRenderer;
import org.joml.Matrix4f;
import lombok.Getter;
import lombok.Setter;
import shipeditor.utility.objects.Size2D;
import shipeditor.utility.graphics.Sprite;

import java.awt.geom.Point2D;

@Getter @Setter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class ProjectilePainter implements OpenGLPainter {

    private final Sprite projectileSprite;

    private final Point2D projectileCenter;

    private Point2D paintAnchor;

    private float spriteOpacity = 1.0f;

    private double rotationRadians;

    private final Size2D spriteDimensions;

    public ProjectilePainter(Sprite sprite, Point2D center, Size2D size) {
        this.projectileSprite = sprite;
        this.projectileCenter = center;
        this.spriteDimensions = size;
    }

    public ProjectilePainter(ProjectilePainter other) {
        this.projectileSprite = other.projectileSprite;
        this.projectileCenter = other.projectileCenter;
        this.spriteDimensions = other.spriteDimensions;
        this.paintAnchor = other.paintAnchor;
        this.spriteOpacity = other.spriteOpacity;
        this.rotationRadians = other.rotationRadians;
    }

    @Override
    public void paint(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view) {
        if (projectileSprite == null) return;
        int textureId = projectileSprite.getTextureId();
        if (textureId == 0) return;

        double spriteWidth = projectileSprite.getImage().getWidth();
        double spriteHeight = projectileSprite.getImage().getHeight();

        double x = paintAnchor.getX() - projectileCenter.getX();
        double y = paintAnchor.getY() - (spriteHeight - projectileCenter.getY());

        org.joml.Vector2f position = new org.joml.Vector2f((float) x, (float) y);
        org.joml.Vector2f size = new org.joml.Vector2f((float) spriteWidth, (float) spriteHeight);
        org.joml.Vector2f rotationAnchor = new org.joml.Vector2f((float) paintAnchor.getX(), (float) paintAnchor.getY());
        float rotation = (float) this.getRotationRadians();
        float opacity = this.getSpriteOpacity();
        org.joml.Vector4f color = new org.joml.Vector4f(1.0f, 1.0f, 1.0f, opacity);

        spriteRenderer.drawSprite(textureId, position, size, rotationAnchor, rotation, color, projection, view);
    }

}
