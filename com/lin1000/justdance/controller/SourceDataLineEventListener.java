package com.lin1000.justdance.controller;

import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

public class SourceDataLineEventListener implements LineListener {

    @Override
    public void update(LineEvent event) {
        LineEvent.Type type = event.getType();

        if (type == LineEvent.Type.START) {
            System.out.println("SourceDataLine started playback.");
        } else if (type == LineEvent.Type.STOP) {
            System.out.println("SourceDataLine stopped playback.");
            // You might want to close the line or perform other actions here
            // ((SourceDataLine) event.getLine()).close(); 
        } else if (type == LineEvent.Type.OPEN) {
            System.out.println("SourceDataLine opened.");
        } else if (type == LineEvent.Type.CLOSE) {
            System.out.println("SourceDataLine closed.");
        }
    }
}