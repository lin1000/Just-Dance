package com.lin1000.justdance.gamepanel;

import java.awt.*;
import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;


import com.github.strikerx3.jxinput.XInputDevice;
import com.github.strikerx3.jxinput.listener.XInputDeviceListener;
import com.lin1000.justdance.XInputDevice.DanceXInputDeviceListener;
import com.lin1000.justdance.beats.BeatsProducer;
import com.lin1000.justdance.beats.arrow;
import com.lin1000.justdance.controller.ConditionController;
import com.lin1000.justdance.controller.SoundController;
import com.lin1000.justdance.gamepanel.action.DanceAction;
import com.lin1000.justdance.gamepanel.action.MainMenuAction;
import com.lin1000.justdance.gamepanel.input.KeyboardControllerInput;

public class Dance extends JWindow
{
    //Windows variable
    private Window window;
    private boolean isSuperPaint = true;
    private Project project = null;

    //搖捍控制箭頭布林值，使用在Gui反應
    public boolean direct[] = new boolean[4];

    //Joystick Device passed into Main Menu
    private XInputDevice device = null;

    //Joystick listener cache
    XInputDeviceListener listener = null;


    //paint要用到的
    public Dimension dim;
    public Image buffer;
    public Graphics gc;
    //很多圖片
    Image image_left;
    Image image_leftfill;
    Image image_up;
    Image image_upfill;
    Image image_down;
    Image image_downfill;
    Image image_right;
    Image image_rightfill;
    Image arrow[] = new Image[4];
    Image image_leftman;


    //產生箭頭的class producer
    public BeatsProducer producer;
    //情況控制中心
    public ConditionController conditionControl;
    //音樂控制中心
    public SoundController soundController;

    //控制上排箭頭的位置
    public int arrow_y_position = 50;
    public int width = 500, height = 768;
    private final int dropSpeed = 300; // 每秒移動300像素
    private final int judgeLineY = 500; // 判定線Y座標

    //歌曲參數
    //music曲目值,移動速度值
    public int music;
    public int y_movement;
    public int BPM;

    //GAMEOVER專門會用到的繪圖參數
    public int gameover_left = 1;
    public int gameover_right = 500;

    //constructor傳入值為曲目
    public Dance(Project project, int whichmusic, int y_movement, int BPM, XInputDevice device, SoundController soundController, GraphicsDevice activeScreen) {
        super(project);
        this.project = project;
        //歌曲參數
        this.music = whichmusic;
        this.y_movement = y_movement;
        this.BPM = BPM;

        System.out.println("whichmusic=" + whichmusic);
        System.out.println("y_movement=" + y_movement);
        System.out.println("BPM=" + BPM);

        //JFrame
        window = this;
        if (activeScreen != null) {
            Rectangle bounds = activeScreen.getDefaultConfiguration().getBounds();
            int x = bounds.x + (bounds.width - this.getWidth()) / 2;
            int y = bounds.y + (bounds.height - this.getHeight()) / 2;
            this.setLocation(x, y);
            this.setBounds(bounds);
        } else {
            // 沒有第二螢幕就顯示在主螢幕中央
            this.setLocationRelativeTo(null);
            setSize(1024,768);
        }

        // 加上 KeyListener（需設定 focusable）
        this.setFocusable(true);
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                //Translate Keyboard Event into InputType
                KeyboardControllerInput keybnoardControllerInput = new KeyboardControllerInput();
                keybnoardControllerInput.setKeyEvent(e);
                keybnoardControllerInput.setPressed(true);
                DanceAction.getInstance().inputAction(keybnoardControllerInput,Dance.this);
            }
            public void keyReleased(KeyEvent e) {
                //Translate Keyboard Event into InputType
                KeyboardControllerInput keyboardControllerInput = new KeyboardControllerInput();
                keyboardControllerInput.setKeyEvent(e);
                keyboardControllerInput.setPressed(false);
                DanceAction.getInstance().inputAction(keyboardControllerInput,Dance.this);
            }
        });

