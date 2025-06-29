package com.lin1000.justdance.gamepanel.action;

import com.lin1000.justdance.beats.Beat;
import com.lin1000.justdance.beats.BeatMapGenerator;
import com.lin1000.justdance.controller.SoundController;
import com.lin1000.justdance.gamepanel.MainMenu;
import com.lin1000.justdance.gamepanel.input.Input;
import com.lin1000.justdance.song.Song;

import java.io.File;
import java.util.List;

public class MainMenuAction {

    private static MainMenuAction mainMenuAction = null;
    private static Object mainMenuActionSingletonLock = new Object();

    private MainMenuAction() {
    }

    public static MainMenuAction getInstance(){
        synchronized (mainMenuActionSingletonLock) {
            if (mainMenuAction == null) {
                mainMenuAction = new MainMenuAction();
            }
        }
        return mainMenuAction;
    }

    public void inputAction(Input input, MainMenu mainWindowTarget) {
        switch (mainWindowTarget.getcontrolFlow()) {
            case 1: //1 means in game landing screen
                if (input.getInputType() == Input.InputType.DOWN && input.isPressed()) {
                    //do nothing
                } else if (input.getInputType() == Input.InputType.UP && input.isPressed()) {
                    //do nothing
                } else if (input.getInputType() == Input.InputType.A && input.isPressed()) {
                    //do nothing
                } else if (input.getInputType() == Input.InputType.START && input.isPressed()) {
                    mainWindowTarget.controlFlow = 2;//2 means chose music
                } else if (input.getInputType() == Input.InputType.BACK && input.isPressed()) {
                    mainWindowTarget.controlFlow = 4;//4 means exit
                } else if (input.getInputType() == Input.InputType.LEFT_SHOULDER && input.isPressed()) {
                    //do nothing
                } else if (input.getInputType() == Input.InputType.RIGHT_SHOULDER && input.isPressed()) {
                    //do nothing
                }

                break;
            case 2://2 means in chose music (main menu screen)
                if (input.getInputType() == Input.InputType.DOWN && input.isPressed()) {
                    mainWindowTarget.musicOptionIndex++;
                    if (mainWindowTarget.musicOptionIndex == 4) mainWindowTarget.musicOptionIndex = 0;
                    switchSong(mainWindowTarget);
                } else if (input.getInputType() == Input.InputType.UP && input.isPressed()) {
                    mainWindowTarget.musicOptionIndex--;
                    if (mainWindowTarget.musicOptionIndex == -1) mainWindowTarget.musicOptionIndex = 3;
                    switchSong(mainWindowTarget);
                } else if (input.getInputType() == Input.InputType.A && input.isPressed()) {
                    switch (mainWindowTarget.controlFlow) {
                        case 1:
                            mainWindowTarget.controlFlow=2;
                            //mainWindowTarget.soundController.playBackgroundSound(mainWindowTarget.musicOptionIndex);
                            break;
                        case 2:
                            mainWindowTarget.soundController.playMainMenuSound(1);
                            mainWindowTarget.controlFlow=3;
                            List<Beat> songBeats = generateBeats(mainWindowTarget);
                            //mainWindowTarget.menuscreen();
                            break;
                        case 3:
                            break;
                        default:
                    }
                } else if (input.getInputType() == Input.InputType.BACK && input.isPressed()) {
                    mainWindowTarget.controlFlow = 4;//4 means exit
                } else if (input.getInputType() == Input.InputType.GUIDE_BUTTON && input.isPressed()) {
                    switchSong(mainWindowTarget);
                } else if (input.getInputType() == Input.InputType.LEFT_SHOULDER && input.isPressed()) {
                    //switch audio analysis mode
                    mainWindowTarget.soundController.switchAudioAnalysisMode(0); //previous algorithm
                    switchSong(mainWindowTarget);
                } else if (input.getInputType() == Input.InputType.RIGHT_SHOULDER && input.isPressed()) {
                    //switch audio analysis mode
                    mainWindowTarget.soundController.switchAudioAnalysisMode(1);//next algorithm
                    switchSong(mainWindowTarget);
                }
                break;

        }
    }

    /**
     * Audio BPM Analysis by FFT_BASS or ENERGY_PEAK and return song
     */
    private Song analyzeSong(MainMenu mainWindowTarget){
        Song song = null;
        File musicFile = mainWindowTarget.soundController.getMusicbox()[mainWindowTarget.musicOptionIndex];
        BeatMapGenerator.Mode mode = mainWindowTarget.soundController.getCurrentAudioAnalysisMode();
        try {
            song  = BeatMapGenerator.analyze(musicFile, mode);
        } catch (Exception e) {
            System.err.println("BeatMapGenerator has problem");
        }
        return song;
    }

    /**
     * Audio BPM Analysis by FFT_BASS or ENERGY_PEAK and return list of beats
     */
    private List<Beat> generateBeats(MainMenu mainWindowTarget) {
        List<Beat> songBeatsList = null;
        File musicFile = mainWindowTarget.soundController.getMusicbox()[mainWindowTarget.musicOptionIndex];
        BeatMapGenerator.Mode mode = mainWindowTarget.soundController.getCurrentAudioAnalysisMode();
        try {
            songBeatsList = BeatMapGenerator.generateBeats(musicFile, BeatMapGenerator.Mode.FFT_BASS);
            //mainWindowTarget.setWhichSong(song);
        } catch (Exception e) {
            System.err.println("BeatMapGenerator has problem");
        }
        return songBeatsList;
    }

    private void switchSong(MainMenu mainWindowTarget) {
        mainWindowTarget.soundController.playBackgroundSound(mainWindowTarget.musicOptionIndex, true);
        Song song = analyzeSong(mainWindowTarget);
        mainWindowTarget.setWhichSong(song);
        mainWindowTarget.menuscreen(mainWindowTarget.musicOptionIndex);
    }


}
