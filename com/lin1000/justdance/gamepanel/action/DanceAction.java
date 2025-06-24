package com.lin1000.justdance.gamepanel.action;

import com.lin1000.justdance.beats.arrow;
import com.lin1000.justdance.gamepanel.Dance;
import com.lin1000.justdance.gamepanel.MainMenu;
import com.lin1000.justdance.gamepanel.input.Input;

public class DanceAction {

    private static DanceAction danceAction = null;
    private static Object danceActionSingletonLock = new Object();

    private DanceAction() {
    }

    public static DanceAction getInstance(){
        synchronized (danceActionSingletonLock) {
            if (danceAction == null) {
                danceAction = new DanceAction();
            }
        }
        return danceAction;
    }

    public void inputAction(Input input, Dance mainWindowTarget) {

        int pressedInt = input.isPressed() ? 1 : 0;
        switch(pressedInt){
            case 1: //pressed
                switch (input.getInputType()){
                    case A:
                        break;
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
                    case LEFT_SHOULDER:
                        break;
                    case RIGHT_SHOULDER:
                        break;
                    case UP:
                        break;
                    case DOWN:
                        break;
                    case LEFT:
                        mainWindowTarget.direct[0]=true;//讓gui反應出有按到
                        try
                        {

                            for(int element_index=0 ; element_index < 4 ; element_index++)
                            {
                                //mainWindowTarget.producer.vec[0].removeElementAt(element_index);
                                arrow myarrow =(arrow) mainWindowTarget.producer.vec[0].get(element_index);

                                //y=55~70是perfect
                                if(myarrow.y >= 50 && myarrow.y <= 70 )
                                {
                                    mainWindowTarget.producer.vec[0].removeElementAt(element_index);
                                    mainWindowTarget.conditionControl.setCondition(1);//１代表perfect
                                    //mainWindowTarget.soundController.play_conditionSound(mainWindowTarget.conditionControl.getCondition());
                                    mainWindowTarget.soundController.playEffectSound(0);
                                }
                                //y=71~90是good
                                if(myarrow.y > 70 && myarrow.y <= 90)
                                {
                                    mainWindowTarget.producer.vec[0].removeElementAt(element_index);
                                    mainWindowTarget.conditionControl.setCondition(2);//２代表good
                                    mainWindowTarget.soundController.playEffectSound(1);
                                }

                            }

                        }catch(java.lang.ArrayIndexOutOfBoundsException e){}

                        break;
                    case RIGHT:
                        break;
                    case LEFT_THUMBSTICK:
                        break;
                    case RIGHT_THUMBSTICK:
                        break;
                    case GUIDE_BUTTON:
                        break;
                    case UNKNOWN:
                        break;
                }

                break;

            case 0: //released
                switch (input.getInputType()){
                    case A:
                        break;
                    case B:
                        break;
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
                    case LEFT_SHOULDER:
                        break;
                    case RIGHT_SHOULDER:
                        break;
                    case UP:
                        break;
                    case DOWN:
                        break;
                    case LEFT:
                        mainWindowTarget.direct[0]=false;//讓gui反應出有按到
                        break;
                    case RIGHT:
                        break;
                    case LEFT_THUMBSTICK:
                        break;
                    case RIGHT_THUMBSTICK:
                        break;
                    case GUIDE_BUTTON:
                        break;
                    case UNKNOWN:
                        break;
                }

                break;
        }

    }


}