//        // 加上 KeyListener（需設定 focusable）
//        this.setFocusable(true);
//        this.addKeyListener(new KeyAdapter() {
//            @Override
//            public void keyPressed(KeyEvent e) {
//                System.out.println(e.getKeyCode());
//                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
//                    System.out.println("ESC pressed, closing window.");
//                    window.dispose();
//                    window.setVisible(false);
//                    ((Dance)window).conditionControl.setCondition(5);//exit
//                }
//            }
//        });

        window.setVisible(true);
        window.requestFocus();
        getContentPane().setBackground(Color.white);

        //double buffering
        dim = getSize();

        //loading image
        loadImage();

        //初始化情況控制中心
        conditionControl = new ConditionController(dim.width / 2, dim.height / 2);

        //初始化按鈕狀態
        direct[0] = false;
        direct[1] = false;
        direct[2] = false;
        direct[3] = false;

        //初始化搖捍setup joystick
        this.device = device;
        if (device != null) {
            // The SimpleXInputDeviceListener allows us to implement only the methods we actually need
            this.listener = new DanceXInputDeviceListener(this);

            //add listener
            device.addListener(listener);
        } else {
            System.err.println("System have no input devices");
            throw new RuntimeException("JXInputDevice is null");
        }

        producer = new BeatsProducer(30, 130, 230, 330, this.BPM);//produce是一個thread會產生箭頭喔，BPM歌曲參數節拍數

        //Setting up and start counting the rhythm nanos
        this.soundController = soundController;
        //暫時拿掉20250510: device.removeListener(listener);

