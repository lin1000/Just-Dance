package com.lin1000.justdance.beats;

import com.lin1000.justdance.audio.FFT;
import com.lin1000.justdance.song.Song;

import javax.sound.sampled.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class BeatMapGenerator {

    public enum Mode {
        ENERGY_PEAK, FFT_BASS, FFT_FLUX,;

        private static final Mode[] vals = values();
        public Mode next() {
            return vals[(this.ordinal() + 1) % vals.length];
        }
        public Mode prev() {
            return vals[(this.ordinal() -1) % vals.length==-1?vals.length-1:(this.ordinal() -1) % vals.length];
        }
    }

    public static Song analyze(File musicFile, Mode mode) throws Exception {

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

        byte[] audioBytes = stream.readAllBytes();
        frameSize = format.getFrameSize();
        frameRate = format.getFrameRate();
        int frameCount = audioBytes.length / (frameSize); //framesize = sampleSize * channel
        int sampleSize = format.getSampleSizeInBits() / 8;
        System.out.println("frameCount: " + frameCount);

        //load original wave form
        double[] samples = new double[frameCount];
        for (int i = 0; i < frameCount; i++) {
            double sum = 0;
            for (int ch = 0; ch < channels; ch++) {
                int base = (i * channels + ch) * sampleSize;
                int low = audioBytes[base] & 0xFF;
                int high = audioBytes[base + 1];
                int sample = bigEndian ? ((high << 8) | low) : ((low) | (high << 8));
                sum += sample / 32768.0;
            }
            samples[i] = sum / channels;
        }

        //load window/imag form for FFT.
        List<Beat> beats = new ArrayList<>();
        int windowSize = 2048;
        double[] window = new double[windowSize];
        double[] imag = new double[windowSize]; //imaginary number means

        for (int i = 0; i < frameCount - windowSize; i += windowSize / 2) {//FOR EACH WINDOW

            for (int j = 0; j < windowSize; j++) {//FOR EACH SAMPLE IN WINDOW
                int sampleIndex = (i + j) * frameSize;
                //System.out.println("sampleIndex="+sampleIndex);
                double sum = 0;
                for (int ch = 0; ch < channels; ch++) {//FOR EACH CHANNEL
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
                imag[j] = 0;
            }

            boolean isBeat = false;
            Mode signalMode = null;
            double signalStrength = 0;
            // analyze the energy pattern of each window (entire window,imag array is the result of FFT)
            if (mode == Mode.ENERGY_PEAK) { // 簡單能量檢測法：總能量超過門檻則產生節奏點
                double energy = 0;
                for (double v : window) energy += Math.abs(v);
                energy /= windowSize;
                isBeat = energy > 0.1;
                signalMode = Mode.ENERGY_PEAK;
                signalStrength = energy;
            } else if (mode == Mode.FFT_BASS) {
                FFT.fft(window, imag);
                double bassEnergy = 0;
                for (int b = 1; b <= 20; b++) {//FOR EACH BASS BAND
                    bassEnergy += Math.sqrt(window[b] * window[b] + imag[b] * imag[b]); //計算低頻的幅度(振幅)
                }
                isBeat = bassEnergy > 500;
                signalMode = Mode.FFT_BASS;
                signalStrength = bassEnergy;
            } else if (mode == Mode.FFT_FLUX) {
                FFT.fft(window, imag);
                double[] prevSpectrum = new double[windowSize / 2];
                double threshold = 0.5;
                double flux = 0;
                for (int b = 0; b < windowSize / 2; b++) {//FOR EACH BAD IN FIRST HALF WINDOW
                    double magnitude = Math.sqrt(window[b] * window[b] + imag[b] * imag[b]);
                    double diff = magnitude - prevSpectrum[b];
                    flux += (diff > 0) ? diff : 0;
                    prevSpectrum[b] = magnitude;

                    if (flux > threshold) {
                        isBeat = true;
                    }
                    signalMode = Mode.FFT_FLUX;
                    signalStrength = flux;
                }

            }

            if (isBeat) {
                double time = i / frameRate;
                Beat beat = new Beat(time, 200, 730);
                beat.setFrameIndex(i);
                beat.setSignalMode(signalMode);
                beat.setSingalStrength(signalStrength);
                beats.add(beat);
            }

        }

        int songBeats = beats.size();
        long songLengthInMillis = (long) (1000 * frameLength / frameRate);
        float songLengthInSeconds = (audioFileLength / (frameSize * frameRate));

        Song song = new Song();
        song.setName(musicFile.getName());
        song.setSongFilePath(musicFile.getAbsolutePath());
        song.setSongNumOfBeats(songBeats);
        song.setSongBPM(((int) (songBeats / songLengthInSeconds)));
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
        song.setAudioAnalysisMethod(mode.toString());
        song.setSongFeature("Analyzed by BeatMapGenerator");
        song.setBeats(beats);
        song.setSamples(samples);
        // Find the beat with the highest signal strength
        Optional<Beat> maxStrengthBeat = beats.stream().max(Comparator.comparing(Beat::getSingalStrength));
        System.out.println("beats.stream()=" + beats.stream().count());
        if (maxStrengthBeat.isPresent()) {
            Beat beat = maxStrengthBeat.get();
            int maxStrengthFrameIndex = beat.getFrameIndex();
            double maxStrengthSignalValue = beat.getSingalStrength();
            System.out.println("maxStrengthFrameIndex: " + maxStrengthFrameIndex);
            System.out.println("maxStrengthSignalValue: " + maxStrengthSignalValue);
            System.out.println("beat.getSignalMode(): " + beat.getSignalMode());
            song.setMaxSignalStrength(maxStrengthSignalValue);
        } else {
            System.out.println("maxStrengthBeat.isPresent() is empty.");
        }

        // Find the beat with the highest signal strength
        Optional<Beat> minStrengthBeat = beats.stream().min(Comparator.comparing(Beat::getSingalStrength));
        if (minStrengthBeat.isPresent()) {
            Beat beat = minStrengthBeat.get();
            int minStrengthFrameIndex = beat.getFrameIndex();
            double minStrengthSignalValue = beat.getSingalStrength();
            System.out.println("minStrengthFrameIndex: " + minStrengthFrameIndex);
            System.out.println("minStrengthSignalValue: " + minStrengthSignalValue);
            System.out.println("beat.getSignalMode(): " + beat.getSignalMode());
            song.setMinSignalStrength(minStrengthSignalValue);
        } else {
            System.out.println("minStrengthBeat.isPresent() is empty.");
        }
        return song;

    }

    public static List<Beat> generateBeats(File musicFile, Mode mode) throws Exception {

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
        int frameCount = audioBytes.length / (frameSize * channels);
        int sampleSize = format.getSampleSizeInBits()/8;
        System.out.println("frameCount: " + frameCount);
        List<Beat> beats = new ArrayList<>();
        int windowSize = 2048;

        double[] window = new double[windowSize];
        double[] imag = new double[windowSize]; //imaginary number means

        for (int i = 0; i < frameCount - windowSize; i += windowSize / 2) {
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
                imag[j] = 0;
            }

            boolean isBeat = false;

            // analyze the energy pattern of each window (entire window,imag array is the result of FFT)
            if (mode == Mode.ENERGY_PEAK) { // 簡單能量檢測法：總能量超過門檻則產生節奏點
                double energy = 0;
                for (double v : window) energy += Math.abs(v);
                energy /= windowSize;
                isBeat = energy > 0.1;
            }else if (mode == Mode.FFT_BASS) {
                FFT.fft(window, imag);
                double bassEnergy = 0;
                for (int b = 1; b <= 10; b++) {
                    bassEnergy += Math.sqrt(window[b] * window[b] + imag[b] * imag[b]); //計算低頻的幅度(振幅)
                }
                isBeat = bassEnergy > 5;
            }else if (mode == Mode.FFT_FLUX) {
                FFT.fft(window, imag);
                double[] prevSpectrum = new double[windowSize / 2];
                double threshold = 0.5;
                double flux = 0;
                for (int b = 0; b < windowSize / 2; b++) {
                    double magnitude = Math.sqrt(window[b] * window[b] + imag[b] * imag[b]);
                    double diff = magnitude - prevSpectrum[b];
                    flux += (diff > 0) ? diff : 0;
                    prevSpectrum[b] = magnitude;
                }

                if (flux > threshold) {
                    isBeat=true;
                }
            }

            if (isBeat) {
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
        song.setSongNumOfBeats(songBeats);
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
        song.setAudioAnalysisMethod(mode.toString());
        song.setSongFeature("Analyzed by BeatMapGenerator");
        //return song;

        return beats;

    }

}
