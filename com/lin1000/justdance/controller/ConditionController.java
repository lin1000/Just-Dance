package com.lin1000.justdance.controller;

//=============================================================
//情況控制中心，即時反應出目前的任何condition的參數，以供其它
//component使用
//=============================================================
public class ConditionController extends Object {
    //紀錄目前belongs to which condition
    //----------------------------------
    //condition=1;COMBO;
    //condition=2;GOOD!;
    //condition=3;MISS;
    //condition=4;GAME OVER;
    //condition=5;exit game;
    //condition=6;paly again;
    //----------------------------------
    public int condition;

    //附加功能--perfect--計數器
    public int perfectCounter;

    //附加功能--score--算分數
    public long score = 0;
    public double e;

    //附加功能--life--算生命
    public int life = 50;

    //離開參數
    public boolean exit = false;
    public boolean continuetoplay = false;
    //gameover參數gameover畫面
    public boolean gameover = false;

    //紀錄display的位置
    public int x_position;
    public int y_position;

    //CONSTRUCTOR
    public ConditionController(int x, int y) {
        //初始化情況參數
        this.condition = 0;

        //assign顯示位置
        this.x_position = x;
        this.y_position = y;

        //perfectCounter用來計次
        perfectCounter = 0;
        //score用來算分數
        score = 0;
    }

    //設定情況參數


    public void setCondition(int c) {
        this.condition = c;

        //附加功能--perfect--計數器
        if (this.condition == 1) this.perfectCounter++;
        else this.perfectCounter = 0;

        //計算分數法則，計算生命
        //MISS不加分 GOOD加80分 COMBO加120分
        //如果連續COMBO 1.8倍指數上升
        switch (c) {
            case 1://combo
                this.score += 120 * (e * 1.8);
                this.life += 2;
                break;
            case 2:
                this.score += 80;
                break;
            case 3:
                this.life -= 3;
                break;
            case 4:
                this.gameover = true;
                break;
            case 5:
                this.exit = true;
                this.continuetoplay = false;
                break;
            case 6:
                this.exit = true;
                this.continuetoplay = true;
                break;
            default://初始指數
                this.e = 1;
        }

    }


    //傳回int代表目前情況，使程式依其情況做出反應
    public int getCondition() {
        return this.condition;
    }

    public int getX() {
        return this.x_position;
    }

    public int getY() {
        return this.y_position;
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
