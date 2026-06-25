package com.lin1000.justdance.gamepanel.inputdevice;

import com.lin1000.justdance.gamepanel.MainMenu;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

public class MainMenuMidiDeviceListener implements Receiver {

    //Binding Main Window Target
    MainMenu mainWindowTarget = null;

    public MainMenuMidiDeviceListener(MainMenu mainWindowTarget) {
        this.mainWindowTarget = mainWindowTarget;
    }

    @Override
    public void send(MidiMessage msg, long timeStamp) {
        if (msg instanceof ShortMessage sm) {
            if(mainWindowTarget!=null) mainWindowTarget.midiControllerComponent.setRactScale();
        }
    }

    @Override
    public void close() {}
}
