package com.lin1000.justdance.beats;

//真正的箭頭
public class Arrow extends Object {
    public int x;
    public int y;
    public boolean triggered = false;

    public Arrow(int x_position, int y_position) {
        x = x_position;
        y = y_position;
    }

    public int move(int y_movement) {
        y = y - y_movement; //movement
        return y;
    }
}
