package shipeditor.utility.graphics;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Generates collision bounds for Starsector {@code .ship} file export.
 * <p>
 * Uses a pure Java approach:
 * <ol>
 *   <li>BFS Blob Detection to isolate the main ship body.</li>
 *   <li>Morphological dilation to bridge transparent gaps in split hulls.</li>
 *   <li>Moore-Neighbor Tracing to extract the boundary contour.</li>
 *   <li>Ramer-Douglas-Peucker algorithm to simplify the vertex count.</li>
 *   <li>Polygon insetting to tighten the hull to the visible sprite edge.</li>
 * </ol>
 * <p>
 * <b>Not for UI highlighting.</b> For visual selection outlines, use {@link SpriteOutlineTracer} instead.
 *
 * @see SpriteOutlineTracer
 */
public final class CollisionHullGenerator {

    private static final int ALPHA_THRESHOLD = 10;
    private static final double SIMPLIFICATION_EPSILON = 2.0;

    private CollisionHullGenerator() {
    }

    /**
     * Generates a concave hull of world coordinates based on the opaque pixels of the given image.
     * 
     * @param image The sprite image.
     * @param anchor The top-left anchor point of the sprite in canvas coordinates.
     * @return A list of Point2D representing the vertices of the concave hull in canvas coordinates.
     */
    public static List<Point2D> generateBounds(BufferedImage image, Point2D anchor) {
        if (image == null) return Collections.emptyList();

        int width = image.getWidth();
        int height = image.getHeight();

        // Bulk-read all pixels in one native call (50-100× faster than per-pixel getRGB)
        int[] rgbArray = new int[width * height];
        image.getRGB(0, 0, width, height, rgbArray, 0, width);

        // Build opaque pixel grid from bulk array
        boolean[][] opaque = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            int rowOffset = y * width;
            for (int x = 0; x < width; x++) {
                int alpha = (rgbArray[rowOffset + x] >> 24) & 0xff;
                opaque[y][x] = alpha > ALPHA_THRESHOLD;
            }
        }

        // 1. Dilate opaque pixels to bridge any transparent gaps (e.g. split hulls)
        boolean[][] dilated = dilateGrid(opaque, width, height, 3);

        // 2. Find the blob closest to the center of the sprite
        boolean[][] blob = findCenterBlob(dilated, width, height);
        if (blob == null) return Collections.emptyList();

        // 3. Trace boundary using Moore-Neighbor
        List<int[]> contour = traceBoundary(blob, width, height);
        if (contour.isEmpty()) return Collections.emptyList();

        // 4. Simplify with RDP
        List<Point2D> simplified = simplifyPolygon(contour, SIMPLIFICATION_EPSILON);

        // 5. Inset bounds to undo dilation and trim by an extra ~5-10% as requested
        List<Point2D> insetPoints = insetPolygon(simplified, 5.0);

