package com.lin1000.justdance.gamepanel.componentpanel;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class PianoComponent {

    private final PianoStyle style;
    private int p_off_x,p_off_y = 0;
    private final Set<Integer> pressedNotes = new HashSet<>();
    private static final int[] BLACK_KEYS = {1, 3, -1, 6, 8, 10, -1}; // C~B中黑鍵相對位置
    private int command;
    private int ch;
    private int note;
    private int velocity;

    public PianoComponent(PianoStyle style, int p_off_x, int p_off_y) {
        this.style = style;
        this.p_off_x=p_off_x;
        this.p_off_y=p_off_y;
    }

    public void noteOn(int note) {
        synchronized (pressedNotes) {
            pressedNotes.add(note);
        }
    }

    public void noteOff(int note) {
        synchronized (pressedNotes) {
            pressedNotes.remove(note);
        }
    }

    private boolean isPressed(int note) {
        synchronized (pressedNotes) {
            return pressedNotes.contains(note);
        }
    }

    private boolean isBlack(int note) {
        int mod = note % 12;
        return switch (mod) {
            case 1, 3, 6, 8, 10 -> true;
            default -> false;
        };
    }

    /**
     * X pixel position of a given MIDI note's key, in this component's own coordinate space
     * (i.e. already offset by {@code p_off_x}). Mirrors the same white/black key layout
     * {@link #draw} computes internally, exposed so callers (e.g. a falling-note renderer)
     * can align notes with the keys they land on. Out-of-range notes fall back to the
     * leftmost key.
     */
    public int xForNote(int note) {
        int baseNote = 21;
        int whiteX = p_off_x;
        int lastWhiteX = p_off_x;
        for (int n = baseNote; n <= note; n++) {
            if (!isBlack(n)) {
                lastWhiteX = whiteX;
                if (n == note) return whiteX;
                whiteX += style.whiteKeyWidth;
            } else if (n == note) {
                return lastWhiteX + style.whiteKeyWidth - (style.blackKeyWidth / 2);
            }
        }
        return p_off_x;
    }

    public void setInput(int command, int ch, int note, int velocity) {
        this.command=command;
        this.ch = ch;
        this.note = note;
        this.velocity = velocity;
    }

    public void draw(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();
        int octaves = 7; // 88 鍵為 A0(21)~C8(108) 共 88 鍵
        int baseNote = 21;

        int totalWhiteKeys = 52;
        int whiteX = p_off_x;
        int[] midiToX = new int[128]; // 音符位置對應畫面 x 座標

        // 畫白鍵
        for (int i = 0, note = baseNote; i < totalWhiteKeys && note <= 108; note++) {
            if (!isBlack(note)) {
                midiToX[note] = whiteX;
                g2.setColor(isPressed(note) ? style.whiteKeyPressedColor : style.whiteKeyColor);
                g2.fillRoundRect(whiteX, p_off_y, style.whiteKeyWidth, style.whiteKeyHeight, style.keyArc, style.keyArc);
                g2.setColor(Color.GRAY);
                g2.drawRect(whiteX, p_off_y, style.whiteKeyWidth, style.whiteKeyHeight);
                whiteX += style.whiteKeyWidth;
            }
        }

        // 畫黑鍵
        whiteX = 0;
        for (int i = 0, note = baseNote; note <= 108; note++) {
            if (!isBlack(note)) {
                whiteX = midiToX[note];
            } else {
                whiteX += style.whiteKeyWidth;
                int x = whiteX - (style.blackKeyWidth / 2);
                g2.setColor(isPressed(note) ? style.blackKeyPressedColor : style.blackKeyColor);
                g2.fillRoundRect(x, p_off_y, style.blackKeyWidth, style.blackKeyHeight, style.keyArc, style.keyArc);
            }
        }

        // 畫裝置輸入提示
        g2.drawString(String.format("command=%d ch=%d note=%d velocity=%d%n",command, ch, note, velocity), p_off_x+10, p_off_y+ style.whiteKeyHeight+10);

        g2.dispose();
    }

}
