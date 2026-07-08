package com.lin1000.justdance.gamepanel;

import com.github.strikerx3.jxinput.XInputDevice;
import com.lin1000.justdance.Project;
import com.lin1000.justdance.beats.PianoNoteProducer;
import com.lin1000.justdance.controller.ConditionController;
import com.lin1000.justdance.controller.SoundController;
import com.lin1000.justdance.gamepanel.componentpanel.PianoComponent;
import com.lin1000.justdance.gamepanel.componentpanel.PianoStyle;
import com.lin1000.justdance.gamepanel.effect.EffectManager;
import com.lin1000.justdance.gamepanel.inputdevice.PianoDanceKeyboardDeviceListener;
import com.lin1000.justdance.gamepanel.inputdevice.PianoDanceMidiDeviceListener;
import com.lin1000.justdance.song.SongLibrary;
import com.lin1000.justdance.song.Song;
import com.lin1000.justdance.song.midi.MidiChart;
import com.lin1000.justdance.song.midi.MidiChartLoader;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Transmitter;
import javax.swing.JWindow;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.File;
import java.util.List;

/**
 * Piano-mode gameplay screen — the piano-mode counterpart to {@link Dance}, sharing the same
 * {@link GameplayScreen} orchestration seam ({@link Project}'s per-round loop,
 * {@link SoundController}'s audio clock, {@link com.lin1000.justdance.controller.FPSTimerTask},
 * {@link ConditionController}) but purpose-built rather than a {@code Dance} subclass, since
 * the arrow-mode consumption layer (fixed 4-lane arrays, hardcoded pixel offsets) has nothing
 * to reuse for an 88-key note highway.
 *
 * The piano keyboard and falling notes are drawn from the song's {@link MidiChart}. Real MIDI
 * input (Phase 6) is hit-tested against {@link #producer} by {@link PianoDanceMidiDeviceListener}
 * and fed into {@link ConditionController#setCondition}, the same fire-and-forget scoring signal
 * arrow mode's {@code DanceAction} uses.
 */
public class PianoDance extends JWindow implements GameplayScreen {

    private Project project;
    public Song song;
    public int music;
    public int BPM;

    public int width = 1024, height = 768;
    public final int g_off_x = 200; // keyboard's horizontal offset, mirrors Dance's g_off_x role
    public final int g_off_y = 0;
    public int life_x = 20;
    public int life_y = 670;

    public ConditionController conditionControl;
    public final EffectManager effectManager;
    public SoundController soundController;
    public PianoComponent pianoComponent;
    public PianoNoteProducer producer;
    public PianoDanceMidiDeviceListener midiDeviceListener;

    private final int keyboardY;
    private static final double LOOKAHEAD_SEC = 3.0; // how far ahead notes become visible
    private static final double TAIL_SEC = 0.5;       // how long a passed note still renders

    private double deltaTime;
    private long deltaFrame;
    private double lastAudioSec = 0;

