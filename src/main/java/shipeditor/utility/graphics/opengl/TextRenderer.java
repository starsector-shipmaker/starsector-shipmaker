package shipeditor.utility.graphics.opengl;

import org.joml.Matrix4f;
import shipeditor.utility.graphics.DrawUtilities;
import shipeditor.utility.overseers.StaticController;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TextRenderer {

    private static final int MAX_CACHE_SIZE = 256;

    private static class TextTexture {
        int textureId;
        int width;
        int height;
    }

    private static final Map<String, TextTexture> textTextureCache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, TextTexture> eldest) {
            if (size() > MAX_CACHE_SIZE) {
                if (eldest.getValue() != null && eldest.getValue().textureId != 0) {
                    org.lwjgl.opengl.GL11.glDeleteTextures(eldest.getValue().textureId);
                }
                return true;
            }
            return false;
        }
    };

    private TextRenderer() {
    }

    private static TextTexture getOrCreateTextTexture(String text, Font font, Color color) {
        String key = text + "_" + color.getRGB() + "_" + font.getName() + "_" + font.getSize();
        return textTextureCache.computeIfAbsent(key, k -> {
            BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gDummy = dummy.createGraphics();
            gDummy.setFont(font);
            FontMetrics fm = gDummy.getFontMetrics();
            int stringWidth = fm.stringWidth(text);
            int stringHeight = fm.getHeight();
            gDummy.dispose();

            int padding = 10;
            int imgWidth = stringWidth + padding * 2;
            int imgHeight = stringHeight + padding * 2;

            BufferedImage image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            GlyphVector glyphVector = font.createGlyphVector(g.getFontRenderContext(), text);
            Shape textShape = glyphVector.getOutline();

            Rectangle2D logicalBounds = glyphVector.getLogicalBounds();
            double x = padding - logicalBounds.getX();
            double y = padding - logicalBounds.getY();

            Shape textShapeTranslated = AffineTransform.getTranslateInstance(x, y).createTransformedShape(textShape);
            Shape translatedBounds = AffineTransform.getTranslateInstance(x, y).createTransformedShape(logicalBounds);

            DrawUtilities.paintOutlinedText(g, translatedBounds, textShapeTranslated, null, color);
            g.dispose();

            int textureId = TextureLoader.loadTexture(image);

            TextTexture tt = new TextTexture();
            tt.textureId = textureId;
            tt.width = imgWidth;
            tt.height = imgHeight;
            return tt;
        });
    }

    public static void drawTextGL(SpriteRenderer spriteRenderer, Matrix4f projection, String text, Font font, Color color, Point2D worldPosition) {
        drawTextGL(spriteRenderer, projection, text, font, color, worldPosition, 1.0f);
    }

    public static void drawTextGL(SpriteRenderer spriteRenderer, Matrix4f projection, String text, Font font, Color color, Point2D worldPosition, float alpha) {
        if (text == null || text.isEmpty()) return;
        TextTexture textTexture = getOrCreateTextTexture(text, font, color);
        if (textTexture == null) return;

        AffineTransform worldToScreen = StaticController.getViewer().getWorldToScreen();
        Point2D screenPosition = worldToScreen.transform(worldPosition, null);
        double anchorOffsetX = 25 + (StaticController.getZoomLevel() * 0.25);
        float drawX = (float) (screenPosition.getX() + anchorOffsetX);
        float drawY = (float) (screenPosition.getY() - textTexture.height / 2.0f);

        drawTextScreenGL(spriteRenderer, projection, textTexture, drawX, drawY, alpha);
    }

    public static void drawTextScreenGL(SpriteRenderer spriteRenderer, Matrix4f projection, String text, Font font, Color color, float drawX, float drawY) {
        drawTextScreenGL(spriteRenderer, projection, text, font, color, drawX, drawY, 1.0f);
    }

    public static void drawTextScreenGL(SpriteRenderer spriteRenderer, Matrix4f projection, String text, Font font, Color color, float drawX, float drawY, float alpha) {
        if (text == null || text.isEmpty()) return;
        TextTexture textTexture = getOrCreateTextTexture(text, font, color);
        if (textTexture == null) return;
        drawTextScreenGL(spriteRenderer, projection, textTexture, drawX, drawY, alpha);
    }

    private static void drawTextScreenGL(SpriteRenderer spriteRenderer, Matrix4f projection, TextTexture textTexture, float drawX, float drawY, float alpha) {
        org.joml.Vector2f pos = new org.joml.Vector2f(drawX, drawY);
        org.joml.Vector2f size = new org.joml.Vector2f(textTexture.width, textTexture.height);
        org.joml.Vector2f rotationAnchor = new org.joml.Vector2f(drawX + textTexture.width / 2.0f, drawY + textTexture.height / 2.0f);
        org.joml.Vector4f colorVec = new org.joml.Vector4f(1.0f, 1.0f, 1.0f, alpha);
        spriteRenderer.drawSprite(textTexture.textureId, pos, size, rotationAnchor, 0.0f, colorVec, projection, new Matrix4f());
    }
}
