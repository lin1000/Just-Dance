package com.lin1000.justdance.song;

import javax.sound.sampled.AudioFormat;

public class Song {

    public String name;
    public String songFilePath;
    int songBeats;
    int songBPM;
    String songFeature;
    long songLengthInMillis;
    float songLengthInSeconds;
    long audioFileLength ;
    float frameRate ;
    int frameSize ;
    long frameLength ;
    AudioFormat.Encoding soundEcoding;
    float sampleRate;
    int sampleSizeInBits ;
    int channels;
    public String audioAnalysisMethod;


    public Song(){}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSongFilePath() {
        return songFilePath;
    }

    public void setSongFilePath(String songFilePath) {
        this.songFilePath = songFilePath;
    }

    public int getSongBeats() {
        return songBeats;
    }

    public void setSongBeats(int songBeats) {
        this.songBeats = songBeats;
    }

    public int getSongBPM() {
        return songBPM;
    }

    public void setSongBPM(int songBPM) {
        this.songBPM = songBPM;
    }

    public String getSongFeature() {
        return songFeature;
    }

    public void setSongFeature(String songFeature) {
        this.songFeature = songFeature;
    }

    public long getSongLengthInMillis() {
        return songLengthInMillis;
    }

    public void setSongLengthInMillis(long songLengthInMillis) {
        this.songLengthInMillis = songLengthInMillis;
    }

    public float getSongLengthInSeconds() {
        return songLengthInSeconds;
    }

    public void setSongLengthInSeconds(float songLengthInSeconds) {
        this.songLengthInSeconds = songLengthInSeconds;
    }

    public long getAudioFileLength() {
        return audioFileLength;
    }

    public void setAudioFileLength(long audioFileLength) {
        this.audioFileLength = audioFileLength;
    }

    public float getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(float frameRate) {
        this.frameRate = frameRate;
    }

    public int getFrameSize() {
        return frameSize;
    }

    public void setFrameSize(int frameSize) {
        this.frameSize = frameSize;
    }

    public long getFrameLength() {
        return frameLength;
    }

    public void setFrameLength(long frameLength) {
        this.frameLength = frameLength;
    }

    public AudioFormat.Encoding getSoundEcoding() {
        return soundEcoding;
    }

    public void setSoundEcoding(AudioFormat.Encoding soundEcoding) {
        this.soundEcoding = soundEcoding;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getSampleSizeInBits() {
        return sampleSizeInBits;
    }

    public void setSampleSizeInBits(int sampleSizeInBits) {
        this.sampleSizeInBits = sampleSizeInBits;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public int[] getSongLengthInMinutesAndSeconds(){
        int minutes = (int)songLengthInSeconds / 60;
        int seconds = (int)songLengthInSeconds % 60;
        return new int[]{minutes, seconds};
    }

    public String getAudioAnalysisMethod() {
        return audioAnalysisMethod;
    }

    public void setAudioAnalysisMethod(String audioAnalysisMethod) {
        this.audioAnalysisMethod = audioAnalysisMethod;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Song Name: ").append(name).append("\n");
        sb.append("Song File Path: ").append(songFilePath).append("\n");
        sb.append("Song Beats: ").append(songBeats).append("\n");
        sb.append("Song BPM: ").append(songBPM).append("\n");
        sb.append("Song Feature: ").append(songFeature).append("\n");
        sb.append("Song Length (ms): ").append(songLengthInMillis).append("\n");
        sb.append("Song Length (s): ").append(songLengthInSeconds).append("\n");
        sb.append("Song Length (mm:ss)").append(getSongLengthInMinutesAndSeconds()[0]+":"+getSongLengthInMinutesAndSeconds()[1]).append("\n");
        sb.append("Audio File Length (bytes): ").append(audioFileLength).append("\n");
        sb.append("Frame Rate: ").append(frameRate).append("\n");
        sb.append("Frame Size: ").append(frameSize).append("\n");
        sb.append("Frame Length: ").append(frameLength).append("\n");
        sb.append("Sound Encoding: ").append(soundEcoding).append("\n");
        sb.append("Sample Rate: ").append(sampleRate).append("\n");
        sb.append("Sample Size in Bits: ").append(sampleSizeInBits).append("\n");
        sb.append("Channels: ").append(channels).append("\n");
        sb.append("Audio Analysis Method: ").append(audioAnalysisMethod).append("\n");

        return sb.toString();
    }

}
