package com.lin1000.justdance.gamepanel.inputdevice;

import com.lin1000.justdance.gamepanel.Dance;

import javax.sound.midi.Receiver;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

public class DanceMidiDeviceListener implements Receiver {

    //Binding Main Window Target
    Dance mainWindowTarget = null;

    public DanceMidiDeviceListener(Dance mainWindowTarget) {
        this.mainWindowTarget = mainWindowTarget;
    }

    @Override
    public void send(MidiMessage msg, long timeStamp) {
        if (msg instanceof ShortMessage sm) {
            int command = sm.getCommand();
            int ch  = sm.getChannel();
            int note = sm.getData1();
            int velocity = sm.getData2();
            mainWindowTarget.pianoComponent.setInput(command, ch,note,velocity);
            if (command == ShortMessage.NOTE_ON && velocity > 0) {
                System.out.println("Note ON: " + note + " with velocity: " + velocity);
                mainWindowTarget.pianoComponent.noteOn(note);
            } else if (command == ShortMessage.NOTE_OFF ||
                    (command == ShortMessage.NOTE_ON && velocity == 0)) {
                System.out.println("Note OFF: " + note + " with velocity: " + velocity);
                mainWindowTarget.pianoComponent.noteOff(note);
            } else if (command == ShortMessage.CONTROL_CHANGE) {
                System.out.printf("ch=%d note=%d velocity=%d",ch, note, velocity);
                if (note == 64) { // 64=Damper(延音踏板)
                    boolean pedalDown = note >= 64;
                    System.out.printf("Sustain %s%n", pedalDown ? "DOWN" : "UP");
                }
            } else if (command == ShortMessage.PITCH_BEND){
                int bend = ((sm.getData2() << 7) | sm.getData1()) - 8192;
                System.out.printf("PitchBend %d%n", bend);
            } else {
                System.out.printf("ch=%d note=%d velocity=%d",ch, note, velocity);
            }

        }
    }

    @Override
    public void close() {}
}
