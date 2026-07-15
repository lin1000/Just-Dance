package com.lin1000.justdance.gamepanel.effect;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Camera-controlled fruit-slicing game (attract/arcade mode, controlFlow==8): swing an arm to
 * slice fruit flying up in parabolic arcs — the acceptance bar is the responsiveness class of
 * commercial motion-console slicing games. All hand input comes from {@link HandTracker}
 * (async capture, velocity tracking, motion prediction); this class owns only gameplay:
 * fruit physics, blade trails, swipe-speed-gated slice detection, combo scoring, bombs, and a
 * live latency HUD (capture fps / vision ms / prediction ms) so responsiveness is measurable,
 * not just claimed.
 *
 * <p>Slicing requires real swipe velocity (a hovering hand cuts nothing) — the speed gate is
 * what makes hits feel deliberate instead of accidental. Original visuals throughout: generic
 * fruit rendered in Java2D, no third-party game assets or branding.
 */
public class MotionSliceGame {

    private static final double GRAVITY_FRAC = 0.85;    // gravity as a fraction of screen height/s^2
    private static final double SLICE_SPEED_FRAC = 0.60; // min swipe speed, fraction of screen width/s
    private static final double TRAIL_SEC = 0.22;
    private static final double COMBO_WINDOW_SEC = 0.45;
    private static final int MAX_FRUITS = 12;

    private final int width, height;
    private final double gravity;
    private final double sliceMinSpeed;
    private final HandTracker tracker;
    private final Random rng = new Random();

    private final List<Fruit> fruits = new ArrayList<>();
    private final List<HalfPiece> halves = new ArrayList<>();
    private final List<Juice> juices = new ArrayList<>();
    private final List<Popup> popups = new ArrayList<>();
    private final Map<Integer, Deque<double[]>> trails = new HashMap<>(); // hand id -> (x, y, tSec)

    private int score = 0;
    private int sliced = 0;
    private int missed = 0;
    private final List<Double> recentSliceTimes = new ArrayList<>();
    private double bombFlash = 0;
    private double spawnTimer = 0.8;
    private double lastNowSec = -1;

    /** Fruit palette: {rind, inner} pairs. Index 0=melon 1=citrus 2=berry-red 3=kiwi-green. */
    private static final Color[][] FRUIT_COLORS = {
            {new Color(0x2e7d32), new Color(0xff5f6d)},
            {new Color(0xef6c00), new Color(0xffc046)},
            {new Color(0xc62828), new Color(0xfff3c4)},
            {new Color(0x6d4c41), new Color(0x9ccc65)},
    };

    private static final class Fruit {
        double x, y, vx, vy, r, angle, spin;
        int kind;
        boolean bomb;
    }

    private static final class HalfPiece {
        double x, y, vx, vy, r, angle, spin, age, life;
        int kind;
    }

    private static final class Juice {
        double x, y, vx, vy, age, life;
        Color c;
    }

    private static final class Popup {
        double x, y, age, life;
        String text;
        Color c;
    }

    public MotionSliceGame(int width, int height) {
        this.width = width;
        this.height = height;
        this.gravity = height * GRAVITY_FRAC;
        this.sliceMinSpeed = width * SLICE_SPEED_FRAC;
        this.tracker = new HandTracker(width, height);
    }

    public boolean isCameraAvailable() {
        return tracker.isCameraAvailable();
    }

    public int getScore() {
        return score;
    }

    public void close() {
        tracker.close();
    }

    // ------------------------------------------------------------------ simulation

