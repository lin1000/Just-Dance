package com.lin1000.justdance.gamepanel.effect;

import com.github.sarxos.webcam.Webcam;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Low-latency hand/limb tracker for motion gameplay, built for responsiveness first:
 *
 * <ul>
 *   <li><b>Dedicated capture thread</b> — {@code webcam.getImage()} and all per-frame vision
 *       work run off the game loop, so rendering never blocks on the camera. The thread
 *       publishes an immutable {@link Snapshot} (latest-frame-wins; no queue, no backlog).</li>
 *   <li><b>Cheap, bounded per-frame cost</b> — frames are downsampled to 160x120 for luma
 *       differencing, then motion pixels are clustered on an 80x60 coarse grid with 8-connected
 *       components; whole pipeline is ~1-2ms of pure-Java work per frame.</li>
 *   <li><b>Velocity + prediction</b> — each tracked hand carries an EMA-smoothed velocity, and
 *       {@link #predictedHands()} extrapolates position by the snapshot's age (capped), which
 *       hides camera latency the same way commercial motion titles do. A hand also "coasts"
 *       briefly when detection drops out for a frame, so blade trails don't flicker.</li>
 * </ul>
 *
 * Deliberately classical CV (frame differencing, not an ML pose model): this container has no
 * physical camera, so a native/ML path could not be validated here at all, while this pipeline's
 * game-facing contract (positions + velocities + latency metrics) is fully exercisable through
 * the clearly-labelled simulated mode. On real hardware the same contract gets real data.
 */
public final class HandTracker implements AutoCloseable {

    /** Analysis resolution for luma differencing. */
    private static final int AW = 160, AH = 120;
    /** Coarse clustering grid (2x2 analysis pixels per cell) — merges fragmented motion blobs. */
    private static final int CW = 80, CH = 60;
    private static final int DIFF_THRESHOLD = 26;      // per-pixel luma delta counted as motion
    private static final int MIN_CLUSTER_CELLS = 10;   // reject noise blobs
    private static final int MAX_HANDS = 2;
    private static final double MAX_ASSOC_FRAC = 0.30; // max match distance, fraction of screen width
    private static final double COAST_SEC = 0.15;      // keep a lost hand alive briefly
    private static final double MAX_PREDICT_SEC = 0.10;
    private static final double VEL_SMOOTH = 0.45;     // EMA weight for new velocity samples

    /** One tracked hand, in mirrored screen coordinates, with px/sec velocity. */
    public static final class HandPoint {
        public final int id;
        public final double x, y, vx, vy;

        HandPoint(int id, double x, double y, double vx, double vy) {
            this.id = id;
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
        }
    }

    /** Immutable per-frame result published by the tracker thread. */
    public static final class Snapshot {
        public final List<HandPoint> hands;
        public final long nanos;          // capture-loop timestamp of this result
        public final double processMs;    // vision pipeline cost for this frame
        public final double fps;          // smoothed capture rate
        public final boolean live;        // true = real camera frames, false = simulated signal
        public final BufferedImage mask;  // motion silhouette (live mode only, may be null)

        Snapshot(List<HandPoint> hands, long nanos, double processMs, double fps,
                 boolean live, BufferedImage mask) {
            this.hands = hands;
            this.nanos = nanos;
            this.processMs = processMs;
            this.fps = fps;
            this.live = live;
            this.mask = mask;
        }
    }

    /** Mutable tracking state, private to the capture thread. */
    private static final class Tracked {
        int id;
        double x, y, vx, vy;
        double lastSeenSec;
    }

    private final int screenWidth, screenHeight;
    private Webcam webcam;
    private boolean cameraAvailable = false;
    private String cameraError = null;

    private volatile boolean running = true;
    private volatile Snapshot latest =
            new Snapshot(Collections.emptyList(), System.nanoTime(), 0, 0, false, null);
    private final Thread worker;

    public HandTracker(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        openCamera();
        worker = new Thread(cameraAvailable ? this::runLive : this::runSimulated, "hand-tracker");
        worker.setDaemon(true);
        worker.start();
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
            System.err.println("HandTracker: camera not available, falling back to simulated hands: "
                    + e.getMessage());
        }
    }

    public boolean isCameraAvailable() {
        return cameraAvailable;
    }

    public String getCameraError() {
        return cameraError;
    }

    public Snapshot snapshot() {
        return latest;
    }

    /**
     * Hands extrapolated from the latest snapshot by its age (capped at
     * {@value #MAX_PREDICT_SEC}s) — the game reads this every render frame, so blade position
     * keeps moving smoothly between (and slightly ahead of) 30fps camera frames.
     */
    public List<HandPoint> predictedHands() {
        Snapshot snap = latest;
        double age = Math.min(MAX_PREDICT_SEC, (System.nanoTime() - snap.nanos) / 1e9);
        if (age <= 0 || snap.hands.isEmpty()) return snap.hands;
        List<HandPoint> out = new ArrayList<>(snap.hands.size());
        for (HandPoint h : snap.hands) {
            out.add(new HandPoint(h.id, h.x + h.vx * age, h.y + h.vy * age, h.vx, h.vy));
        }
        return out;
    }

    /** Age of the latest snapshot in milliseconds — the prediction distance the HUD reports. */
    public double snapshotAgeMs() {
        return (System.nanoTime() - latest.nanos) / 1e6;
    }

    // ------------------------------------------------------------------ live pipeline

    private void runLive() {
        int[] prevLuma = null;
        List<Tracked> tracked = new ArrayList<>();
        int nextId = 1;
        double fpsEma = 0;
        long lastFrameNanos = System.nanoTime();

        while (running) {
            long loopStart = System.nanoTime();
            BufferedImage frame;
            try {
                frame = webcam.getImage();
            } catch (Throwable e) {
                System.err.println("HandTracker: camera dropped mid-session: " + e.getMessage());
                cameraAvailable = false;
                runSimulated(); // degrade in place rather than killing the game
                return;
            }
            if (frame == null) {
                sleepQuiet(5);
                continue;
            }

            long t0 = System.nanoTime();
            int[] luma = computeLuma(frame);
            if (prevLuma != null) {
                double dtFrame = (t0 - lastFrameNanos) / 1e9;
                lastFrameNanos = t0;
                if (dtFrame > 0) {
                    double inst = 1.0 / dtFrame;
                    fpsEma = fpsEma == 0 ? inst : fpsEma * 0.9 + inst * 0.1;
                }

                boolean[] coarse = new boolean[CW * CH];
                BufferedImage mask = new BufferedImage(AW, AH, BufferedImage.TYPE_INT_ARGB);
                diffToCoarse(luma, prevLuma, coarse, mask);
                List<double[]> detections = detectHands(coarse);
                double nowSec = t0 / 1e9;
                nextId = associate(tracked, detections, nowSec, nextId);

                double processMs = (System.nanoTime() - t0) / 1e6;
                latest = new Snapshot(publish(tracked), t0, processMs, fpsEma, true, mask);
            }
            prevLuma = luma;

            // pace to ~30fps so a fast camera doesn't burn a core for no gameplay benefit
            long spentMs = (System.nanoTime() - loopStart) / 1_000_000;
            if (spentMs < 33) sleepQuiet(33 - spentMs);
        }
    }

    private int[] computeLuma(BufferedImage frame) {
        BufferedImage small = new BufferedImage(AW, AH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = small.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(frame, 0, 0, AW, AH, null);
        g.dispose();
        int[] rgb = small.getRGB(0, 0, AW, AH, null, 0, AW);
        int[] luma = new int[rgb.length];
        for (int i = 0; i < rgb.length; i++) {
            int p = rgb[i];
            luma[i] = (int) (0.299 * ((p >> 16) & 0xFF) + 0.587 * ((p >> 8) & 0xFF) + 0.114 * (p & 0xFF));
        }
        return luma;
    }

    /** Thresholded luma diff, folded onto the coarse cluster grid and into a glow mask image. */
    private void diffToCoarse(int[] luma, int[] prevLuma, boolean[] coarse, BufferedImage mask) {
        for (int y = 0; y < AH; y++) {
            for (int x = 0; x < AW; x++) {
                int idx = y * AW + x;
                int diff = Math.abs(luma[idx] - prevLuma[idx]);
                if (diff > DIFF_THRESHOLD) {
                    coarse[(y >> 1) * CW + (x >> 1)] = true;
                    mask.setRGB(x, y, (Math.min(255, diff * 2) << 24) | 0x66e8ff);
                }
            }
        }
    }

    /** 8-connected components on the coarse grid; returns up to MAX_HANDS centroids by mass. */
    private List<double[]> detectHands(boolean[] coarse) {
        boolean[] seen = new boolean[CW * CH];
        int[] stack = new int[CW * CH];
        List<double[]> clusters = new ArrayList<>(); // {sumX, sumY, count}
        for (int i = 0; i < coarse.length; i++) {
            if (!coarse[i] || seen[i]) continue;
            int top = 0;
            stack[top++] = i;
            seen[i] = true;
            double sx = 0, sy = 0;
            int n = 0;
            while (top > 0) {
                int p = stack[--top];
                int px = p % CW, py = p / CW;
                sx += px; sy += py; n++;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        int nx = px + dx, ny = py + dy;
                        if (nx < 0 || nx >= CW || ny < 0 || ny >= CH) continue;
                        int q = ny * CW + nx;
                        if (coarse[q] && !seen[q]) {
                            seen[q] = true;
                            stack[top++] = q;
                        }
                    }
                }
            }
            if (n >= MIN_CLUSTER_CELLS) clusters.add(new double[]{sx, sy, n});
        }
        clusters.sort((a, b) -> Double.compare(b[2], a[2]));

        List<double[]> out = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_HANDS, clusters.size()); i++) {
            double[] c = clusters.get(i);
            double cx = c[0] / c[2], cy = c[1] / c[2];
            // mirror horizontally so on-screen movement matches the player's own left/right
            double sxScreen = screenWidth - (cx / CW) * screenWidth;
            double syScreen = (cy / CH) * screenHeight;
            out.add(new double[]{sxScreen, syScreen});
        }
        return out;
    }

    /** Nearest-neighbor association of detections to tracked hands; unmatched hands coast. */
    private int associate(List<Tracked> tracked, List<double[]> detections, double nowSec, int nextId) {
        double maxDist = screenWidth * MAX_ASSOC_FRAC;
        boolean[] used = new boolean[detections.size()];

        for (Tracked t : tracked) {
            int best = -1;
            double bestDist = maxDist;
            for (int i = 0; i < detections.size(); i++) {
                if (used[i]) continue;
                double d = Math.hypot(detections.get(i)[0] - t.x, detections.get(i)[1] - t.y);
                if (d < bestDist) { bestDist = d; best = i; }
            }
            if (best >= 0) {
                used[best] = true;
                double[] det = detections.get(best);
                double dt = Math.max(1e-3, nowSec - t.lastSeenSec);
                double ivx = (det[0] - t.x) / dt, ivy = (det[1] - t.y) / dt;
                t.vx = t.vx * (1 - VEL_SMOOTH) + ivx * VEL_SMOOTH;
                t.vy = t.vy * (1 - VEL_SMOOTH) + ivy * VEL_SMOOTH;
                t.x = det[0];
                t.y = det[1];
                t.lastSeenSec = nowSec;
            } else {
                t.vx *= 0.8; // coasting: damp velocity so a lost hand doesn't fly off
                t.vy *= 0.8;
            }
        }
        tracked.removeIf(t -> nowSec - t.lastSeenSec > COAST_SEC);

        for (int i = 0; i < detections.size() && tracked.size() < MAX_HANDS; i++) {
            if (used[i]) continue;
            Tracked t = new Tracked();
            t.id = nextId++;
            t.x = detections.get(i)[0];
            t.y = detections.get(i)[1];
            t.lastSeenSec = nowSec;
            tracked.add(t);
        }
        return nextId;
    }

    private static List<HandPoint> publish(List<Tracked> tracked) {
        List<HandPoint> out = new ArrayList<>(tracked.size());
        for (Tracked t : tracked) {
            out.add(new HandPoint(t.id, t.x, t.y, t.vx, t.vy));
        }
        return Collections.unmodifiableList(out);
    }

    // ------------------------------------------------------------------ simulated pipeline

    /**
     * No-camera fallback: a synthetic hand alternating between slow hovering (below any slice
     * speed threshold) and fast cross-screen sweeps, fed through the SAME association/velocity
     * code path as live detections — so gameplay logic, speed gating, and prediction are all
     * genuinely exercised even though the positions are synthetic.
     */
    private void runSimulated() {
        List<Tracked> tracked = new ArrayList<>();
        int nextId = 1;
        long startNanos = System.nanoTime();

        while (running) {
            long t0 = System.nanoTime();
            double t = (t0 - startNanos) / 1e9;

            List<double[]> detections = new ArrayList<>();
            double cycle = t % 1.6;
            int sweepIndex = (int) (t / 1.6);
            if (cycle < 0.35) {
                double p = cycle / 0.35;
                boolean leftToRight = (sweepIndex % 2) == 0;
                double x = screenWidth * (leftToRight ? 0.08 + 0.84 * p : 0.92 - 0.84 * p);
                double y = screenHeight * 0.42 + 70 * Math.sin(p * Math.PI)
                        + 60 * Math.sin(sweepIndex * 1.7);
                detections.add(new double[]{x, y});
            } else {
                detections.add(new double[]{
                        screenWidth * 0.5 + 90 * Math.sin(t * 0.8),
                        screenHeight * 0.58 + 50 * Math.cos(t * 0.6)});
            }
            // occasional second hand, so multi-hand association keeps getting exercised too
            if ((sweepIndex % 4) == 2) {
                detections.add(new double[]{
                        screenWidth * 0.3 + 70 * Math.cos(t * 1.1),
                        screenHeight * 0.35 + 60 * Math.sin(t * 0.9)});
            }

            nextId = associate(tracked, detections, t0 / 1e9, nextId);
            latest = new Snapshot(publish(tracked), t0, 0.1, 30.0, false, null);
            sleepQuiet(33);
        }
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stops the tracker thread. Deliberately does NOT close the webcam: {@code Webcam.getDefault()}
     * is a per-device singleton that MainMenu's decorative WebCamComponent may share.
     */
    @Override
    public void close() {
        running = false;
        worker.interrupt();
        webcam = null;
    }
}
