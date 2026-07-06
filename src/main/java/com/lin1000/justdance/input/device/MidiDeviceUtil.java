package com.lin1000.justdance.input.device;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

/**
 * Shared "can we read live note input from this MIDI device" check, used by both
 * {@code Project.initMidiDevice()} (the one-shot startup scan) and {@link MidiDeviceWatcher}
 * (the ongoing hot-plug poll). Both previously hardcoded a check for a device literally named
 * "USB-MIDI" whose {@code toString()} contained "MidiInDevice" — tuned to one specific piece
 * of hardware (a comment elsewhere referenced a "KAWAI CN201 Piano" MIDI interface), so any
 * other MIDI keyboard/controller was silently ignored.
 *
 * A device that can transmit MIDI messages to us ({@code getMaxTransmitters()} non-zero,
 * where -1 means "unlimited") is a general enough signal that it's an input-capable device,
 * without hardcoding a specific product's name — except every JVM also exposes the JDK's own
 * built-in {@link Sequencer} ("Real Time Sequencer") and {@link Synthesizer} ("Gervill")
 * devices via {@code MidiSystem.getMidiDeviceInfo()}, and the Sequencer in particular reports
 * unlimited transmitters (confirmed via a real headless run: a "Real Time Sequencer" with no
 * physical keyboard attached passed a transmitters-only check). Neither is a physical
 * controller a person plays, so both types are excluded explicitly.
 */
public final class MidiDeviceUtil {

    private MidiDeviceUtil() {}

    public static boolean isUsableInputDevice(MidiDevice device) {
        return device.getMaxTransmitters() != 0
                && !(device instanceof Sequencer)
                && !(device instanceof Synthesizer);
    }
}
