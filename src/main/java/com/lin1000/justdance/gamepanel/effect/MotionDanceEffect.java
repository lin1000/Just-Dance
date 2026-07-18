package com.lin1000.justdance.gamepanel.effect;

import com.github.sarxos.webcam.Webcam;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.ArrayList;
import java.util.List;

/**
 * Attract-mode motion-tracking showcase (controlFlow==7 in MainMenu): a real (not stubbed)
 * webcam frame-differencing pipeline, independent from {@link WaterDanceEffect} and
 * {@link WaterDanceParticleEffect}. Divides the camera view into vertical zones, measures
 * per-zone motion energy from consecutive-frame luma differencing, and renders a mirrored
 * silhouette-glow overlay plus a reactive equalizer/spark visualization — the same family of
 * technique EyeToy/Kinect-era motion games used, done in plain Java2D + a hand-rolled diff (not
 * OpenCV's Imgproc), since this environment has no physical camera to validate OpenCV's native
 * calls against. No scoring, no chart — this is a tracking-quality validation step before any
 * gameplay wiring.
 *
 * <p>If no webcam is available, falls back to a clearly-labelled simulated motion signal so the
 * visualization itself can still be demoed/recorded — the label is never omitted, so simulated
 * output can never be mistaken for real tracking.
 */
public class MotionDanceEffect {

    private static final int ZONES = 6;
    private static final int SAMPLE_W = 160; // downscaled analysis resolution, kept small for speed
    private static final int SAMPLE_H = 120;
    private static final int DIFF_THRESHOLD = 28; // per-pixel luma delta considered "moved" (0-255)
    private static final double ENERGY_SMOOTH = 0.35; // EMA weight for each new sample
    private static final double FLASH_TRIGGER = 0.16;  // zone energy above this can spawn a spark
    private static final double FLASH_DECAY = 0.90;
    private static final int MAX_SPARKS = 400;

    private final int width;
    private final int height;

    private Webcam webcam;
    private boolean cameraAvailable = false;
    private String cameraError = null;

    private int[] prevLuma;          // SAMPLE_W*SAMPLE_H, null until the first two frames arrive
    private BufferedImage diffMask;  // SAMPLE_W x SAMPLE_H ARGB, bright where motion was detected
    private BufferedImage lastFrame; // most recent raw camera frame, for the mirrored background

    private final double[] zoneEnergy = new double[ZONES]; // smoothed 0..1 per zone
    private final double[] zoneFlash = new double[ZONES];  // spark-burst brightness, decays each tick
    private final List<Spark> sparks = new ArrayList<>();

    private double lastNowSec = -1;

    public MotionDanceEffect(int width, int height) {
        this.width = width;
        this.height = height;
        openCamera();
    }

