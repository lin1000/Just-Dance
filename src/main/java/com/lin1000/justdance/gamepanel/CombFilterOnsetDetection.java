package com.lin1000.justdance.gamepanel;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CombFilterOnsetDetection extends JPanel {
    private static final int WINDOW_SIZE = 1024;
    private static final int HOP_SIZE = 512;
    private static float sampleRate;
    private static List<Double> envelope = new ArrayList<>();
    private static List<Double> onsets = new ArrayList<>();

    public static List<Double> detectOnsets(String filepath) throws Exception {
        AudioInputStream stream = AudioSystem.getAudioInputStream(new File(filepath));
        AudioFormat format = stream.getFormat();
        byte[] audioBytes = stream.readAllBytes();

        int sampleSize = format.getSampleSizeInBits() / 8;
        boolean bigEndian = format.isBigEndian();
        sampleRate = format.getSampleRate();

        double[] samples = new double[audioBytes.length / sampleSize];
        for (int i = 0; i < samples.length; i++) {
            int base = i * sampleSize;
            int low = audioBytes[base] & 0xFF;
            int high = audioBytes[base + 1];
            int sample = bigEndian ? ((high << 8) | low) : ((low) | (high << 8));
            samples[i] = sample / 32768.0;
        }

        for (int i = 0; i < samples.length - WINDOW_SIZE; i += HOP_SIZE) {
            double sum = 0;
            for (int j = 0; j < WINDOW_SIZE; j++) {
                sum += Math.abs(samples[i + j]);
            }
            envelope.add(sum / WINDOW_SIZE);
        }

        // 自動估算 combPeriod
        List<Integer> peakDistances = new ArrayList<>();
        for (int i = 1; i < envelope.size() - 1; i++) {
            if (envelope.get(i) > envelope.get(i - 1) && envelope.get(i) > envelope.get(i + 1)) {
                peakDistances.add(i);
            }
        }

        List<Integer> intervals = new ArrayList<>();
        for (int i = 1; i < peakDistances.size(); i++) {
            intervals.add(peakDistances.get(i) - peakDistances.get(i - 1));
        }

        int medianInterval = intervals.isEmpty() ? 4410 : intervals.get(intervals.size() / 2);
        Collections.sort(intervals);
        if (!intervals.isEmpty()) {
            medianInterval = intervals.get(intervals.size() / 2);
        }

        int combPeriod = medianInterval;

        double[] combFilter = new double[envelope.size()];
        for (int i = combPeriod; i < envelope.size(); i++) {
            combFilter[i] = envelope.get(i) - envelope.get(i - combPeriod);
        }

        double threshold = 0.05;
        for (int i = 1; i < combFilter.length - 1; i++) {
            if (combFilter[i] > combFilter[i - 1] && combFilter[i] > combFilter[i + 1] && combFilter[i] > threshold) {
                double time = i * HOP_SIZE / sampleRate;
                onsets.add(time);
            }
        }

        return onsets;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int w = getWidth();
        int h = getHeight();

        g2.setColor(Color.black);
        g2.fillRect(0, 0, w, h);

        g2.setColor(Color.green);
        for (int i = 0; i < envelope.size(); i++) {
            int x = (int) ((i / (float) envelope.size()) * w);
            int y = (int) (h - envelope.get(i) * h);
            g2.drawLine(x, h, x, y);
        }

        g2.setColor(Color.red);
        for (Double onset : onsets) {
            int x = (int) ((onset * sampleRate / HOP_SIZE) / envelope.size() * w);
            g2.drawLine(x, 0, x, h);
        }
    }

    public static void main(String[] args) throws Exception {
        String audioFile = "./sound/musicbox/music0.wav"; // 替換成你的 .wav 檔案路徑
        onsets = detectOnsets(audioFile);

        for (Double time : onsets) {
            System.out.printf("🔔 Onset at %.3f sec\n", time);
        }

        JFrame frame = new JFrame("Comb Filter Onset Detection");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);
        frame.setContentPane(new CombFilterOnsetDetection());
        frame.setVisible(true);
    }
}
