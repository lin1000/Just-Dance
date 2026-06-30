package com.lin1000.justdance.beats;

import com.lin1000.justdance.controller.ConditionController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

//=====================================================================
//利用producer，本身是一個執行緒，會自動的按照舞步產生箭頭，至於箭頭產
//出後如何往上移，完全是由move()Function在控制，特別的是跟producer本身，
//無關，因為move是給外面的program去呼叫的，
//=======================================================================
public class ArrowsProducer extends Object implements Runnable
{

        @SuppressWarnings("unchecked")
        public List<Arrow>[] vec = new CopyOnWriteArrayList[4]; // 上下左右箭頭
        public Thread produceThread;

        //上下左右箭頭的X軸位置
        public int position_left;
        public int position_down;
        public int position_up;
        public int position_right;

        //處理舞步檔
        File foot_file;
        FileInputStream foot;

        //歌曲參數BPM
        public int BPM;

        //stop indicator — volatile ensures the producer thread sees the write immediately
        private volatile boolean isStop = false;


        public ArrowsProducer(int position_left, int position_down, int position_up, int position_right, int BPM)
        {
                vec[0] = new CopyOnWriteArrayList<>();
                vec[1] = new CopyOnWriteArrayList<>();
                vec[2] = new CopyOnWriteArrayList<>();
                vec[3] = new CopyOnWriteArrayList<>();

                //指定上下左右箭頭的X軸位置
                this.position_left= position_left;
                this.position_down= position_down;
                this.position_up= position_up;
                this.position_right= position_right;

                //歌曲參數
                this.BPM=BPM;

                //讀取舞步檔
                try{
                        foot_file=new File("./foot/foot.txt");
                        foot=new FileInputStream(foot_file);
                }
                catch(java.io.FileNotFoundException e){ e.printStackTrace(); }

                //執行緒
                produceThread = new Thread(this);
                produceThread.start();

        }

        public void run()
        {
                while(true)
                {
                        try{
                                Thread.sleep(300); //控制BPM!!!Beats Per Minute
                        }catch(InterruptedException e){
                                e.printStackTrace();
                        }
                        if(isStop) break;
                        produce(); //核心
                }
        }

        public void stop()
        {
                isStop = true;
                try { if (foot != null) foot.close(); } catch (IOException e) { e.printStackTrace(); }
        }

        public void produce()
        {
                try
                {
                        System.out.println("***Generation Foot steps in ArrowProducer***");
                //讀取舞步檔!! input為ASCII碼，如 0-->讀出來變48 , eof=-1
                int b0 = foot.read();
                if (b0 == -1) {        // end of the dance chart — stop spawning new arrows
                        isStop = true;
                        return;
                }
                int input[]={b0-48,foot.read()-48,foot.read()-48,foot.read()-48};

                if(input[0]==1) vec[0].add(new Arrow(position_left,730));
                if(input[1]==1) vec[1].add(new Arrow(position_down,730));
                if(input[2]==1) vec[2].add(new Arrow(position_up,730));
                if(input[3]==1) vec[3].add(new Arrow(position_right,730));

                foot.skip(1);

                }catch(java.io.IOException e){e.printStackTrace();}

        }

        // Advance every on-screen arrow upward by `dyPixels` (a fractional, time-scaled
        // amount computed by the caller from elapsed audio time). Arrows that scroll past
        // the top are removed and counted as a MISS.
        public void move(ConditionController conditionControl, double dyPixels)
        {
        //讓每個vec裡的箭頭往上走
                for(int vec_index=0;vec_index<4;vec_index++)
                {
                        List<Arrow> toRemove = new ArrayList<>();
                        for (Arrow myarrow : vec[vec_index])
                        {
                                int remove_or_not=myarrow.move(dyPixels);
                                if(remove_or_not < 0)
                                {
                                        toRemove.add(myarrow);
                                        conditionControl.setCondition(3); //MISS
                                }
                        }
                        vec[vec_index].removeAll(toRemove);
                }

        }
}
