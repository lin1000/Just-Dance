package com.lin1000.justdance.beats;

import com.lin1000.justdance.controller.ConditionController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

//=====================================================================
//Producer holds the dance chart and spawns arrows. Spawn timing is no
//longer wall-clock based: the whole chart is pre-loaded once, and rows
//are emitted on demand by spawnDueArrows(nowSec, ...) — driven by the
//audio sample position from the single game tick. move() advances the
//on-screen arrows; both are called from Dance.tick() on the EDT.
//=======================================================================
public class ArrowsProducer extends Object
{

        @SuppressWarnings("unchecked")
        public List<Arrow>[] vec = new CopyOnWriteArrayList[4]; // 上下左右箭頭

        //上下左右箭頭的X軸位置
        public int position_left;
        public int position_down;
        public int position_up;
        public int position_right;

        //歌曲參數BPM
        public int BPM;

        // Pre-loaded dance chart: one int[4] (left/down/up/right, 0 or 1) per row.
        private final List<int[]> chartRows = new ArrayList<>();
        // Index of the next row that has not yet been spawned.
        private int nextRow = 0;

        //stop indicator — volatile so a stop() from another thread is seen immediately
        private volatile boolean isStop = false;


        public ArrowsProducer(int position_left, int position_down, int position_up, int position_right, int BPM, String chartPath)
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

                //讀取舞步檔 — per-song chart path (from SongMeta); loaded up front so no file
                //I/O happens during play
                loadChart(new File(chartPath));
        }

        // Parse the foot-step file into chartRows. The file is whitespace-separated
        // 4-character tokens (e.g. "0000 0010 0100"), each token one chart row.
        private void loadChart(File footFile)
        {
                try {
                        String content = new String(Files.readAllBytes(footFile.toPath()));
                        for (String token : content.split("\\s+")) {
                                if (token.length() != 4) continue; // skip blanks / malformed tokens
                                int[] row = new int[4];
                                for (int i = 0; i < 4; i++) {
                                        row[i] = (token.charAt(i) == '1') ? 1 : 0;
                                }
                                chartRows.add(row);
                        }
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }

        // Spawn every chart row whose scheduled time (rowIndex * rowIntervalSec) has been
        // reached by the audio clock. Locked to the audio sample position via nowSec, so
        // spawning advances exactly as the music plays — pausing on stalls, catching up on
        // seeks — instead of drifting on a wall-clock timer. Called from Dance.tick().
        public void spawnDueArrows(double nowSec, double rowIntervalSec)
        {
                if (isStop) return;
                while (nextRow < chartRows.size() && nextRow * rowIntervalSec <= nowSec) {
                        int[] r = chartRows.get(nextRow);
                        if (r[0] == 1) vec[0].add(new Arrow(position_left, 730));
                        if (r[1] == 1) vec[1].add(new Arrow(position_down, 730));
                        if (r[2] == 1) vec[2].add(new Arrow(position_up, 730));
                        if (r[3] == 1) vec[3].add(new Arrow(position_right, 730));
                        nextRow++;
                }
        }

        public void stop()
        {
                isStop = true;
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