    public void tick(double nowSec) {
        double dt = lastNowSec < 0 ? 1.0 / 60 : Math.min(0.05, Math.max(0, nowSec - lastNowSec));
        lastNowSec = nowSec;

        spawnTimer -= dt;
        if (spawnTimer <= 0 && fruits.size() < MAX_FRUITS) {
            int burst = rng.nextDouble() < 0.22 ? 2 + rng.nextInt(2) : 1;
            for (int i = 0; i < burst && fruits.size() < MAX_FRUITS; i++) {
                spawnFruit();
            }
            spawnTimer = 0.75 + rng.nextDouble() * 0.6;
        }

        for (Iterator<Fruit> it = fruits.iterator(); it.hasNext(); ) {
            Fruit f = it.next();
            f.vy += gravity * dt;
            f.x += f.vx * dt;
            f.y += f.vy * dt;
            f.angle += f.spin * dt;
            if (f.y - f.r > height && f.vy > 0) {
                it.remove();
                if (!f.bomb) missed++;
            }
        }

        updateBlades(nowSec);

        for (Iterator<HalfPiece> it = halves.iterator(); it.hasNext(); ) {
            HalfPiece h = it.next();
            h.age += dt;
            if (h.age >= h.life) { it.remove(); continue; }
            h.vy += gravity * dt;
            h.x += h.vx * dt;
            h.y += h.vy * dt;
            h.angle += h.spin * dt;
        }
        for (Iterator<Juice> it = juices.iterator(); it.hasNext(); ) {
            Juice j = it.next();
            j.age += dt;
            if (j.age >= j.life) { it.remove(); continue; }
            j.vy += gravity * 0.6 * dt;
            j.x += j.vx * dt;
            j.y += j.vy * dt;
        }
        for (Iterator<Popup> it = popups.iterator(); it.hasNext(); ) {
            Popup p = it.next();
            p.age += dt;
            if (p.age >= p.life) it.remove();
        }

        recentSliceTimes.removeIf(t -> nowSec - t > COMBO_WINDOW_SEC);
        bombFlash = Math.max(0, bombFlash - dt * 2.2);
    }

    private void spawnFruit() {
        Fruit f = new Fruit();
        f.r = height * (0.045 + rng.nextDouble() * 0.02);
        f.x = width * (0.15 + rng.nextDouble() * 0.7);
        // launch speed chosen so the arc peaks between ~35% and ~75% of screen height
        double peak = height * (0.35 + rng.nextDouble() * 0.40);
        f.vy = -Math.sqrt(2 * gravity * peak);
        f.vx = (rng.nextDouble() - 0.5) * width * 0.22;
        // keep the arc on screen: bias horizontal velocity back toward the center
        if (f.x < width * 0.3) f.vx = Math.abs(f.vx);
        if (f.x > width * 0.7) f.vx = -Math.abs(f.vx);
        f.y = height + f.r;
        f.kind = rng.nextInt(FRUIT_COLORS.length);
        f.spin = (rng.nextDouble() - 0.5) * 5;
        f.bomb = rng.nextDouble() < 0.10;
        fruits.add(f);
    }

    /**
     * Reads predicted hand positions (extrapolated past camera latency by the tracker), extends
     * each hand's blade trail, and slices any fruit the newest trail segment crosses — but only
     * when the segment's own speed clears the swipe gate.
     */
    private void updateBlades(double nowSec) {
        List<HandTracker.HandPoint> hands = tracker.predictedHands();

        // prune trails of hands that vanished
        trails.keySet().removeIf(id -> hands.stream().noneMatch(h -> h.id == id));

        for (HandTracker.HandPoint hand : hands) {
            Deque<double[]> trail = trails.computeIfAbsent(hand.id, k -> new ArrayDeque<>());
            double[] prev = trail.peekLast();
            trail.addLast(new double[]{hand.x, hand.y, nowSec});
            while (!trail.isEmpty() && nowSec - trail.peekFirst()[2] > TRAIL_SEC) {
                trail.removeFirst();
            }
            if (prev == null) continue;

            double segDt = nowSec - prev[2];
            if (segDt <= 0) continue;
            double segSpeed = Math.hypot(hand.x - prev[0], hand.y - prev[1]) / segDt;
            if (segSpeed < sliceMinSpeed) continue;

            double cutAngle = Math.atan2(hand.y - prev[1], hand.x - prev[0]);
            for (Iterator<Fruit> it = fruits.iterator(); it.hasNext(); ) {
                Fruit f = it.next();
                if (segmentCircleDistance(prev[0], prev[1], hand.x, hand.y, f.x, f.y) <= f.r) {
                    it.remove();
                    slice(f, cutAngle, nowSec);
                }
            }
        }
    }

