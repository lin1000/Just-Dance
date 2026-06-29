package com.lin1000.justdance.gamepanel.action;

import com.lin1000.justdance.beats.Arrow;
import com.lin1000.justdance.gamepanel.Dance;
import com.lin1000.justdance.input.Input;

import java.util.ArrayList;
import java.util.List;

public class DanceAction {

    private static volatile DanceAction danceAction = null;
    private static Object danceActionSingletonLock = new Object();
    private static int judgeLine[] = {60, 80, 105}; // 判斷線位置，根據需要調整

    private DanceAction() {
    }

    public static DanceAction getInstance(){
        synchronized (danceActionSingletonLock) {
            if (danceAction == null) {
                danceAction = new DanceAction();
            }
            return danceAction;
        }
    }

    private void handleArrowHit(int vecIndex, Dance target) {
        List<Arrow> arrows = target.producer.vec[vecIndex];
        List<Arrow> toRemove = new ArrayList<>();
        for (Arrow myarrow : arrows) {
            if (myarrow.y >= judgeLine[0] && myarrow.y <= judgeLine[1]) {
                toRemove.add(myarrow);
                target.conditionControl.setCondition(0); // perfect
                target.effectManager.addSpecialEffect(
                    target.g_off_x + myarrow.x + 50, target.g_off_y + myarrow.y + 40);
                target.soundController.playEffectSound(0);
            } else if (myarrow.y > judgeLine[1] && myarrow.y <= judgeLine[2]) {
                toRemove.add(myarrow);
                target.conditionControl.setCondition(1); // good
                target.soundController.playEffectSound(1);
            }
        }
        arrows.removeAll(toRemove);
    }

    public void inputAction(Input input, Dance mainWindowTarget) {

        int pressedInt = input.isPressed() ? 1 : 0;
        if (!mainWindowTarget.conditionControl.getGameOver()) {
            switch(pressedInt){
                case 1: //pressed
                    switch (input.getInputType()){
                        case A: if(mainWindowTarget.dddCanvasComponent!=null)mainWindowTarget.dddCanvasComponent.keyPressed(input); break;
                        case S: if(mainWindowTarget.dddCanvasComponent!=null)mainWindowTarget.dddCanvasComponent.keyPressed(input); break;
                        case D: if(mainWindowTarget.dddCanvasComponent!=null)mainWindowTarget.dddCanvasComponent.keyPressed(input); break;
                        case W: if(mainWindowTarget.dddCanvasComponent!=null)mainWindowTarget.dddCanvasComponent.keyPressed(input); break;
                        case B:
                            break;
                        case X:
                            break;
                        case Y:
                            break;
                        case BACK:
                            break;
                        case START:
                            break;
                        case LEFT_SHOULDER:if(mainWindowTarget.dddCanvasComponent!=null)mainWindowTarget.dddCanvasComponent.keyPressed(input); break;
                        case RIGHT_SHOULDER:if(mainWindowTarget.dddCanvasComponent!=null)mainWindowTarget.dddCanvasComponent.keyPressed(input); break;
                        case UP:
                            mainWindowTarget.direct[2].set(true);
                            handleArrowHit(2, mainWindowTarget);
                            break;
                        case DOWN:
                            mainWindowTarget.direct[1].set(true);
                            handleArrowHit(1, mainWindowTarget);
                            break;
                        case LEFT:
                            mainWindowTarget.direct[0].set(true);
                            handleArrowHit(0, mainWindowTarget);
                            break;
                        case RIGHT:
                            mainWindowTarget.direct[3].set(true);
                            handleArrowHit(3, mainWindowTarget);
                            break;
                        case LEFT_THUMBSTICK_MOVE_LEFT:if(mainWindowTarget.dddCanvasComponent!=null)mainWindowTarget.dddCanvasComponent.keyPressed(input); break;
                        case LEFT_THUMBSTICK_MOVE_RIGHT:if(mainWindowTarget.dddCanvasComponent!=null)mainWindowTarget.dddCanvasComponent.keyPressed(input); break;
                        case GUIDE_BUTTON:
                            break;
                        case UNKNOWN:
                            break;
                    }

                    break;

                case 0: //released
                    switch (input.getInputType()){
                        case A : mainWindowTarget.dddCanvasComponent.keyReleased(input); break;
                        case S : mainWindowTarget.dddCanvasComponent.keyReleased(input); break;
                        case D : mainWindowTarget.dddCanvasComponent.keyReleased(input); break;
                        case W : mainWindowTarget.dddCanvasComponent.keyReleased(input); break;
                        case X:
                            break;
                        case Y:
                            break;
                        case BACK:
                            //mainThreadPause and wait until game thread notify
                            synchronized (mainWindowTarget.getProject().getMainThreadPauseLock()){
                                mainWindowTarget.producer.stop();
                                mainWindowTarget.conditionControl.setCondition(5);//5代表exit
                                mainWindowTarget.getProject().getMainThreadPauseLock().notifyAll();
                            }
                            break;
                        case START:
                            break;
                        case LEFT_SHOULDER:if(mainWindowTarget.dddCanvasComponent!=null)mainWindowTarget.dddCanvasComponent.keyReleased(input); break;
                        case RIGHT_SHOULDER:if(mainWindowTarget.dddCanvasComponent!=null)mainWindowTarget.dddCanvasComponent.keyReleased(input); break;
                        case UP:
                            mainWindowTarget.direct[2].set(false);
                            break;
                        case DOWN:
                            mainWindowTarget.direct[1].set(false);
                            break;
                        case LEFT:
                            mainWindowTarget.direct[0].set(false);
                            break;
                        case RIGHT:
                            mainWindowTarget.direct[3].set(false);
                            break;
                        case LEFT_THUMBSTICK_MOVE_LEFT: mainWindowTarget.dddCanvasComponent.keyReleased(input); break;
                        case LEFT_THUMBSTICK_MOVE_RIGHT:mainWindowTarget.dddCanvasComponent.keyReleased(input); break;
                        case GUIDE_BUTTON:
                            break;
                        case UNKNOWN:
                            break;
                    }

                    break;
            }
        } else { //mainWindowTarget.conditionControl.getGameOver()==true
            switch (pressedInt) {
                case 1: //pressed
                    switch (input.getInputType()) {
                        case LEFT_SHOULDER:
                            //mainWindowTarget.isListening=false;
                            break;
                        case RIGHT_SHOULDER:
                            break;
                        case A://play again (replay), not leave the game
                            synchronized (mainWindowTarget.getProject().getMainThreadPauseLock()){
                                mainWindowTarget.conditionControl.setCondition(6);//6代表replay
                                mainWindowTarget.getProject().getMainThreadPauseLock().notifyAll();
                            }
                            break;
                        case B:
                            break;
                        case X://exit , leave the game
                            //mainThreadPause and wait until game thread notify
                            synchronized (mainWindowTarget.getProject().getMainThreadPauseLock()){
                                mainWindowTarget.conditionControl.setCondition(5);//5代表exit
                                mainWindowTarget.getProject().getMainThreadPauseLock().notifyAll();
                            }
                            break;
                        case Y:
                            break;
                    }
            }
        }

    }


}
