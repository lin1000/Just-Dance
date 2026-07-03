package com.lin1000.justdance.gamepanel;
import com.lin1000.justdance.audio.AudioFormatAdapter;
import com.lin1000.justdance.audio.FFT;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class WaveformVisualizer extends JPanel {
    private double[] samples;
    private double[] samplessum;
    private double[][] samplesChannel;
    private byte[] audioBytes;
    private float sampleRate;
    private int channels;
    private long startTime = -1;
    private Timer repaintTimer;
    private SourceDataLine line;
    private AudioFormat format;
    private double durationSec;
    private static final int DISPLAY_SECONDS = 2;
    private DecimalFormat timeFormat = new DecimalFormat("0.0");
    private int currentPlaybackSample = 0;
    private java.util.List<Double> beatTimestamps = new ArrayList<Double>();

    public WaveformVisualizer(String filepath) throws Exception {
        AudioInputStream stream = AudioFormatAdapter.openPcm(new File(filepath));
        format = stream.getFormat();
        sampleRate = format.getSampleRate();
        channels = format.getChannels();
        audioBytes = stream.readAllBytes();

        int sampleSize = format.getSampleSizeInBits() / 8;
        boolean bigEndian = format.isBigEndian();
        int totalSamples = audioBytes.length / (sampleSize * channels);
        samples = new double[totalSamples];
        //samplessum = new double[totalSamples];
        //samplesChannel =  new double[2][totalSamples];

        for (int i = 0; i < totalSamples; i++) {
            double sum = 0;
            for (int ch = 0; ch < channels; ch++) {
                int base = (i * channels + ch) * sampleSize;
                int low = audioBytes[base] & 0xFF;
                int high = audioBytes[base + 1];
                int sample = bigEndian ? ((high << 8) | low) : ((low) | (high << 8));
                sum += sample / 32768.0;
                //samplesChannel[ch][i] = sample;
            }
            samples[i] = sum / channels;
           // samplessum[i] = sum;
        }

        //Display for understanding.
//        for(int i=0; i <samples.length; i++){//            System.out.print("sCh[0]["+i+"]="+samplesChannel[0][i]);
//            System.out.print(",sCh[1]["+i+"]="+samplesChannel[1][i]);
//            System.out.print(",samplesum["+i+"]="+samplessum[i]);
//            System.out.println(",sample["+i+"]="+samples[i]);
//        }

        durationSec = samples.length / sampleRate;

        System.out.println("samples.length="+samples.length);
        System.out.println("sampleRate="+sampleRate);
        System.out.println("frameRate="+format.getFrameRate());
        generateFluxBeats(); // 假設產生節奏點供示範

        new Thread(() -> {
            try {
                playAudio();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        repaintTimer = new Timer();
        repaintTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                repaint();
            }
        }, 0, 16);
    }

    private void generateFluxBeats() {
        int windowSize = 1024;
        double[] window = new double[windowSize];
        double[] prevSpectrum = new double[windowSize / 2];
        double threshold = 0.8;

        for (int i = 0; i < samples.length - windowSize; i += windowSize / 2) {
            for (int j = 0; j < windowSize; j++) {
                window[j] = samples[i + j];
            }
            double[] imag = new double[windowSize];
            FFT.fft(window, imag);

            double flux = 0;
            for (int b = 0; b < windowSize / 2; b++) {
                double mag = Math.sqrt(window[b] * window[b] + imag[b] * imag[b]);
                double diff = mag - prevSpectrum[b];
                flux += (diff > 0) ? diff : 0;
                prevSpectrum[b] = mag;
            }

            if (flux > threshold) {
                double time = i / sampleRate;
                beatTimestamps.add(time);
            }
        }
    }

    private void playAudio() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        startTime = System.nanoTime(); // high-resolution clock
        new Thread(() -> {
            int bufferSize = 4096;
            int written = 0;
            while (written < audioBytes.length) {
                int len = Math.min(bufferSize, audioBytes.length - written);
                line.write(audioBytes, written, len);
                written += len;
                currentPlaybackSample = written / (format.getFrameSize());
            }
            line.drain();
            line.stop();
            line.close();
        }).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int w = getWidth();
        int h = getHeight();
        g2.setColor(Color.black);
        g2.fillRect(0, 0, w, h);

        // 計算當前播放秒數
        double currentSec = currentPlaybackSample / sampleRate;
        // 或改用取得當前秒數
        double currentSecSourceDataLine = line!=null?line.getMicrosecondPosition()/1000_000d:0;
        // 或改用取得當前LongFrameposition
        long currentLongFramePositionSourceDataLine = line!=null?line.getLongFramePosition():0;
        // 取得當前之avaiable(已在緩衝區的資料量)及buffer size
        long currentAvailableSourceDataLine = line!=null?line.available():0;
        int currentBufferSize = line!=null?line.getBufferSize():0;

        // 顯示時間與樣本數資訊
        g2.setColor(Color.white);
        g2.drawString("Time: " + timeFormat.format(currentSec) + "s / " + timeFormat.format(durationSec) + "s", 10, 20);
        g2.drawString("Sample: " + currentPlaybackSample + " / " + samples.length, 10, 40);
        g2.drawString("Time: " + timeFormat.format(currentSecSourceDataLine) + "s / " + timeFormat.format(durationSec) + "s", 10, 60);
        g2.drawString("Sample: " + currentLongFramePositionSourceDataLine + " / " + samples.length, 10, 80);
        g2.drawString("available: " + currentAvailableSourceDataLine  , 10, 100);
        g2.drawString("currentBufferSize: " + currentBufferSize  , 10, 120);

        // 畫波形（避免跳動：預先平均 + 定點計算）
        g2.setColor(Color.green);
        int middle = h / 2;
        int totalVisibleSamples = (int)(DISPLAY_SECONDS * sampleRate);
        int startSample = Math.max(0, currentPlaybackSample - totalVisibleSamples / 2);
        int samplesPerPixel = Math.max(1, totalVisibleSamples / w);

        for (int x = 0; x < w; x++) {
            int index = startSample + x * samplesPerPixel;
            if (index >= samples.length) break;
            double avg = 0;
            for (int k = 0; k < samplesPerPixel && index + k < samples.length; k++) {//FOR EACH GROUPING OF Samples [k0,k1...kn]
                avg += samples[index + k];
            }
            avg /= samplesPerPixel;//COUNT GROUP AVERAGE BY K
            int y = (int) (avg * middle);//Middle is h/2
            g2.drawLine(x, middle, x, middle - y);
        }

        // 時間軸刻度
        g2.setColor(Color.gray);
        for (int i = 0; i <= DISPLAY_SECONDS; i++) {
            int x = (int) (i * w / (double) DISPLAY_SECONDS);
            double timeMark = currentSec + i - DISPLAY_SECONDS / 2.0;
            if (timeMark >= 0 && timeMark <= durationSec) {
                g2.drawLine(x, h - 15, x, h);
                g2.drawString(timeFormat.format(timeMark) + "s", x + 2, h - 2);
            }
        }

        // 節奏點 overlay
        g2.setColor(Color.yellow);
        for (double beatTime : beatTimestamps) {
            double offset = beatTime - currentSec + DISPLAY_SECONDS / 2.0;
            int x = (int)(offset * w / DISPLAY_SECONDS);
            if (x >= 0 && x < w) {
                g2.drawLine(x, 0, x, h);
            }
        }

        // 播放進度條（紅色中線）
        g2.setColor(Color.red);
        int currentX = (int) (w / 2.0);
            g2.drawLine(currentX, 0, currentX, h);
    }

    public static void main(String[] args) throws Exception {
        String audioFile = "./sound/musicbox/music0.wav";
        JFrame frame = new JFrame("Waveform Visualizer with Time");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 300);

        WaveformVisualizer visualizer = new WaveformVisualizer(audioFile);
        frame.setContentPane(visualizer);
        frame.setVisible(true);
    }
}
