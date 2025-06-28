package com.lin1000.justdance.beats;

//真正的箭頭
public class Beat extends Object {
    public double time; // 節奏點發生的時間（秒）
    public int x;
    public int y;
    public boolean triggered = false;

    public Beat(double time, int x_position, int y_position) {
        this.time = time;
        x = x_position;
        y = y_position;
    }

}
