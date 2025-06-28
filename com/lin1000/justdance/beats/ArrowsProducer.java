package com.lin1000.justdance.beats;

import com.lin1000.justdance.controller.ConditionController;

import java.io.File;
import java.io.FileInputStream;
import java.util.Vector;

//=====================================================================
//利用producer，本身是一個執行緒，會自動的按照舞步產生箭頭，至於箭頭產
//出後如何往上移，完全是由move()Function在控制，特別的是跟producer本身，
//無關，因為move是給外面的program去呼叫的，
//
//vector的用法
//      1.宣告   Vector vec[]= new Vector[9];
//      2.實體化 vec[x]=new Vector;
//      3.新增   vec[x].addElement(Object obj);
//      4.取得   vec[x].get(int index);
//      5.刪除   vec[x].removeElementAt(int index);
//
//=======================================================================
public class ArrowsProducer extends Object implements Runnable
{

        public Vector vec[]= new Vector[4];; //vec陣列儲存上下左右箭頭
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
        
                        
        public ArrowsProducer(int position_left, int position_down, int position_up, int position_right, int BPM)
        {
                //把四個Vector實體化，一定要用
                vec[0]=new Vector();
                vec[1]=new Vector();
                vec[2]=new Vector();
                vec[3]=new Vector();
                
                //指定上下左右箭頭的X軸位置
                this.position_left= position_left;
                this.position_down= position_down;
                this.position_up= position_up;
                this.position_right= position_right;
                
                //歌曲參數
                //設定BPM
                this.BPM=BPM;
                
                //讀取舞步檔exception!!!!!!!!!
                try{
                        foot_file=new File("./foot/foot.txt");
                        foot=new FileInputStream(foot_file);
                }
                catch(java.io.FileNotFoundException e){}
                catch(java.io.IOException e){} 
                
                //執行緒
                produceThread = new Thread(this);
                produceThread.start();
                                
        }
        
        public void run()
        {
                while(true)
                {
                        try{

                                //System.out.println ("1.(1000L)/(BPM/60L)="+(1000f)/(BPM/60f));
                                //System.out.println ("2.(1000L)/(BPM/60L)="+(long)((1000f)/(BPM/60f)));
                                //Thread.sleep((long)((1000f)/(BPM/60f))); //控制BPM!!!Beats Per Minute
                                Thread.sleep(300); //控制BPM!!!Beats Per Minute
                        }catch(InterruptedException e){
                                e.printStackTrace();
                        }
                        produce();//核心
                }
        }
        
        public void stop()
        {
                produceThread=null;
        }
                
        public void produce()
        {
                try
                {
                        System.out.println("***Generation Foot steps in ArrowProducer***");
                //極重要!!讀取舞步檔!!input為ASCII碼，如 0-->讀出來變48 ,eof=-1
                int input[]={foot.read()-48,foot.read()-48,foot.read()-48,foot.read()-48};
                //int input[]={(int)(Math.random()*2)%2,(int)(Math.random()*2)%2,(int)(Math.random()*2)%2,(int)(Math.random()*2)%2};
                                                        
                if(input[0]==1) vec[0].addElement(new Arrow(position_left,730));
                if(input[1]==1) vec[1].addElement(new Arrow(position_down,730));
                if(input[2]==1) vec[2].addElement(new Arrow(position_up,730));
                if(input[3]==1) vec[3].addElement(new Arrow(position_right,730));
                
                //if(input[0]==-1 || input[1]==-1 || input[2]==-1 || input[3]==-1) ; //檔案結束
                
                foot.skip(1);
                
                }catch(java.io.IOException e){e.printStackTrace();}
                
        }
        
        public void move(ConditionController conditionControl, int y_movement)
        {
        //y_movement是變數依歌而定走幾格
        //讓每個vec裡的箭頭往上走
                for(int vec_index=0;vec_index<4;vec_index++)
                {               
                        for(int element_index=0;element_index < vec[vec_index].size();element_index++)
                        {
                                Arrow myarrow=(Arrow)vec[vec_index].get(element_index);
                                int remove_or_not=myarrow.move(y_movement);
                                
                                //檢測要不要把箭頭kick掉，如果kick掉顯示MISS然後perfectCounter重設
                                if(remove_or_not < 0)
                                {
                                	vec[vec_index].removeElementAt(element_index); 
                                	conditionControl.setCondition(3); //MISS
                                }
                        }
                }
        
        }
}