        // Convert pixel coordinates (top-left origin) to canvas coordinates
        double anchorX = anchor != null ? anchor.getX() : 0.0;
        double anchorY = anchor != null ? anchor.getY() : 0.0;
        List<Point2D> canvasBounds = new ArrayList<>(insetPoints.size());
        for (Point2D p : insetPoints) {
            canvasBounds.add(new Point2D.Double(anchorX + p.getX(), anchorY + p.getY()));
        }
        return canvasBounds;
    }


    /**
     * Dilates the opaque grid by the given radius using a two-pass grid sweep.
     * No Point objects are allocated.
     */
    private static boolean[][] dilateGrid(boolean[][] opaque, int width, int height, int radius) {
        boolean[][] dilated = new boolean[height][width];
        int radiusSq = radius * radius;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!opaque[y][x]) continue;
                // Mark all cells within circular radius
                int yMin = Math.max(0, y - radius);
                int yMax = Math.min(height - 1, y + radius);
                for (int ny = yMin; ny <= yMax; ny++) {
                    int dyVal = ny - y;
                    int dySq = dyVal * dyVal;
                    int xMin = Math.max(0, x - radius);
                    int xMax = Math.min(width - 1, x + radius);
                    for (int nx = xMin; nx <= xMax; nx++) {
                        int dxVal = nx - x;
                        if (dxVal * dxVal + dySq <= radiusSq) {
                            dilated[ny][nx] = true;
                        }
                    }
                }
            }
        }
        return dilated;
    }

    /**
     * Finds the blob closest to the center of the sprite using packed-int BFS.
     * Returns a boolean[][] grid of the blob, or null if empty.
     */
    private static boolean[][] findCenterBlob(boolean[][] grid, int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2;

        // Find the pixel in the grid nearest to center
        int nearestX = -1;
        int nearestY = -1;
        long minDistSq = Long.MAX_VALUE;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!grid[y][x]) continue;
                long dx = x - centerX;
                long dy = y - centerY;
                long distSq = dx * dx + dy * dy;
                if (distSq < minDistSq) {
                    minDistSq = distSq;
                    nearestX = x;
                    nearestY = y;
                }
            }
        }

        if (nearestX < 0) return null;

        // BFS using packed int queue (y * width + x) — zero object allocation
        boolean[][] blob = new boolean[height][width];
        int[] queue = new int[width * height];
        int head = 0;
        int tail = 0;

        int startPacked = nearestY * width + nearestX;
        queue[tail++] = startPacked;
        blob[nearestY][nearestX] = true;

        while (head < tail) {
            int packed = queue[head++];
            int py = packed / width;
            int px = packed % width;

            // 8-way connectivity for ships to avoid gaps in thin diagonal structures
            for (int dy = -1; dy <= 1; dy++) {
                int ny = py + dy;
                if (ny < 0 || ny >= height) continue;
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = px + dx;
                    if (nx < 0 || nx >= width) continue;
                    if (grid[ny][nx] && !blob[ny][nx]) {
                        blob[ny][nx] = true;
                        queue[tail++] = ny * width + nx;
                    }
                }
            }
        }

        return blob;
    }

    /**
     * Moore-Neighbor boundary tracing using direct grid lookups.
     * Returns contour as list of [x, y] int pairs — no Point object allocation.
     */
    private static List<int[]> traceBoundary(boolean[][] blob, int width, int height) {
        // Find starting pixel (top-leftmost) — first true cell in row-major order
        int startX = -1;
        int startY = -1;
        outer:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (blob[y][x]) {
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

                if (nx >= 0 && nx < width && ny >= 0 && ny < height && blob[ny][nx]) {
                    curX = nx;
                    curY = ny;
                    enterDir = (checkDir + 5) % 8; // Start searching next from relative "behind-left"
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

    private static List<Point2D> insetPolygon(List<Point2D> poly, double insetAmount) {
        if (poly.size() < 3) return poly;
        List<Point2D> inset = new ArrayList<>();
        int n = poly.size();
        for (int i = 0; i < n; i++) {
            Point2D prev = poly.get((i - 1 + n) % n);
            Point2D curr = poly.get(i);
            Point2D next = poly.get((i + 1) % n);

            double dx1 = curr.getX() - prev.getX();
            double dy1 = curr.getY() - prev.getY();
            double len1 = Math.sqrt(dx1*dx1 + dy1*dy1);
            if (len1 > 0) { dx1 /= len1; dy1 /= len1; }

            double dx2 = next.getX() - curr.getX();
            double dy2 = next.getY() - curr.getY();
            double len2 = Math.sqrt(dx2*dx2 + dy2*dy2);
            if (len2 > 0) { dx2 /= len2; dy2 /= len2; }

            // Inward normals (clockwise polygon -> right side is inside)
            // Normal to (dx, dy) is (-dy, dx)
            double nx1 = -dy1; double ny1 = dx1;
            double nx2 = -dy2; double ny2 = dx2;

            // Average normal
            double nx = nx1 + nx2;
            double ny = ny1 + ny2;
            double len = Math.sqrt(nx*nx + ny*ny);
            
            if (len > 0.0001) {
                nx /= len;
                ny /= len;
                
                // Calculate correct miter length to preserve sharp corners
                double dot = nx * nx1 + ny * ny1;
                double miterLength = insetAmount;
                if (dot > 0.1) { // Avoid massive spikes for very sharp angles
                    miterLength = insetAmount / dot;
                    // Cap the miter length to prevent extreme spikes on zig-zags
                    miterLength = Math.min(miterLength, insetAmount * 3.0);
                }
                
                inset.add(new Point2D.Double(curr.getX() + nx * miterLength, curr.getY() + ny * miterLength));
            } else {
                inset.add(new Point2D.Double(curr.getX() + nx1 * insetAmount, curr.getY() + ny1 * insetAmount));
            }
        }
        return inset;
    }
}
