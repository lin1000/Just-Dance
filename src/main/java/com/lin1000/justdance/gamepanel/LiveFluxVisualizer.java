package com.lin1000.justdance.gamepanel;

import com.lin1000.justdance.audio.FFT;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class LiveFluxVisualizer extends JPanel {
    private static final int BINS = 1024;
    private static final int HOP_SIZE = 512;
    private double[] lastMagnitudes = new double[BINS / 2];
    private double[] currentMagnitudes = new double[BINS / 2];
    private double flux = 0;

    public LiveFluxVisualizer() {
        new Thread(this::startListening).start();
    }

    private void startListening() {
        try {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            TargetDataLine line = AudioSystem.getTargetDataLine(format);
            line.open(format, BINS * 2);
            line.start();

            byte[] buffer = new byte[BINS * 2];
            double[] real = new double[BINS];
            double[] imag = new double[BINS];

            while (true) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead < BINS * 2) continue;

                for (int i = 0, j = 0; i < BINS; i++, j += 2) {
                    int low = buffer[j] & 0xFF;
                    int high = buffer[j + 1];
                    int sample = (high << 8) | low;
                    real[i] = sample / 32768.0;
                    imag[i] = 0;
                }

                FFT.fft(real, imag);
                flux = 0;
                for (int i = 0; i < BINS / 2; i++) {
                    currentMagnitudes[i] = Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
                    double diff = currentMagnitudes[i] - lastMagnitudes[i];
                    if (diff > 0) flux += diff;
                    lastMagnitudes[i] = currentMagnitudes[i];
                }
                repaint();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int w = getWidth();
        int h = getHeight();

        g2.setColor(Color.black);
        g2.fillRect(0, 0, w, h);

        g2.setColor(Color.cyan);
        for (int i = 0; i < BINS / 2; i++) {
            int barHeight = (int) (currentMagnitudes[i] * h);
            g2.fillRect(i * w / (BINS / 2), h - barHeight, w / (BINS / 2), barHeight);
        }

        g2.setColor(Color.red);
        int fluxBar = (int) (flux * 20);
        g2.fillRect(w - 50, h - fluxBar, 40, fluxBar);
        g2.drawString("Flux: " + String.format("%.3f", flux), w - 140, 20);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Live FFT Flux Visualizer");
        LiveFluxVisualizer panel = new LiveFluxVisualizer();
        frame.setContentPane(panel);
        frame.setSize(800, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
