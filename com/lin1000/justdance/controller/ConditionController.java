package com.lin1000.justdance.controller;

import com.lin1000.justdance.gamepanel.Dance;

//=============================================================
//情況控制中心，即時反應出目前的任何condition的參數，以供其它
//component使用
//=============================================================
public class ConditionController extends Object {
    //紀錄目前belongs to which condition
    //----------------------------------
    //condition=0;PERFECT;
    //condition=1;GOOD;
    //condition=2;;
    //condition=3;MISS;
    //condition=4;GAME OVER;
    //condition=5;exit game;
    //condition=6;paly again;
    //----------------------------------
    //Binding Main Window Target
    Dance mainWindowTarget = null;

    public int condition;

    //附加功能--perfect--計數器
    public int perfectCounter;

    //附加功能--score--算分數
    public long score = 0;
    public double e = 1;

    //附加功能--life--算生命
    public int life = 100;

    //離開參數
    public boolean exit = false;
    public boolean continuetoplay = false;
    //gameover參數gameover畫面
    public boolean gameover = false;

    //CONSTRUCTOR
    public ConditionController(Dance mainWindowTarget) {
        //Reference to Main Window
        this.mainWindowTarget = mainWindowTarget;
        //初始化情況參數
        this.condition = 0;
        //perfectCounter用來計次
        perfectCounter = 0;
        //score用來算分數
        score = 0;
    }

    //設定情況參數


    public void setCondition(int c) {
        this.condition = c;

        //附加功能--perfect--計數器
        if (this.condition == 0 || this.condition == 1) this.perfectCounter++;
        else this.perfectCounter = 0;

        //計算分數法則，計算生命
        //MISS不加分 GOOD加80分 COMBO加120分
        //如果連續COMBO 1.8倍指數上升
        switch (c) {
            case 0: //perfect
                this.score += 200 ;
                if (this.perfectCounter >= 3) {
                    //mainWindowTarget.effectManager.addPerfectEffect(mainWindowTarget.g_off_x + mainWindowTarget.perfect_x, mainWindowTarget.g_off_y + mainWindowTarget.perfect_y);
                }
                break;
            case 1://GOOD
                this.score += 120 * (e * 1.8);
                this.life += 2;
                break;
            case 2://
                this.score += 80;
                break;
            case 3://MISS
                this.life -= 3;
                if(this.life<=0) life=0;
                if(this.life > 30) {
                    for (int i = 0; i < 10; i++) {
                        mainWindowTarget.effectManager.addLifeParticleEffect(mainWindowTarget.g_off_x + mainWindowTarget.life_x + (getLife() * 3), mainWindowTarget.g_off_y + mainWindowTarget.life_y);
                    }
                }else{
                    for (int i = 0; i < 20; i++) {
                        mainWindowTarget.effectManager.addLifeParticleExtremeEffect(mainWindowTarget.g_off_x + mainWindowTarget.life_x + (getLife() * 3), mainWindowTarget.g_off_y + mainWindowTarget.life_y);
                    }
                }
                break;
            case 4://GAMEOVER
                this.gameover = true;
                break;
            case 5://NOT PLAYING
                this.exit = true;
                this.continuetoplay = false;
                break;
            case 6://PLAY AGAIN
                this.exit = true;
                this.continuetoplay = true;
                break;
            case 7:
                break;
            default://初始指數
                this.e = 1;
        }
    }


    //傳回int代表目前情況，使程式依其情況做出反應
    public int getCondition() {
        return this.condition;
    }

    //附加功能--perfectCounter--傳回perfectCounter
    public String getperfectCounter() {
        //如果大於３才return
        if (this.perfectCounter >= 3) {
            return String.valueOf(this.perfectCounter);
        } else {
            return "  ";
        }
    }

    //附加功能--傳回score
    public long getScore() {
        return this.score;
    }

    //附加功能--傳回life
    public int getLife() {
        return this.life;
    }

    //遊戲進行參數gameover
    public boolean getGameOver() {
        return this.gameover;
    }

    //＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝傳給外面的變數
    public boolean getExit() {
        return this.exit;
    }

    public boolean getContinue() {
        return this.continuetoplay;
    }
    //＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝傳給外面的變數


}