    public PianoDance(Project project, Song song, int whichmusic, int y_movement, int BPM,
                       XInputDevice xInputDevice, MidiDevice midiDevice, SoundController soundController,
                       GraphicsDevice activeScreen) {
        super(project);
        this.project = project;
        this.song = song;
        this.music = whichmusic;
        this.BPM = BPM;
        this.soundController = soundController;

        System.out.println("==PianoDance Constructor==");
        System.out.println("song.getName()=" + song.getName());
        System.out.println("whichmusic=" + whichmusic);
        System.out.println("BPM=" + BPM);

        if (activeScreen != null) {
            Rectangle bounds = activeScreen.getDefaultConfiguration().getBounds();
            bounds.setLocation(0, 0);
            this.setBounds(bounds);
            width = this.getWidth();
            height = this.getHeight();
        } else {
            this.setLocationRelativeTo(null);
            setSize(1024, 768);
            width = 1024;
            height = 768;
        }
        life_y = Math.min(life_y, height - 50);
        keyboardY = height - 220;

        this.setFocusable(true);
        this.addKeyListener(new PianoDanceKeyboardDeviceListener(this));
        this.setVisible(true);
        this.requestFocus();
        getContentPane().setBackground(Color.black);

        // Must exist before the window paints, same requirement as Dance.
        conditionControl = new ConditionController(this);
        conditionControl.setCondition(7);

        effectManager = new EffectManager(g_off_x, g_off_y);

        // The keyboard is always drawn regardless of whether a physical MIDI device is
        // present — unlike Dance (where the piano is a bonus decorative overlay), here it's
        // the core visual, matching how Dance always draws its 4 arrow receptors regardless
        // of whether a gamepad happens to be plugged in.
        pianoComponent = new PianoComponent(new PianoStyle(), g_off_x, keyboardY);

        MidiChart chart;
        String midiPath = SongLibrary.get(this.music).getMidiPath();
        try {
            chart = MidiChartLoader.load(new File(midiPath));
        } catch (Exception e) {
            System.err.println("PianoDance: failed to load piano chart " + midiPath + ": " + e);
            e.printStackTrace();
            chart = new MidiChart(List.of());
        }
        producer = new PianoNoteProducer(chart);

        // MIDI wiring, mirrors Dance.java's DanceMidiDeviceListener setup verbatim. Placed
        // last: MIDI can deliver on a background thread the instant setReceiver returns, and
        // the listener's send() dereferences conditionControl/effectManager/pianoComponent/
        // producer, all of which must already be assigned by this point.
        try {
            if (midiDevice != null) {
                this.midiDeviceListener = new PianoDanceMidiDeviceListener(this);
                Transmitter transmitter = midiDevice.getTransmitter();
                transmitter.setReceiver(this.midiDeviceListener);
            } else {
                System.err.println("System has NO midi devices, please use computer keyboard to play");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("System has midi devices, but cannot correctly setup. Please use computer keyboard to play");
        }
    }

    /**
     * Audio-clock-or-wallclock-fallback, shared by {@link #tick()}, {@link #paint(Graphics)},
     * and {@link PianoDanceMidiDeviceListener} so a MIDI NOTE_ON hit-tests against the same
     * "now" the renderer is using.
     */
    public double currentNowSec() {
        double nowSec = soundController.currentSec;
        if (nowSec <= 0) {
            long startNanos = soundController.getStartTimeNanos();
            nowSec = startNanos > 0 ? (System.nanoTime() - startNanos) / 1_000_000_000.0 : 0;
        }
        return nowSec;
    }

    @Override
    public void setDeltaTime(double deltaTime) { this.deltaTime = deltaTime; }

    @Override
    public void setDeltaFrame(long deltaFrame) { this.deltaFrame = deltaFrame; }

    @Override
    public void tick() {
        if (soundController == null) return;
        if (conditionControl.getGameOver() || conditionControl.getExit()) return;

        double nowSec = currentNowSec();
        double deltaSec = nowSec - lastAudioSec;
        lastAudioSec = nowSec;
        if (deltaSec <= 0 || deltaSec > 1.0) return;

        producer.sweepMisses(nowSec, conditionControl);
    }

    @Override
    public void paint(Graphics g) {
        if (soundController == null) {
            System.err.println("SoundController is null, cannot paint.");
            return;
        }
        java.awt.Image buffer = createImage(width, height);
        Graphics2D gc = (Graphics2D) buffer.getGraphics();
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        gc.setColor(Color.black);
        gc.fillRect(0, 0, width, height);

        double nowSec = currentNowSec();

        // Falling notes: each note's y is derived from the audio clock exactly like Dance's
        // arrows (y = keyboardY - secondsUntilItArrives * pxPerSec), landing on the keyboard
        // at its target time.
        double pxPerSec = (keyboardY - 40) / LOOKAHEAD_SEC;
        gc.setColor(new Color(0x5fe6ff));
        for (MidiChart.PianoNote note : producer.visibleNotes(nowSec, TAIL_SEC, LOOKAHEAD_SEC)) {
            int x = pianoComponent.xForNote(note.pitch);
            int yStart = keyboardY - (int) Math.round((note.startSec - nowSec) * pxPerSec);
            int yEnd = keyboardY - (int) Math.round((note.endSec - nowSec) * pxPerSec);
            gc.fillRoundRect(x, Math.min(yStart, yEnd), 16, Math.max(4, Math.abs(yEnd - yStart)), 6, 6);
        }

        pianoComponent.draw(gc);

        gc.setColor(Color.white);
        gc.drawString("PIANO MODE", g_off_x, 20);
        gc.drawString(song.getName(), g_off_x, 40);
        gc.drawString(String.format("Time: %.1fs", nowSec), g_off_x, 60);

        gc.setColor(Color.white);
        gc.drawString("LIFE", life_x, g_off_y + life_y);
        gc.setColor(Color.red);
        gc.drawRect(life_x, g_off_y + life_y + 10, 300, 25);
        gc.fillRect(life_x, g_off_y + life_y + 10, conditionControl.getLife() * 3, 25);

        g.drawImage(buffer, 0, 0, width, height, this);
    }

    @Override
    public ConditionController getConditionControl() { return conditionControl; }

    @Override
    public void stopGameplay() { producer.stop(); }

    @Override
    public EffectManager getEffectManager() { return effectManager; }

    @Override
    public int getLifeX() { return life_x; }

    @Override
    public int getLifeY() { return life_y; }

    @Override
    public int getOffsetY() { return g_off_y; }

    @Override
    public void removeInputDeviceListener() {
        this.removeAll();
    }
}
