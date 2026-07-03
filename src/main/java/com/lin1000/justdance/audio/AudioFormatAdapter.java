package com.lin1000.justdance.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

/**
 * Single chokepoint for opening a song's audio file, regardless of container/codec.
 *
 * Every other piece of this game (the byte-level PCM math in {@code BeatMapGenerator}, the
 * sample-clock loop in {@code SoundController}, {@code Clip.open(...)}) assumes it is handed
 * raw PCM_SIGNED samples. WAV already decodes to that directly; MP3/OGG decode to a compressed
 * {@link AudioFormat.Encoding} (MPEG1L3/VORBIS) that none of that downstream code understands.
 *
 * {@link #openPcm} is the adapter: it opens the file exactly as before, and if what comes back
 * isn't already PCM, converts it through {@code AudioSystem.getAudioInputStream(target, source)}
 * — the standard javax.sound.sampled two-step (container -> compressed stream -> PCM stream).
 * That second step only works because mp3spi/vorbisspi register javax.sound.sampled SPI
 * providers on the classpath (see pom.xml); this class doesn't reference those libraries by
 * name at all, so adding support for another format later is purely a classpath change.
 *
 * Callers never need to branch on file extension or codec — every call site in the codebase
 * that used to call {@code AudioSystem.getAudioInputStream(file)} directly now calls this
 * instead, and gets the same PCM shape whether the song is a .wav, .ogg, or .mp3.
 */
public final class AudioFormatAdapter {

    private AudioFormatAdapter() {}

    /** Bits per sample the rest of the pipeline is written against (see BeatMapGenerator/SoundController). */
    private static final int PCM_BITS = 16;

    public static AudioInputStream openPcm(File audioFile) throws UnsupportedAudioFileException, IOException {
        AudioInputStream source = AudioSystem.getAudioInputStream(audioFile);
        AudioFormat format = source.getFormat();
        if (format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) {
            return source; // WAV/AIFF/AU: already PCM, nothing to convert
        }

        // Compressed (MP3/OGG/etc): decode to 16-bit signed little-endian PCM at the source's
        // own sample rate/channel count, via the SPI FormatConversionProvider mp3spi/vorbisspi
        // register. If no provider on the classpath understands this encoding, AudioSystem
        // throws IllegalArgumentException — let it propagate, same as an unsupported file today.
        AudioFormat pcmFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                format.getSampleRate(),
                PCM_BITS,
                format.getChannels(),
                format.getChannels() * (PCM_BITS / 8),
                format.getSampleRate(),
                false); // little-endian, matching the byte order BeatMapGenerator/SoundController assume
        return AudioSystem.getAudioInputStream(pcmFormat, source);
    }
}