    private void slice(Fruit f, double cutAngle, double nowSec) {
        if (f.bomb) {
            score = Math.max(0, score - 25);
            bombFlash = 1.0;
            addPopup(f.x, f.y, "-25", new Color(0xff5252));
            burstJuice(f, new Color(0x616161), 22);
            return;
        }

        sliced++;
        double perp = cutAngle + Math.PI / 2;
        for (int side = -1; side <= 1; side += 2) {
            HalfPiece h = new HalfPiece();
            h.x = f.x;
            h.y = f.y;
            h.r = f.r;
            h.kind = f.kind;
            double sep = height * (0.18 + rng.nextDouble() * 0.10);
            h.vx = f.vx * 0.6 + Math.cos(perp) * sep * side;
            h.vy = f.vy * 0.5 + Math.sin(perp) * sep * side - height * 0.08;
            h.angle = cutAngle + (side < 0 ? Math.PI : 0);
            h.spin = (rng.nextDouble() - 0.5) * 7;
            h.life = 1.5;
            halves.add(h);
        }
        burstJuice(f, FRUIT_COLORS[f.kind][1], 16);

        recentSliceTimes.add(nowSec);
        int combo = recentSliceTimes.size();
        int points = 10;
        if (combo >= 2) {
            points += 5 * (combo - 1);
            addPopup(f.x, f.y - f.r - 26, "COMBO x" + combo, new Color(0xffd54f));
        }
        score += points;
        addPopup(f.x, f.y, "+" + points, Color.WHITE);
    }

    private void burstJuice(Fruit f, Color c, int count) {
        for (int i = 0; i < count; i++) {
            Juice j = new Juice();
            j.x = f.x;
            j.y = f.y;
            double a = rng.nextDouble() * Math.PI * 2;
            double sp = height * (0.1 + rng.nextDouble() * 0.35);
            j.vx = Math.cos(a) * sp;
            j.vy = Math.sin(a) * sp - height * 0.1;
            j.life = 0.5 + rng.nextDouble() * 0.3;
            j.c = c;
            juices.add(j);
        }
    }

    private void addPopup(double x, double y, String text, Color c) {
        Popup p = new Popup();
        p.x = x;
        p.y = y;
        p.text = text;
        p.c = c;
        p.life = 0.9;
        popups.add(p);
    }

    private static double segmentCircleDistance(double x1, double y1, double x2, double y2,
                                                double cx, double cy) {
        double dx = x2 - x1, dy = y2 - y1;
        double len2 = dx * dx + dy * dy;
        double t = len2 <= 1e-9 ? 0 : Math.max(0, Math.min(1, ((cx - x1) * dx + (cy - y1) * dy) / len2));
        return Math.hypot(cx - (x1 + t * dx), cy - (y1 + t * dy));
    }

    // ------------------------------------------------------------------ rendering

    public void draw(Graphics2D gc, double nowSec) {
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GradientPaint bg = new GradientPaint(0, 0, new Color(0x101a2c), 0, height, new Color(0x060a12));
        gc.setPaint(bg);
        gc.fillRect(0, 0, width, height);

        HandTracker.Snapshot snap = tracker.snapshot();
        if (snap.mask != null) {
            drawMirroredMask(gc, snap.mask);
        }

        for (HalfPiece h : halves) {
            drawHalf(gc, h);
        }
        for (Fruit f : fruits) {
            if (f.bomb) drawBomb(gc, f, nowSec);
            else drawFruit(gc, f);
        }
        for (Juice j : juices) {
            float a = (float) Math.max(0, 1 - j.age / j.life);
            gc.setColor(new Color(j.c.getRed(), j.c.getGreen(), j.c.getBlue(), (int) (a * 220)));
            double r = 4.5 * (1 - j.age / j.life) + 1.5;
            gc.fill(new Ellipse2D.Double(j.x - r, j.y - r, r * 2, r * 2));
        }

        drawBlades(gc, nowSec);

        for (Popup p : popups) {
            float a = (float) Math.max(0, 1 - p.age / p.life);
            gc.setFont(new Font("SansSerif", Font.BOLD, 22));
            gc.setColor(new Color(p.c.getRed(), p.c.getGreen(), p.c.getBlue(), (int) (a * 255)));
            gc.drawString(p.text, (float) p.x - 20, (float) (p.y - p.age * 60));
        }

        if (bombFlash > 0) {
            gc.setColor(new Color(255, 60, 40, (int) (bombFlash * 130)));
            gc.fillRect(0, 0, width, height);
        }

        drawVignette(gc);
        drawHud(gc);
    }