    private void openCamera() {
        try {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                cameraError = "no webcam detected";
                return;
            }
            if (!webcam.isOpen()) {
                Dimension[] sizes = webcam.getViewSizes();
                Dimension target = new Dimension(640, 480);
                boolean supported = false;
                for (Dimension d : sizes) {
                    if (d.equals(target)) { supported = true; break; }
                }
                webcam.setViewSize(supported || sizes.length == 0 ? target : sizes[sizes.length - 1]);
                webcam.open();
            }
            cameraAvailable = true;
        } catch (Throwable e) {
            cameraAvailable = false;
            cameraError = String.valueOf(e.getMessage());
            System.err.println("MotionDanceEffect: camera not available, falling back to simulated motion: " + e.getMessage());
        }
    }

    public void tick(double nowSec) {
        double dt = lastNowSec < 0 ? 1.0 / 60 : Math.min(0.1, Math.max(0, nowSec - lastNowSec));
        lastNowSec = nowSec;

        if (cameraAvailable) {
            BufferedImage frame = null;
            try {
                frame = webcam.getImage();
            } catch (Throwable e) {
                // camera dropped mid-session (unplugged, driver hiccup) -- degrade instead of crashing the demo
                cameraAvailable = false;
                cameraError = String.valueOf(e.getMessage());
            }
            if (frame != null) {
                lastFrame = frame;
                processFrame(frame, nowSec);
            }
        } else {
            simulateMotion(nowSec);
        }

        for (int z = 0; z < ZONES; z++) {
            zoneFlash[z] *= FLASH_DECAY;
        }
        updateSparks(dt);
    }

    private void processFrame(BufferedImage frame, double nowSec) {
        int[] luma = computeLuma(frame);
        if (diffMask == null) {
            diffMask = new BufferedImage(SAMPLE_W, SAMPLE_H, BufferedImage.TYPE_INT_ARGB);
        }
        if (prevLuma == null) {
            prevLuma = luma;
            return; // need two frames before a diff means anything
        }

        double[] zoneSum = new double[ZONES];
        int[] zoneCount = new int[ZONES];
        int zoneWidthPx = Math.max(1, SAMPLE_W / ZONES);

        for (int y = 0; y < SAMPLE_H; y++) {
            for (int x = 0; x < SAMPLE_W; x++) {
                int idx = y * SAMPLE_W + x;
                int diff = Math.abs(luma[idx] - prevLuma[idx]);
                int zone = Math.min(ZONES - 1, x / zoneWidthPx);
                zoneCount[zone]++;
                if (diff > DIFF_THRESHOLD) {
                    zoneSum[zone] += diff;
                    int alpha = Math.min(255, diff * 2);
                    diffMask.setRGB(x, y, (alpha << 24) | 0x66e8ff);
                } else {
                    diffMask.setRGB(x, y, 0);
                }
            }
        }
        diffMask = blur(diffMask);

        for (int z = 0; z < ZONES; z++) {
            double raw = zoneCount[z] > 0 ? (zoneSum[z] / zoneCount[z]) / 255.0 : 0;
            raw = Math.min(1.0, raw * 3.2); // gain so ordinary hand/arm motion visibly registers
            zoneEnergy[z] = zoneEnergy[z] * (1 - ENERGY_SMOOTH) + raw * ENERGY_SMOOTH;
            if (zoneEnergy[z] > FLASH_TRIGGER && Math.random() < 0.5) {
                spawnSpark(z);
            }
        }

        prevLuma = luma;
    }

    private void simulateMotion(double nowSec) {
        for (int z = 0; z < ZONES; z++) {
            double phase = z * 0.9;
            double wave = 0.5 + 0.5 * Math.sin(nowSec * 1.3 + phase) * Math.sin(nowSec * 0.37 + phase * 1.7);
            double raw = Math.max(0, wave - 0.35) * 1.6;
            zoneEnergy[z] = zoneEnergy[z] * (1 - ENERGY_SMOOTH) + raw * ENERGY_SMOOTH;
            if (zoneEnergy[z] > FLASH_TRIGGER && Math.random() < 0.06) {
                spawnSpark(z);
            }
        }
    }

    private static int[] computeLuma(BufferedImage frame) {
        BufferedImage small = new BufferedImage(SAMPLE_W, SAMPLE_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = small.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(frame, 0, 0, SAMPLE_W, SAMPLE_H, null);
        g.dispose();
        int[] rgb = small.getRGB(0, 0, SAMPLE_W, SAMPLE_H, null, 0, SAMPLE_W);
        int[] luma = new int[rgb.length];
        for (int i = 0; i < rgb.length; i++) {
            int p = rgb[i];
            int r = (p >> 16) & 0xFF, gr = (p >> 8) & 0xFF, b = p & 0xFF;
            luma[i] = (int) (0.299 * r + 0.587 * gr + 0.114 * b);
        }
        return luma;
    }

    private static final Kernel BLUR_KERNEL = new Kernel(3, 3, new float[]{
            1 / 16f, 2 / 16f, 1 / 16f,
            2 / 16f, 4 / 16f, 2 / 16f,
            1 / 16f, 2 / 16f, 1 / 16f
    });

    private static BufferedImage blur(BufferedImage src) {
        return new ConvolveOp(BLUR_KERNEL, ConvolveOp.EDGE_NO_OP, null).filter(src, null);
    }

    // -- sparks: a lightweight reactive accent, spawned when a zone's motion energy crosses the
    // trigger threshold, drifting up from the equalizer bar and fading out --

    private static final class Spark {
        double x, y, vx, vy;
        double age, life;
        float hue;
    }

    private void spawnSpark(int zone) {
        if (sparks.size() > MAX_SPARKS) return;
        int zoneWidthPx = width / ZONES;
        Spark s = new Spark();
        s.x = zone * zoneWidthPx + zoneWidthPx * (0.2 + 0.6 * Math.random());
        s.y = height - 90;
        s.vx = (Math.random() - 0.5) * 40;
        s.vy = -140 - Math.random() * 90;
        s.age = 0;
        s.life = 0.55 + Math.random() * 0.35;
        s.hue = (float) zone / ZONES;
        sparks.add(s);
        zoneFlash[zone] = 1.0;
    }

    private void updateSparks(double dt) {
        for (int i = sparks.size() - 1; i >= 0; i--) {
            Spark s = sparks.get(i);
            s.age += dt;
            if (s.age >= s.life) {
                sparks.remove(i);
                continue;
            }
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            s.vy += 90 * dt; // gentle gravity so sparks arc rather than fly straight up
        }
    }

    // -- rendering --

    public void draw(Graphics2D gc, double nowSec) {
        gc.setColor(Color.BLACK);
        gc.fillRect(0, 0, width, height);

        if (lastFrame != null) {
            drawMirroredCamera(gc, lastFrame, 0.55f);
        } else {
            drawIdleBackground(gc, nowSec);
        }

        if (diffMask != null) {
            drawMirroredCamera(gc, diffMask, 0.95f);
        }

        drawZoneBars(gc);
        drawSparks(gc);
        drawVignette(gc);
        drawHud(gc);
    }

    private void drawMirroredCamera(Graphics2D gc, BufferedImage img, float alpha) {
        AffineTransform t = new AffineTransform();
        t.translate(width, 0);
        t.scale(-(double) width / img.getWidth(), (double) height / img.getHeight());
        Composite old = gc.getComposite();
        gc.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        gc.drawImage(img, t, null);
        gc.setComposite(old);
    }

    private void drawIdleBackground(Graphics2D gc, double nowSec) {
        RadialGradientPaint p = new RadialGradientPaint(
                new Point2D.Double(width * 0.5, height * 0.45), Math.max(width, height) * 0.75f,
                new float[]{0f, 1f},
                new Color[]{new Color(0x142033), new Color(0x05070c)});
        gc.setPaint(p);
        gc.fillRect(0, 0, width, height);
    }

    private void drawZoneBars(Graphics2D gc) {
        int zoneWidthPx = width / ZONES;
        int floorY = height - 60;
        int maxBarHeight = 220;
        for (int z = 0; z < ZONES; z++) {
            int x = z * zoneWidthPx;
            double e = Math.min(1.0, zoneEnergy[z]);
            int barH = (int) (e * maxBarHeight);
            float glow = (float) Math.min(1.0, zoneFlash[z]);

            Color base = Color.getHSBColor((float) z / ZONES, 0.55f, 0.9f);
            Color top = blend(base, Color.WHITE, glow * 0.6f);

            GradientPaint gp = new GradientPaint(x, floorY - barH, top, x, floorY, new Color(base.getRed(), base.getGreen(), base.getBlue(), 40));
            gc.setPaint(gp);
            int pad = 10;
            gc.fill(new RoundRectangle2D.Double(x + pad, floorY - barH, zoneWidthPx - pad * 2, barH, 10, 10));

            gc.setColor(new Color(255, 255, 255, 60));
            gc.drawLine(x + pad, floorY, x + zoneWidthPx - pad, floorY);

            gc.setColor(new Color(255, 255, 255, 190));
            gc.setFont(new Font("SansSerif", Font.PLAIN, 11));
            gc.drawString(String.format("%.0f%%", e * 100), x + pad, floorY + 16);
        }
    }

    private void drawSparks(Graphics2D gc) {
        for (Spark s : sparks) {
            float t = (float) (s.age / s.life);
            float alpha = 1f - t;
            Color c = Color.getHSBColor(s.hue, 0.6f, 1f);
            gc.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (alpha * 220)));
            double r = 5 * (1 - t) + 1.5;
            gc.fill(new Ellipse2D.Double(s.x - r, s.y - r, r * 2, r * 2));
        }
    }

    private void drawVignette(Graphics2D gc) {
        RadialGradientPaint vignette = new RadialGradientPaint(
                new Point2D.Double(width * 0.5, height * 0.5), Math.max(width, height) * 0.75f,
                new float[]{0.6f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 160)});
        gc.setPaint(vignette);
        gc.fillRect(0, 0, width, height);
    }

    private void drawHud(Graphics2D gc) {
        gc.setFont(new Font("SansSerif", Font.BOLD, 13));
        if (cameraAvailable) {
            gc.setColor(new Color(0x8dffb0));
            gc.drawString("LIVE CAMERA — frame-diff motion tracking, " + ZONES + " zones", 28, 52);
        } else {
            gc.setColor(new Color(0xffb066));
            gc.drawString("SIMULATED — NO CAMERA DETECTED (" + (cameraError == null ? "unavailable" : cameraError) + ")", 28, 52);
        }
    }

    private static Color blend(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl);
    }

    /**
     * Releases nothing but this effect's own resources. Deliberately does NOT call
     * {@code webcam.close()}: {@link Webcam#getDefault()} returns the same per-device singleton
     * MainMenu's landing-screen {@code WebCamComponent} may already be holding open for its
     * decorative corner thumbnail, and closing it here would break that unrelated feature.
     */
    public void close() {
        webcam = null;
    }

    public boolean isCameraAvailable() {
        return cameraAvailable;
    }
}
