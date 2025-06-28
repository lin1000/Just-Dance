package com.lin1000.justdance.beats;

import com.lin1000.justdance.audio.FFT;
import com.lin1000.justdance.song.Song;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BeatMapGenerator {

    public static Song analyze(File musicFile) throws Exception {

        AudioInputStream stream = AudioSystem.getAudioInputStream(musicFile);
        AudioFormat format = stream.getFormat();

        long audioFileLength = musicFile.length();
        float frameRate = format.getFrameRate();
        int frameSize = format.getFrameSize();
        long frameLength = stream.getFrameLength();
        AudioFormat.Encoding soundEcoding = format.getEncoding();
        float sampleRate = format.getSampleRate();
        int sampleSizeInBits = format.getSampleSizeInBits();
        int channels = format.getChannels();
        boolean bigEndian = format.isBigEndian();

        System.out.println("=========================================");
        System.out.println("musicbox[music]: " + musicFile.getName());
        System.out.println("frameLength: " + frameLength);
        System.out.println("format.getFrameRate(): " + frameRate);
        System.out.println("format.getFrameSize(): " + frameSize);
        System.out.println("format.getEncoding(): " + soundEcoding.toString());
        System.out.println("format.getSampleSizeInBits(): " + sampleSizeInBits);
        System.out.println("format.getChannels(): " + channels);
        System.out.println("format.isBigEndian(): " + bigEndian);


//        if (format.getChannels() != 1 || format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
//            throw new UnsupportedAudioFileException("只支援單聲道PCM格式音檔");
//        }

        byte[] audioBytes = stream.readAllBytes();
        frameSize = format.getFrameSize();
        frameRate = format.getFrameRate();
        int frameCount = audioBytes.length / frameSize;
        int sampleSize = format.getSampleSizeInBits()/8;
        System.out.println("frameCount: " + frameCount);
        List<Beat> beats = new ArrayList<>();
        int windowSize = 2048;

        double[] window = new double[windowSize];

        for (int i = 0; i < frameCount - windowSize; i += windowSize / 2) {
            // count the energy power of each window
            for (int j = 0; j < windowSize; j++) {
                int sampleIndex = (i + j) * frameSize;
                //System.out.println("sampleIndex="+sampleIndex);
                double sum = 0;
                for (int ch = 0; ch < channels; ch++) {
                    int base = sampleIndex + ch * sampleSize;
                    int sample = 0;
                    if (sampleSize == 2) {
                        int low = audioBytes[base] & 0xFF;
                        int high = audioBytes[base + 1];
                        sample = bigEndian ? ((high << 8) | low) : ((low) | (high << 8));
                    } else if (sampleSize == 1) {
                        sample = audioBytes[base];
                    }
                    sum += sample / 32768.0; // normalize
                }
                window[j] = sum / channels;
            }

            // 簡單能量檢測法：總能量超過門檻則產生節奏點
            double energy = 0;
            for (double v : window) {
                energy += Math.abs(v);
            }
            energy /= windowSize;

            // add beats at specific time point whenever energy is over the threshold
            if (energy > 0.02) {
                double time = i / frameRate;
                beats.add(new Beat(time, 200, 730));
            }
        }

        int songBeats = beats.size();
        long songLengthInMillis = (long) (1000 * frameLength / frameRate);
        float songLengthInSeconds = (audioFileLength / (frameSize * frameRate));

        Song song = new Song();
        song.setName(musicFile.getName());
        song.setSongFilePath(musicFile.getAbsolutePath());
        song.setSongBeats(songBeats);
        song.setSongBPM(((int)(songBeats/songLengthInSeconds)));
        song.setSongLengthInSeconds(songLengthInSeconds);
        song.setSongLengthInMillis(songLengthInMillis);
        song.setFrameLength(frameLength);
        song.setFrameRate(frameRate);
        song.setFrameSize(frameSize);
        song.setSampleRate(sampleRate);
        song.setSampleSizeInBits(sampleSizeInBits);
        song.setChannels(channels);
        song.setSoundEcoding(soundEcoding);
        song.setAudioFileLength(audioFileLength);
        song.setSongFeature("Analyzed by BeatMapGenerator");
        return song;
    }

    public static List<Beat> generate(File musicFile) throws Exception {

        AudioInputStream stream = AudioSystem.getAudioInputStream(musicFile);
        AudioFormat format = stream.getFormat();

        long audioFileLength = musicFile.length();
        float frameRate = format.getFrameRate();
        int frameSize = format.getFrameSize();
        long frameLength = stream.getFrameLength();
        AudioFormat.Encoding soundEcoding = format.getEncoding();
        float sampleRate = format.getSampleRate();
        int sampleSizeInBits = format.getSampleSizeInBits();
        int channels = format.getChannels();
        boolean bigEndian = format.isBigEndian();


        System.out.println("musicbox[music]: " + musicFile.getName());
        System.out.println("frameLength: " + frameLength);
        System.out.println("format.getFrameRate(): " + frameRate);
        System.out.println("format.getFrameSize(): " + frameSize);
        System.out.println("format.getEncoding(): " + soundEcoding.toString());
        System.out.println("format.getSampleSizeInBits(): " + sampleSizeInBits);
        System.out.println("format.getChannels(): " + channels);
        System.out.println("format.isBigEndian(): " + bigEndian);


//        if (format.getChannels() != 1 || format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
//            throw new UnsupportedAudioFileException("只支援單聲道PCM格式音檔");
//        }

        byte[] audioBytes = stream.readAllBytes();
        frameSize = format.getFrameSize();
        frameRate = format.getFrameRate();
        int frameCount = audioBytes.length / frameSize;
        int sampleSize = format.getSampleSizeInBits()/8;
        System.out.println("frameCount: " + frameCount);
        List<Beat> beats = new ArrayList<>();
        int windowSize = 2048;

        double[] window = new double[windowSize];

        for (int i = 0; i < frameCount - windowSize; i += windowSize / 2) {
            // count the energy power of each window
            for (int j = 0; j < windowSize; j++) {
                int sampleIndex = (i + j) * frameSize;
                //System.out.println("sampleIndex="+sampleIndex);
                double sum = 0;
                for (int ch = 0; ch < channels; ch++) {
                    int base = sampleIndex + ch * sampleSize;
                    int sample = 0;
                    if (sampleSize == 2) {
                        int low = audioBytes[base] & 0xFF;
                        int high = audioBytes[base + 1];
                        sample = bigEndian ? ((high << 8) | low) : ((low) | (high << 8));
                    } else if (sampleSize == 1) {
                        sample = audioBytes[base];
                    }
                    sum += sample / 32768.0; // normalize
                }
                window[j] = sum / channels;
            }

            // 簡單能量檢測法：總能量超過門檻則產生節奏點
            double energy = 0;
            for (double v : window) {
                energy += Math.abs(v);
            }
            energy /= windowSize;

            // add beats at specific time point whenever energy is over the threshold
            if (energy > 0.1) {
                double time = i / frameRate;
                System.out.println("time (seconds):" + time);
                beats.add(new Beat(time, 200, 730));
            }
        }

        int songBeats = beats.size();
        long songLengthInMillis = (long) (1000 * frameLength / frameRate);
        float songLengthInSeconds = (audioFileLength / (frameSize * frameRate));

        Song song = new Song();
        song.setName(musicFile.getName());
        song.setSongFilePath(musicFile.getAbsolutePath());
        song.setSongBeats(songBeats);
        song.setSongBPM(((int)(songBeats/songLengthInSeconds)));
        song.setSongLengthInSeconds(songLengthInSeconds);
        song.setSongLengthInMillis(songLengthInMillis);
        song.setFrameLength(frameLength);
        song.setFrameRate(frameRate);
        song.setFrameSize(frameSize);
        song.setSampleRate(sampleRate);
        song.setSampleSizeInBits(sampleSizeInBits);
        song.setChannels(channels);
        song.setSoundEcoding(soundEcoding);
        song.setAudioFileLength(audioFileLength);
        song.setSongFeature("Generated by BeatMapGenerator");

        System.out.println(song.toString());

//        double[] windowReal = new double[windowSize];
//        double[] windowImag = new double[windowSize];
//
//        for (int i = 0; i < sampleCount - windowSize; i += windowSize / 2) {
//            for (int j = 0; j < windowSize; j++) {
//                int low = audioBytes[(i + j) * 2] & 0xff;
//                int high = audioBytes[(i + j) * 2 + 1];
//                int sample = (high << 8) | low;
//                windowReal[j] = sample / 32768.0;
//                windowImag[j] = 0;
//            }
//
//            FFT.fft(windowReal, windowImag);
//
//            double bassEnergy = 0;
//            int bassStart = 1;
//            int bassEnd = 10;
//            for (int k = bassStart; k <= bassEnd; k++) {
//                bassEnergy += Math.sqrt(windowReal[k] * windowReal[k] + windowImag[k] * windowImag[k]);
//            }
//
//            if (bassEnergy > 5) {
//                double time = i / frameRate;
//                //beats.add(new Beat(time, 200)); // x=200固定座標
//            }
//        }

        return beats;
    }
}
