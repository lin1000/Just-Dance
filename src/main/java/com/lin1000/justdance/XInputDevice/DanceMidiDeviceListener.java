package com.lin1000.justdance.XInputDevice;

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
            int note = sm.getData1();
            int velocity = sm.getData2();

            if (command == ShortMessage.NOTE_ON && velocity > 0) {
                System.out.println("Note ON: " + note + " with velocity: " + velocity);
            } else if (command == ShortMessage.NOTE_OFF ||
                    (command == ShortMessage.NOTE_ON && velocity == 0)) {
                System.out.println("Note OFF: " + note + " with velocity: " + velocity);
            }
        }
    }

    @Override
    public void close() {}
}