//        //usually not triggered unless loading music has some issues.
//        while (!this.soundController.getStatus()) {
//            System.out.println("loading music");
//        }

    }
        
        
    /*    public void  initDevice()
        {
                int cnf=JXInputManager.getNumberOfDevices();
                if(cnf!=0)
                {
                        JXInputDevice dev = JXInputManager.getJXInputDevice(0);
                        
                        		//設定左鍵反應
                                        new ButtonListener(dev.getButton(12))
                                        {                                                
                                                public void changed(JXInputButtonEvent ev) 
                                                {                                                                                                        
                                                     	
                                                        	if(ev.getButton().getState())//true
                                                        	{
                                                         		direct[0]=true;//讓gui反應出有按到
                                                         		
                                                         		try
                                                         		{
                                                         		
                                                         		for(int element_index=0 ; element_index < 4 ; element_index++)
                                                         		{
                                                         			com.lin1000.justdance.beats.arrow myarrow =(com.lin1000.justdance.beats.arrow) produce.vec[0].get(element_index);
                                                         			
                                                         			//y=55~70是perfect
                                                         			if(myarrow.y >= 50 && myarrow.y <= 70 )
                                                         			{ 
                                                         				produce.vec[0].removeElementAt(element_index);
                                                         				conditionControl.setCondition(1);//１代表perfect
                                                         				soundControl.play_conditionSound(conditionControl.getCondition());
                                                         			}
                                                         			//y=71~90是good
                                                         			if(myarrow.y > 70 && myarrow.y <= 90)
                                                         			{
                                                         				 produce.vec[0].removeElementAt(element_index);
                                                         				 conditionControl.setCondition(2);//２代表good
                                                         				 soundControl.play_conditionSound(conditionControl.getCondition());
                                                         			}
                                                         			
                                                         		}
                                                         		
                                                         		}catch(java.lang.ArrayIndexOutOfBoundsException e){}
                                                         		
								}
                                                        	else//false
                                                        	{
                                                        		direct[0]=false;
                                                        	}
                                                                                                                                                                        
                                                        //repaint();
        
                                                }        
                                        };               
                                        //設定下鍵反應               
                                        new ButtonListener(dev.getButton(13))
                                        {                                                
                                                public void changed(JXInputButtonEvent ev) 
                                                {                                                                                                        
                                                     	
                                                        	if(ev.getButton().getState())//true
                                                        	{
                                                         		direct[1]=true;//讓gui反應出有按到
                                                         		
                                                         		try
                                                         		{
                                                         		
                                                         		for(int element_index=0 ; element_index < 4 ; element_index++)
                                                         		{
                                                         			com.lin1000.justdance.beats.arrow myarrow =(com.lin1000.justdance.beats.arrow) produce.vec[1].get(element_index);
                                                         			
                                                         			//y=55~70是perfect
                                                         			if(myarrow.y >= 50 && myarrow.y <= 70 )
                                                         			{ 
                                                         				produce.vec[1].removeElementAt(element_index);
                                                         				conditionControl.setCondition(1);//１代表perfect
                                                         				soundControl.play_conditionSound(conditionControl.getCondition());
                                                         			}
                                                         			//y=71~90是good
                                                         			if(myarrow.y > 70 && myarrow.y <= 90)
                                                         			{
                                                         				 produce.vec[1].removeElementAt(element_index);
                                                         				 conditionControl.setCondition(2);//２代表good
                                                         				 soundControl.play_conditionSound(conditionControl.getCondition());
                                                         			}
                                                         			
                                                         		}
                                                         		
                                                         		}catch(java.lang.ArrayIndexOutOfBoundsException e){}
                                                               	}
                                                        	else//false
                                                        	{
                                                        		direct[1]=false;
                                                        	}
                                                                                                                                                                        
                                                        //repaint();
        
                                                }        
                                        }; 
                                        //設定右鍵反應
                                        new ButtonListener(dev.getButton(14))
                                        {                                                
                                                public void changed(JXInputButtonEvent ev) 
                                                {                                                                                                        
                                                     	
                                                        	if(ev.getButton().getState())//true
                                                        	{
                                                         		direct[2]=true;//讓gui反應出有按到
                                                         		
                                                         		try
                                                         		{
                                                         		
                                                         		for(int element_index=0 ; element_index < 4 ; element_index++)
                                                         		{
                                                         			com.lin1000.justdance.beats.arrow myarrow =(com.lin1000.justdance.beats.arrow) produce.vec[3].get(element_index);
                                                         			
                                                         			//y=55~70是perfect
                                                         			if(myarrow.y >= 50 && myarrow.y <= 70 )
                                                         			{ 
                                                         				produce.vec[3].removeElementAt(element_index);
                                                         				conditionControl.setCondition(1);//１代表perfect
                                                         				soundControl.play_conditionSound(conditionControl.getCondition());
                                                         			}
                                                         			//y=71~90是good
                                                         			if(myarrow.y > 70 && myarrow.y <= 90)
                                                         			{
                                                         				 produce.vec[3].removeElementAt(element_index);
                                                         				 conditionControl.setCondition(2);//２代表good
                                                         				 soundControl.play_conditionSound(conditionControl.getCondition());
                                                         			}
                                                         			
                                                         		}
                                                         		
                                                         		}catch(java.lang.ArrayIndexOutOfBoundsException e){}
                                                         		
                                                               	}
                                                        	else//false
                                                        	{
                                                        		direct[2]=false;
                                                        	}
                                                                                                                                                                        
                                                        //repaint();
        
                                                }        
                                        }; 
                                        //設定上鍵反應
                                        new ButtonListener(dev.getButton(15))
                                        {                                                
                                                public void changed(JXInputButtonEvent ev) 
                                                {                                                                                                        
                                                     	
                                                        	if(ev.getButton().getState())//true
                                                        	{
                                                         		direct[3]=true;//讓gui反應出有按到
                                                         		
                                                         		try
                                                         		{
                                                         		
                                                         		for(int element_index=0 ; element_index < 4 ; element_index++)
                                                         		{
                                                         			com.lin1000.justdance.beats.arrow myarrow =(com.lin1000.justdance.beats.arrow) produce.vec[2].get(element_index);
                                                         			
                                                         			//y=55~70是perfect
                                                         			if(myarrow.y >= 50 && myarrow.y <= 70 )
                                                         			{ 
                                                         				produce.vec[2].removeElementAt(element_index);
                                                         				conditionControl.setCondition(1);//１代表perfect
                                                         				soundControl.play_conditionSound(conditionControl.getCondition());
                                                         			}
                                                         			//y=71~90是good
                                                         			if(myarrow.y > 70 && myarrow.y <= 90)
                                                         			{
                                                         				 produce.vec[2].removeElementAt(element_index);
                                                         				 conditionControl.setCondition(2);//２代表good
                                                         				 soundControl.play_conditionSound(conditionControl.getCondition());
                                                         			}
                                                         			
                                                         		}
                                                         		
                                                         		}catch(java.lang.ArrayIndexOutOfBoundsException e){}
                                                               	}
                                                        	else//false
                                                        	{
                                                        		direct[3]=false;
                                                        	}
                                                                                                                                                                        
                                                        //repaint();
        
                                                }        
                                        }; 
                                        //設定取消鍵反應
                                        new ButtonListener(dev.getButton(2))
                                        {                                                
                                                public void changed(JXInputButtonEvent ev) 
                                                {                                                                                                        
                                                     	
                                                        	if(ev.getButton().getState())//true
                                                        	{
                                                        		if(conditionControl.getGameOver())
                                                        		{
                                                         			conditionControl.setCondition(5);//5代表exit
                                                         		}
                                                               	}
                                                        	                                                                                                                                                                        
                                                        //repaint();
        
                                                }        
                                        }; 
                                        //設定esc反應
                                        new ButtonListener(dev.getButton(8))
                                        {                                                
                                                public void changed(JXInputButtonEvent ev) 
                                                {                                                                                                        
                                                     	
                                                        	if(ev.getButton().getState())//true
                                                        	{
                                                        		
                                                         			conditionControl.setCondition(5);//5代表exit
                                                              	}
                                                        	                                                                                                                                                                        
                                                        //repaint();
        
                                                }        
                                        }; 
                                        
                                        //設定確定鍵反應
                                        new ButtonListener(dev.getButton(1))
                                        {                                                
                                                public void changed(JXInputButtonEvent ev) 
                                                {                                                                                                        
                                                     	
                                                        	if(ev.getButton().getState())//true
                                                        	{
                                                        		if(conditionControl.getGameOver())
                                                        		{
                                                         			conditionControl.setCondition(6);//6代表playagain
                                                         		}
                                                               	}
                                                        	                                                                                                                                                                        
                                                        //repaint();
        
                                                }        
                                        }; 
                                        //alternative;
                                        //JXInputAxisEventListener listen =(JXInputAxisEventListener) new AxisListener(dev.getAxis(i));
                                        //JXInputEventManager.addListener(listen,dev.getAxis(i));
                                //}
                        //}
                                                
                }
        }*/
        
        public void update(Graphics g)
        {
                System.out.println("update before paint");
                paint(g);
        }

        public void paint(Graphics g) 
        {
            long nowMicros = 0;
            double elapsedSeconds = 0;
            if(soundController!= null) {
                nowMicros = System.nanoTime() / 1000;
                elapsedSeconds = (nowMicros - soundController.getStartTimeMicros()) / 1_000_000.0;
                //System.out.println("nowMicros=" + nowMicros);
                //System.out.println("elapsedSeconds=" + elapsedSeconds);
            }
            //double buffering
            //dim = getSize();
            buffer = createImage(width, height);
            gc = buffer.getGraphics();

            // screen control logic 20250510
            try {
                int removecondition = 0;
                //while (true) {
                    if (device.poll()) {
                        // 輪詢控制器狀態，觸發事件
                        DanceXInputDeviceListener.calculateAxis(device);
                    }

                    //定時把字幕消除
                    if ((removecondition %= 200) == 0) {
                        if (conditionControl.getCondition() != 1)
                            conditionControl.setCondition(0);
                    }
                    removecondition++;

                    //tohandle:
                    producer.move(conditionControl, this.y_movement);//歌曲參數y_movement
                    //tohandle:repaint();



                //}

//                if (device.poll()) {
//                    // 輪詢控制器狀態，觸發事件
//                    com.lin1000.justdance.XInputDevice.DanceXInputDeviceListener.calculateAxis(device);
//                }

//                while (!(conditionControl.getExit())) {
//                    if (device.poll()) {
//                        // 輪詢控制器狀態，觸發事件
//                        com.lin1000.justdance.XInputDevice.DanceXInputDeviceListener.calculateAxis(device);
//                    }
//                    Thread.sleep(20);
//                    repaint();
//
//                } //離開此歌
                //結束所有音樂
//                this.soundController.stop_all();
            } catch (Exception e) {
                //throw new RuntimeException(e);
            }
//            catch (java.lang.InterruptedException e) {
//            }

            try {
                //如果叫了super.paint畫面會閃爍,因為super.paint()會去執行清除畫面的動作
                if (isSuperPaint) {
                    super.paint(g);
                    isSuperPaint = false;
                }

                //畫左邊的人
                g.drawImage(image_leftman, 0, 0, 360, 669, null);

                //-- clear background -->
                gc.setColor(Color.white);
                gc.fillRect(0, 0, width, height);
                //-- clear background -->

                //-- 箭頭反應區 -->
                if (!this.direct[0]) gc.drawImage(image_left, 30, arrow_y_position, this);
                else gc.drawImage(image_leftfill, 30, arrow_y_position, this);
                if (!this.direct[1]) gc.drawImage(image_down, 130, arrow_y_position, this);
                else gc.drawImage(image_downfill, 130, arrow_y_position, this);
                if (!this.direct[3]) gc.drawImage(image_up, 230, arrow_y_position, this);
                else gc.drawImage(image_upfill, 230, arrow_y_position, this);
                if (!this.direct[2]) gc.drawImage(image_right, 330, arrow_y_position, this);
                else gc.drawImage(image_rightfill, 330, arrow_y_position, this);
                //-- 箭頭反應區 -->

                if (!(conditionControl.getGameOver())) {
                    gc.setColor(Color.black);
                    //把畫布上所有的箭頭畫出來，共四個vec[0,1,2,3]
                    for (int vec_index = 0; vec_index < 4; vec_index++) {
                        for (int element_index = 0; element_index < producer.vec[vec_index].size(); element_index++) {
                            arrow myarrow = (com.lin1000.justdance.beats.arrow) producer.vec[vec_index].get(element_index);
                            gc.drawImage(arrow[vec_index], myarrow.x, myarrow.y, null);
                        }

                    }

                    //反應出judgementor--conditionControl情況控制中心的情況
                    gc.setFont(new Font("verdana", Font.PLAIN, 40));
                    if (conditionControl.getCondition() == 1)
                        gc.drawString(conditionControl.getperfectCounter() + "COMBO", 120, conditionControl.getY());
                    if (conditionControl.getCondition() == 2) gc.drawString("  GOOD", 120, conditionControl.getY());
                    if (conditionControl.getCondition() == 3) gc.drawString("  Miss", 120, conditionControl.getY());

                    //分數顯示
                    gc.setFont(new Font("verdana", Font.PLAIN, 18));
                    gc.drawString("SCORE:", 20, 20);
                    gc.drawString(conditionControl.getScore() + "", 100, 20);

                    //生命顯示
                    gc.drawString("LIFE", 20, 670);
                    gc.setColor(Color.red);
                    gc.drawRect(20, 680, 300, 25);
                    gc.fillRect(20, 680, conditionControl.getLife() * 3, 25);

                } else {
                    gc.setColor(Color.black);
                    gc.fillRect(0, 0, gameover_left, height);
                    gc.fillRect(gameover_right, 0, 500, height);
                    gameover_left *= 2.8;
                    gameover_right /= 1.1;
                    if (gameover_left >= 480 && gameover_right <= 50) {
                        gameover_left = 500;
                        gameover_right = 0;
                        gc.setColor(Color.white);
                        gc.setFont(new Font("標階體", Font.PLAIN, 26));
                        gc.drawString("GAME OVER", 155, (height / 2) - 20);
                        gc.drawString("(A) DANCE!!!  (X) EXIT", 110, (height / 2) + 15);
                    }
                }

                //FPS Info Block
                int fps_x = 300;
                int fps_y = 20;
                int fps_w = 150;
                int fps_h = 20;
                gc.setColor(Color.white);
                gc.fillRect(fps_x,fps_y,fps_w,fps_h);
                gc.setColor(Color.blue);
                gc.drawString("Seconds:", fps_x, fps_y);
                gc.drawString(elapsedSeconds + "", fps_x+80, fps_y);
                g.drawImage(buffer, 450, 0, width, height, this);

            } catch (java.lang.NullPointerException f) {
                f.printStackTrace();
            }
                
        }
        
        //loadImage what paint need
        public void loadImage()
        {
                Toolkit kit=Toolkit.getDefaultToolkit();
                image_left=kit.getImage("img/left.gif");
                image_leftfill=kit.getImage("img/leftfill.gif");
                image_up=kit.getImage("img/up.gif");
                image_upfill=kit.getImage("img/upfill.gif");
                image_down=kit.getImage("img/down.gif");
                image_downfill=kit.getImage("img/downfill.gif");
                image_right=kit.getImage("img/right.gif");
                image_rightfill=kit.getImage("img/rightfill.gif");
                
                image_leftman=kit.getImage("img/leftman.jpg");
                
                arrow[0]=kit.getImage("img/jumpleft.gif");
                arrow[1]=kit.getImage("img/jumpdown.gif");
                arrow[2]=kit.getImage("img/jumpup.gif");
                arrow[3]=kit.getImage("img/jumpright.gif");
                
        }

    public Project getProject() {
        return project;
    }
}





