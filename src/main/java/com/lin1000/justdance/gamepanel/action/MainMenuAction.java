package com.lin1000.justdance.gamepanel.action;

import com.lin1000.justdance.beats.Beat;
import com.lin1000.justdance.beats.BeatMapGenerator;
import com.lin1000.justdance.gamepanel.MainMenu;
import com.lin1000.justdance.input.Input;
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

    // BeatMapGenerator.analyze() decodes and FFT-scans the whole song (hundreds of ms to
    // low seconds); running it inline on every wheel tick used to freeze the caller (EDT for
    // keyboard nav, projectThread for gamepad nav) since both also drive rendering. This
    // background worker coalesces requests: each scroll tick just overwrites
    // `pendingRequest` rather than queuing, so a burst of fast-seek ticks never piles up
    // backlog, and a result computed for a song the user has since scrolled past is
    // discarded instead of published (latest request always wins).
    private record AnalysisRequest(File musicFile, BeatMapGenerator.Mode mode, MainMenu target) {}

    private static final Object analysisLock = new Object();
    private static AnalysisRequest pendingRequest;
    private static Thread analysisWorkerThread;

    private static void requestSongAnalysis(File musicFile, BeatMapGenerator.Mode mode, MainMenu target) {
        synchronized (analysisLock) {
            pendingRequest = new AnalysisRequest(musicFile, mode, target);
            if (analysisWorkerThread == null) {
                analysisWorkerThread = new Thread(MainMenuAction::runAnalysisWorker, "song-analysis-worker");
                analysisWorkerThread.setDaemon(true);
                analysisWorkerThread.start();
            }
            analysisLock.notifyAll();
        }
    }

    private static void runAnalysisWorker() {
        while (true) {
            AnalysisRequest request;
            synchronized (analysisLock) {
                while (pendingRequest == null) {
                    try {
                        analysisLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                request = pendingRequest;
                pendingRequest = null;
            }

            Song song = null;
            try {
                song = BeatMapGenerator.analyze(request.musicFile(), request.mode());
            } catch (Exception e) {
                System.err.println("BeatMapGenerator has problem: " + e);
                e.printStackTrace();
            }

            synchronized (analysisLock) {
                if (pendingRequest == null) {
                    // Nothing newer arrived while we were analyzing: still the latest selection.
                    request.target().setWhichSong(song);
                }
                // else: the user has already scrolled past this song; drop the stale result
                // and loop back around to pick up pendingRequest immediately.
            }
        }
    }

    // Fast-seek acceleration: holding UP/DOWN delivers OS key-repeat events in a rapid
    // stream; a streak of them grows the per-press step (1 → 2 → 3 slots) so long
    // libraries can be crossed quickly. Any pause resets to single-step precision.
    private long navLastMs = 0;
    private int navStreak = 0;

    private int navStep() {
        long now = System.currentTimeMillis();
        navStreak = (now - navLastMs < 230) ? navStreak + 1 : 1;
        navLastMs = now;
        if (navStreak >= 10) return 3;
        if (navStreak >= 5)  return 2;
        return 1;
    }

    public void inputAction(Input input, MainMenu mainWindowTarget) {
        switch (mainWindowTarget.getcontrolFlow()) {
            case 1: //1 means in game landing screen
                switch(input.getInputType()){
                    case DOWN->{}
                    case UP->{}
                    case A->{}
                    case S ->{}
                    case D ->{}
                    case W ->{}
                    case B ->{}
                    case X ->{}
                    case Y ->{}
                    case START-> mainWindowTarget.controlFlow = 2;//2 means chose music;
                    case BACK->mainWindowTarget.controlFlow = 4;//4 means exit
                    case LEFT_SHOULDER->{}
                    case RIGHT_SHOULDER->{}
                    case LEFT ->{}
                    case RIGHT ->{}
                    case LEFT_THUMBSTICK_MOVE_LEFT ->{}
                    case LEFT_THUMBSTICK_MOVE_RIGHT ->{}
                    case LEFT_THUMBSTICK_MOVE_FORWARD ->{}
                    case LEFT_THUMBSTICK_MOVE_BACKWARD ->{}
                    case GUIDE_BUTTON ->{}
                    case UNKNOWN ->{}
                }
                break;
            case 2://2 means in chose music (main menu screen)
                if (input.getInputType() == Input.InputType.DOWN && input.isPressed()) {
                    int n = com.lin1000.justdance.song.SongLibrary.size();
                    mainWindowTarget.musicOptionIndex = (mainWindowTarget.musicOptionIndex + navStep()) % n;
                    switchSong(mainWindowTarget);
                } else if (input.getInputType() == Input.InputType.UP && input.isPressed()) {
                    int n = com.lin1000.justdance.song.SongLibrary.size();
                    mainWindowTarget.musicOptionIndex = (mainWindowTarget.musicOptionIndex - navStep() % n + n) % n;
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
                            //beats already auto generated in switchSong when choosing music
                            //List<Beat> songBeats = generateBeats(mainWindowTarget);
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
                } else if (input.getInputType() == Input.InputType.LEFT && input.isPressed()) {
                    //cycle difficulty level down (easier / slower scroll)
                    mainWindowTarget.speedModifier.easier();
                } else if (input.getInputType() == Input.InputType.RIGHT && input.isPressed()) {
                    //cycle difficulty level up (harder / faster scroll)
                    mainWindowTarget.speedModifier.harder();
                } else if (input.getInputType() == Input.InputType.X && input.isPressed()) {
                    //toggle game mode (arrow <-> piano)
                    mainWindowTarget.gameMode.toggle();
                } else if (input.getInputType() == Input.InputType.Y && input.isPressed()) {
                    //enter the standalone water-dance showcase (attract-mode, no scoring)
                    mainWindowTarget.controlFlow = 5;
                } else if (input.getInputType() == Input.InputType.B && input.isPressed()) {
                    //enter the second, independent particle-based water-dance showcase
                    mainWindowTarget.controlFlow = 6;
                } else if (input.getInputType() == Input.InputType.S && input.isPressed()) {
                    //enter the camera motion-tracking showcase (real frame-diff, sim fallback)
                    mainWindowTarget.controlFlow = 7;
                } else if (input.getInputType() == Input.InputType.W && input.isPressed()) {
                    //enter the camera fruit-slicing game (hand-swipe tracking, sim fallback)
                    mainWindowTarget.controlFlow = 8;
                }
                break;

            case 5: //5 means in the water-dance showcase (attract-mode)
                if (input.getInputType() == Input.InputType.BACK && input.isPressed()) {
                    mainWindowTarget.controlFlow = 2; //back to song-select, not a full exit
                }
                break;

            case 6: //6 means in the water-dance particle showcase (attract-mode)
                if (input.getInputType() == Input.InputType.BACK && input.isPressed()) {
                    mainWindowTarget.controlFlow = 2; //back to song-select, not a full exit
                }
                break;

            case 7: //7 means in the motion-dance camera showcase (attract-mode)
                if (input.getInputType() == Input.InputType.BACK && input.isPressed()) {
                    mainWindowTarget.controlFlow = 2; //back to song-select, not a full exit
                }
                break;

            case 8: //8 means in the motion-slice camera game (attract-mode)
                if (input.getInputType() == Input.InputType.BACK && input.isPressed()) {
                    mainWindowTarget.controlFlow = 2; //back to song-select, not a full exit
                }
                break;

        }
    }

    /**
     * Audio BPM Analysis by FFT_BASS or ENERGY_PEAK, run asynchronously on the background
     * analysis worker; the result lands in mainWindowTarget.whichSong once ready (see
     * requestSongAnalysis/runAnalysisWorker above).
     */
    private void analyzeSongAsync(MainMenu mainWindowTarget){
        File musicFile = mainWindowTarget.soundController.getMusicbox()[mainWindowTarget.musicOptionIndex];
        BeatMapGenerator.Mode mode = mainWindowTarget.soundController.getCurrentAudioAnalysisMode();
        requestSongAnalysis(musicFile, mode, mainWindowTarget);
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
        analyzeSongAsync(mainWindowTarget);
        mainWindowTarget.menuscreen(mainWindowTarget.musicOptionIndex);
    }


}