    private void drawMirroredMask(Graphics2D gc, java.awt.image.BufferedImage mask) {
        AffineTransform t = new AffineTransform();
        t.translate(width, 0);
        t.scale(-(double) width / mask.getWidth(), (double) height / mask.getHeight());
        Composite old = gc.getComposite();
        gc.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
        gc.drawImage(mask, t, null);
        gc.setComposite(old);
    }

    private void drawFruit(Graphics2D gc, Fruit f) {
        Color rind = FRUIT_COLORS[f.kind][0];
        Color inner = FRUIT_COLORS[f.kind][1];
        RadialGradientPaint body = new RadialGradientPaint(
                new Point2D.Double(f.x - f.r * 0.3, f.y - f.r * 0.35), (float) (f.r * 1.5),
                new float[]{0f, 1f},
                new Color[]{lighten(rind, 0.45), rind});
        gc.setPaint(body);
        gc.fill(new Ellipse2D.Double(f.x - f.r, f.y - f.r, f.r * 2, f.r * 2));
        gc.setColor(new Color(255, 255, 255, 70));
        gc.fill(new Ellipse2D.Double(f.x - f.r * 0.55, f.y - f.r * 0.65, f.r * 0.5, f.r * 0.35));
        // stem leaf
        AffineTransform old = gc.getTransform();
        gc.translate(f.x, f.y - f.r);
        gc.rotate(f.angle * 0.2);
        gc.setColor(new Color(0x4caf50));
        gc.fill(new Ellipse2D.Double(-3, -10, 14, 8));
        gc.setTransform(old);
        // faint inner-color hint so each fruit kind reads distinctly
        gc.setColor(new Color(inner.getRed(), inner.getGreen(), inner.getBlue(), 45));
        gc.fill(new Ellipse2D.Double(f.x - f.r * 0.6, f.y - f.r * 0.6, f.r * 1.2, f.r * 1.2));
    }

    private void drawBomb(Graphics2D gc, Fruit f, double nowSec) {
        RadialGradientPaint body = new RadialGradientPaint(
                new Point2D.Double(f.x - f.r * 0.3, f.y - f.r * 0.35), (float) (f.r * 1.5),
                new float[]{0f, 1f},
                new Color[]{new Color(0x4a4a52), new Color(0x17171c)});
        gc.setPaint(body);
        gc.fill(new Ellipse2D.Double(f.x - f.r, f.y - f.r, f.r * 2, f.r * 2));
        // pulsing warning ring
        float pulse = (float) (0.5 + 0.5 * Math.sin(nowSec * 9));
        gc.setColor(new Color(255, 60, 40, (int) (90 + 120 * pulse)));
        gc.setStroke(new BasicStroke(3f));
        gc.draw(new Ellipse2D.Double(f.x - f.r - 4, f.y - f.r - 4, f.r * 2 + 8, f.r * 2 + 8));
        // fuse with spark
        gc.setColor(new Color(0x8d6e63));
        gc.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        gc.draw(new Line2D.Double(f.x, f.y - f.r, f.x + 10, f.y - f.r - 14));
        gc.setColor(new Color(255, 200, 80, (int) (150 + 105 * pulse)));
        gc.fill(new Ellipse2D.Double(f.x + 6, f.y - f.r - 19, 9, 9));
    }

    private void drawHalf(Graphics2D gc, HalfPiece h) {
        float a = (float) Math.max(0, 1 - h.age / h.life);
        Color rind = FRUIT_COLORS[h.kind][0];
        Color inner = FRUIT_COLORS[h.kind][1];
        AffineTransform old = gc.getTransform();
        gc.translate(h.x, h.y);
        gc.rotate(h.angle);
        gc.setColor(withAlpha(inner, (int) (a * 255)));
        gc.fill(new Arc2D.Double(-h.r, -h.r, h.r * 2, h.r * 2, 0, 180, Arc2D.PIE));
        gc.setColor(withAlpha(rind, (int) (a * 255)));
        gc.setStroke(new BasicStroke((float) (h.r * 0.22)));
        gc.draw(new Arc2D.Double(-h.r, -h.r, h.r * 2, h.r * 2, 0, 180, Arc2D.OPEN));
        gc.setColor(withAlpha(lighten(inner, 0.4), (int) (a * 200)));
        gc.setStroke(new BasicStroke(2f));
        gc.draw(new Line2D.Double(-h.r, 0, h.r, 0));
        gc.setTransform(old);
    }

