package com.lin1000.justdance.beats;

import com.lin1000.justdance.controller.ConditionController;
import com.lin1000.justdance.song.sm.Simfile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

//=====================================================================
// Holds a song's chart as beat-timed notes (from a parsed .sm) and drives
// them off the audio clock. Each note's target time is computed once from
// its beat via the simfile's tempo map; every tick, update() spawns notes
// as they enter view, repositions on-screen arrows by (targetTime - now),
// and culls misses. vec[] are the currently visible arrows per lane.
//=====================================================================
public class ArrowsProducer extends Object
{
        @SuppressWarnings("unchecked")
        public List<Arrow>[] vec = new CopyOnWriteArrayList[4]; // active on-screen arrows: L D U R

        //上下左右箭頭的X軸位置
        public int position_left;
        public int position_down;
        public int position_up;
        public int position_right;

        //歌曲參數BPM
        public int BPM;

        // Y at which a note first appears (bottom of the field); it rises to the judge line.
        private static final int SPAWN_Y = 730;

        // All chart notes as timed Arrows, sorted by target time; spawned into vec[] as they near.
        private final List<Arrow> pending = new ArrayList<>();
        private int nextPending = 0;

        //stop indicator — volatile so a stop() from another thread is seen immediately
        private volatile boolean isStop = false;

        public ArrowsProducer(int position_left, int position_down, int position_up, int position_right, int BPM, Simfile simfile)
        {
                for (int i = 0; i < 4; i++) vec[i] = new CopyOnWriteArrayList<>();
                this.position_left = position_left;
                this.position_down = position_down;
                this.position_up   = position_up;
                this.position_right = position_right;
                this.BPM = BPM;
                buildNotes(simfile);
        }

        private int laneX(int lane) {
                switch (lane) {
                        case 0:  return position_left;
                        case 1:  return position_down;
                        case 2:  return position_up;
                        default: return position_right;
                }
        }

        // Convert the playable chart's (lane, beat) notes into timed Arrows once, up front.
        private void buildNotes(Simfile sm) {
                if (sm == null) return;
                Simfile.Chart chart = sm.playableChart();
                if (chart == null) return;
                for (Simfile.Note n : chart.notes) {
                        double t = sm.timing.beatToSeconds(n.beat);
                        pending.add(new Arrow(laneX(n.lane), n.lane, t));
                }
                pending.sort((a, b) -> Double.compare(a.targetTimeSec, b.targetTimeSec));
                System.out.println("ArrowsProducer: built " + pending.size() + " notes from chart");
        }

        /**
         * One simulation step, driven by the audio clock. Spawns notes as they enter view,
         * repositions all on-screen arrows by their target time, and culls notes that rose past
         * the top un-hit (MISS). Called from Dance.tick() on the EDT.
         */
        public void update(double nowSec, double pxPerSec, int judgeY, ConditionController conditionControl) {
                if (isStop) return;

                // A note enters view when its Y would reach SPAWN_Y, i.e. it's within the lead time.
                double leadSec = (pxPerSec > 0) ? (SPAWN_Y - judgeY) / pxPerSec : 0;
                while (nextPending < pending.size()
                        && pending.get(nextPending).targetTimeSec - nowSec <= leadSec) {
                        Arrow a = pending.get(nextPending++);
                        vec[a.lane].add(a);
                }

                for (int lane = 0; lane < 4; lane++) {
                        List<Arrow> toRemove = null;
                        for (Arrow a : vec[lane]) {
                                int y = a.updateY(nowSec, pxPerSec, judgeY);
                                if (y < 0) { // rose past the top without being hit
                                        if (toRemove == null) toRemove = new ArrayList<>();
                                        toRemove.add(a);
                                        conditionControl.setCondition(3); // MISS
                                }
                        }
                        if (toRemove != null) vec[lane].removeAll(toRemove);
                }
        }

        public void stop() { isStop = true; }
}
