package shipeditor.utility.graphics;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Traces sprite alpha boundaries for UI highlighting.
 * <p>
 * This class produces pixel-accurate contour outlines from a sprite's opaque pixels,
 * intended for visual selection indicators (e.g., module selection highlights in the viewer).
 * The output is <b>not</b> suitable for game data export — use {@link CollisionHullGenerator}
 * for Starsector {@code .ship} collision bounds instead.
 * <p>
 * Algorithm:
 * <ol>
 *   <li>Collect all opaque pixels (alpha &gt; threshold).</li>
 *   <li>Moore-Neighbor Tracing to extract the boundary contour.</li>
 *   <li>Ramer-Douglas-Peucker simplification to remove staircase pixel artifacts
 *       while preserving sharp features (epsilon = 1.0px).</li>
 * </ol>
 * The result is in <b>local pixel coordinates</b> {@code [0, width] × [0, height]}.
 * Callers must apply their own anchor offset and rotation transforms.
 *
 * @see CollisionHullGenerator
 */
public final class SpriteOutlineTracer {

    private static final int ALPHA_THRESHOLD = 10;

    private SpriteOutlineTracer() {
    }

    /**
     * Generates an exact contour of the sprite's opaque pixels in local pixel coordinates.
     *
     * @param image The sprite image to trace.
     * @return A list of Point2D vertices forming the contour polygon, or an empty list
     *         if the image is null or fully transparent.
     */
    public static List<Point2D> generateExactContour(BufferedImage image) {
        if (image == null) return Collections.emptyList();

        int width = image.getWidth();
        int height = image.getHeight();

        // Bulk-read all pixels in one native call (50-100× faster than per-pixel getRGB)
        int[] rgbArray = new int[width * height];
        image.getRGB(0, 0, width, height, rgbArray, 0, width);

        // Build opaque pixel grid from bulk array
        boolean[][] opaque = new boolean[height][width];
        boolean hasOpaque = false;
        for (int y = 0; y < height; y++) {
            int rowOffset = y * width;
            for (int x = 0; x < width; x++) {
                int alpha = (rgbArray[rowOffset + x] >> 24) & 0xff;
                if (alpha > ALPHA_THRESHOLD) {
                    opaque[y][x] = true;
                    hasOpaque = true;
                }
            }
        }

        if (!hasOpaque) return Collections.emptyList();

        List<int[]> contour = traceBoundary(opaque, width, height);
        if (contour.isEmpty()) return Collections.emptyList();

        // Simplify slightly to eliminate staircase pixel artifacts while preserving exact sharp features
        return simplifyPolygon(contour, 1.0);
    }

    /**
     * Moore-Neighbor boundary tracing using direct grid lookups.
     * Returns contour as list of [x, y] int pairs — no Point object allocation.
     */
    private static List<int[]> traceBoundary(boolean[][] grid, int width, int height) {
        // Find starting pixel (top-leftmost) — first true cell in row-major order
        int startX = -1;
        int startY = -1;
        outer:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (grid[y][x]) {
                    startX = x;
                    startY = y;
                    break outer;
                }
            }
        }

        if (startX < 0) return Collections.emptyList();

        List<int[]> contour = new ArrayList<>();

        // Directions: Clockwise from N
        int[] dxDir = { 0, 1, 1, 1, 0, -1, -1, -1 };
        int[] dyDir = { -1, -1, 0, 1, 1, 1, 0, -1 };

        int curX = startX;
        int curY = startY;
        int enterDir = 6; // West, since start is top-leftmost

        int secondX = -1;
        int secondY = -1;

        int safetyLimit = width * height * 2;

        while (true) {
            contour.add(new int[]{curX, curY});
            boolean found = false;

            int checkDir = enterDir;

            for (int i = 0; i < 8; i++) {
                int nx = curX + dxDir[checkDir];
                int ny = curY + dyDir[checkDir];

                if (nx >= 0 && nx < width && ny >= 0 && ny < height && grid[ny][nx]) {
                    curX = nx;
                    curY = ny;
                    enterDir = (checkDir + 5) % 8;
                    found = true;
                    break;
                }
                checkDir = (checkDir + 1) % 8;
            }

            if (!found) {
                break;
            }

            if (secondX < 0) {
                secondX = curX;
                secondY = curY;
            } else if (contour.size() > 1) {
                int[] last = contour.get(contour.size() - 1);
                if (last[0] == startX && last[1] == startY && curX == secondX && curY == secondY) {
                    contour.remove(contour.size() - 1);
                    break; // Jacob's stopping criterion met
                }
            }

            if (contour.size() > safetyLimit) break; // Infinite loop safety
        }

        return contour;
    }

    private static List<Point2D> simplifyPolygon(List<int[]> points, double epsilon) {
        if (points.size() < 3) {
            List<Point2D> res = new ArrayList<>();
            for (int[] p : points) res.add(new Point2D.Double(p[0], p[1]));
            return res;
        }

        double maxDistance = 0.0;
        int index = 0;
        int end = points.size() - 1;

        int[] first = points.get(0);
        int[] last = points.get(end);

        for (int i = 1; i < end; i++) {
            double distance = perpendicularDistance(points.get(i), first, last);
            if (distance > maxDistance) {
                index = i;
                maxDistance = distance;
            }
        }

        List<Point2D> result = new ArrayList<>();
        if (maxDistance > epsilon) {
            List<int[]> firstLine = points.subList(0, index + 1);
            List<int[]> secondLine = points.subList(index, end + 1);

            List<Point2D> firstResult = simplifyPolygon(firstLine, epsilon);
            List<Point2D> secondResult = simplifyPolygon(secondLine, epsilon);

            firstResult.remove(firstResult.size() - 1);
            result.addAll(firstResult);
            result.addAll(secondResult);
        } else {
            result.add(new Point2D.Double(first[0], first[1]));
            result.add(new Point2D.Double(last[0], last[1]));
        }

        return result;
    }

    private static double perpendicularDistance(int[] pt, int[] lineStart, int[] lineEnd) {
        double dx = lineEnd[0] - lineStart[0];
        double dy = lineEnd[1] - lineStart[1];

        if (dx == 0 && dy == 0) {
            return Math.hypot(pt[0] - lineStart[0], pt[1] - lineStart[1]);
        }

        double t = ((pt[0] - lineStart[0]) * dx + (pt[1] - lineStart[1]) * dy) / (dx * dx + dy * dy);

        if (t < 0) {
            return Math.hypot(pt[0] - lineStart[0], pt[1] - lineStart[1]);
        } else if (t > 1) {
            return Math.hypot(pt[0] - lineEnd[0], pt[1] - lineEnd[1]);
        }

        double closestX = lineStart[0] + t * dx;
        double closestY = lineStart[1] + t * dy;

        return Math.hypot(pt[0] - closestX, pt[1] - closestY);
    }
}