    /** Tapered two-pass glow blade along each hand's recent trail. */
    private void drawBlades(Graphics2D gc, double nowSec) {
        for (Deque<double[]> trail : trails.values()) {
            if (trail.size() < 2) continue;
            double[][] pts = trail.toArray(new double[0][]);
            for (int i = 1; i < pts.length; i++) {
                float recency = (float) Math.max(0, 1 - (nowSec - pts[i][2]) / TRAIL_SEC);
                // outer glow
                gc.setStroke(new BasicStroke(14f * recency + 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                gc.setColor(new Color(90, 220, 255, (int) (65 * recency)));
                gc.draw(new Line2D.Double(pts[i - 1][0], pts[i - 1][1], pts[i][0], pts[i][1]));
                // bright core
                gc.setStroke(new BasicStroke(5f * recency + 1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                gc.setColor(new Color(235, 250, 255, (int) (220 * recency)));
                gc.draw(new Line2D.Double(pts[i - 1][0], pts[i - 1][1], pts[i][0], pts[i][1]));
            }
        }
    }

    private void drawVignette(Graphics2D gc) {
        RadialGradientPaint vignette = new RadialGradientPaint(
                new Point2D.Double(width * 0.5, height * 0.5), Math.max(width, height) * 0.75f,
                new float[]{0.62f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 150)});
        gc.setPaint(vignette);
        gc.fillRect(0, 0, width, height);
    }

    private void drawHud(Graphics2D gc) {
        // score, top-right
        gc.setFont(new Font("SansSerif", Font.BOLD, 34));
        String scoreText = String.valueOf(score);
        int sw = gc.getFontMetrics().stringWidth(scoreText);
        gc.setColor(new Color(255, 255, 255, 235));
        gc.drawString(scoreText, width - sw - 34, 58);
        gc.setFont(new Font("SansSerif", Font.PLAIN, 12));
        gc.setColor(new Color(255, 255, 255, 130));
        String stats = "sliced " + sliced + " · missed " + missed;
        gc.drawString(stats, width - gc.getFontMetrics().stringWidth(stats) - 34, 76);

        // tracking status + latency instrumentation, top-left under the MainMenu title line
        HandTracker.Snapshot snap = tracker.snapshot();
        gc.setFont(new Font("SansSerif", Font.BOLD, 13));
        if (snap.live) {
            gc.setColor(new Color(0x8dffb0));
            gc.drawString("LIVE CAMERA", 28, 52);
        } else {
            gc.setColor(new Color(0xffb066));
            String reason = tracker.getCameraError() == null ? "unavailable" : tracker.getCameraError();
            gc.drawString("SIMULATED — NO CAMERA DETECTED (" + reason + ")", 28, 52);
        }
        gc.setFont(new Font("SansSerif", Font.PLAIN, 12));
        gc.setColor(new Color(200, 220, 255, 170));
        gc.drawString(String.format("capture %.1f fps · vision %.1f ms · predict %.0f ms ahead",
                snap.fps, snap.processMs, tracker.snapshotAgeMs()), 28, 70);

        gc.setFont(new Font("SansSerif", Font.PLAIN, 13));
        gc.setColor(new Color(180, 200, 230, 150));
        gc.drawString("SWING YOUR ARM TO SLICE — fast swipes cut, avoid the bombs", 28, height - 24);
    }

    private static Color lighten(Color c, double f) {
        return new Color(
                (int) Math.min(255, c.getRed() + (255 - c.getRed()) * f),
                (int) Math.min(255, c.getGreen() + (255 - c.getGreen()) * f),
                (int) Math.min(255, c.getBlue() + (255 - c.getBlue()) * f));
    }

    private static Color withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }
}
