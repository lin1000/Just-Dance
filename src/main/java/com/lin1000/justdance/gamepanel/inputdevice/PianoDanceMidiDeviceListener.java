package com.lin1000.justdance.gamepanel.inputdevice;

import com.lin1000.justdance.gamepanel.PianoDance;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

/**
 * Real judgment for piano mode (Phase 6) — the counterpart to {@link DanceMidiDeviceListener},
 * which stays purely decorative for its existing job elsewhere. On a NOTE_ON, in addition to
 * updating the visual keyboard overlay, hit-tests the note against {@link PianoDance#producer}
 * and feeds the result into {@link PianoDance#conditionControl}.
 */
public class PianoDanceMidiDeviceListener implements Receiver {

    private final PianoDance mainWindowTarget;

    public PianoDanceMidiDeviceListener(PianoDance mainWindowTarget) {
        this.mainWindowTarget = mainWindowTarget;
    }

    @Override
    public void send(MidiMessage msg, long timeStamp) {
        if (!(msg instanceof ShortMessage sm)) return;

        int command = sm.getCommand();
        int ch = sm.getChannel();
        int note = sm.getData1();
        int velocity = sm.getData2();
        mainWindowTarget.pianoComponent.setInput(command, ch, note, velocity);

        if (command == ShortMessage.NOTE_ON && velocity > 0) {
            mainWindowTarget.pianoComponent.noteOn(note);
            if (!mainWindowTarget.conditionControl.getGameOver()
                    && !mainWindowTarget.conditionControl.getExit()) {
                int cond = mainWindowTarget.producer.tryHit(note, mainWindowTarget.currentNowSec());
                if (cond >= 0) {
                    mainWindowTarget.conditionControl.setCondition(cond);
                }
            }
        } else if (command == ShortMessage.NOTE_OFF ||
                (command == ShortMessage.NOTE_ON && velocity == 0)) {
            mainWindowTarget.pianoComponent.noteOff(note);
        }
    }

    @Override
    public void close() {}
}
