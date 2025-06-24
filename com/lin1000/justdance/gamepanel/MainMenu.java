package com.lin1000.justdance.gamepanel;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.*;


import com.github.strikerx3.jxinput.XInputDevice;
import com.github.strikerx3.jxinput.listener.XInputDeviceListener;
import com.lin1000.justdance.XInputDevice.MainMenuXInputDeviceListener;
import com.lin1000.justdance.controller.SoundController;
import com.lin1000.justdance.gamepanel.action.MainMenuAction;
import com.lin1000.justdance.gamepanel.input.KeyboardControllerInput;
import com.lin1000.justdance.gamepanel.input.XBoxControllerInput;
//import de.hardcode.jxinput.JXInputManager;
//import de.hardcode.jxinput.JXInputDevice;
//import de.hardcode.jxinput.test.ButtonListener;
//import de.hardcode.jxinput.event.JXInputButtonEvent;
//import de.hardcode.jxinput.event.JXInputEventManager;

public class MainMenu extends JWindow
{
        //Windows variable
        private Window window = null;

        //?n?±???b?Y???L??A??ΦbGui????
        private boolean direct[]=new boolean[4];

        //Joystick Device passed into Main Menu
        private XInputDevice device = null;

        //Joystick listener cache
        XInputDeviceListener listener = null;
        //paint
        private Dimension dim;
        private Image buffer;
        private Graphics gc;
        //??h???
        Image mark;
        Image menutitle;
        Image background;
        Image option[]=new Image[4];
        Image optionSelected[]=new Image[4];
        public static int musicOptionIndex =0; // 0,1,2,3
        
        //Sound Controller
        public SoundController soundController;
                       
        //Game Main Control Flow
        public static int controlFlow=1; //1,2,3,4(exit)
        
        //Which Music
        //y_movement
        //BPM(Beats per Minutes)
        private int whichmusic;
        private int y_movement;
        private int BPM;
        
        //played multiple times
        private boolean multiplePlay=false;

        // lock object for synchronization
        public final Object pauseLock = new Object();
        public boolean pause = false;

        public MainMenu(Project project, boolean multiplePlay, XInputDevice device, SoundController soundController, GraphicsDevice activeScreen)
        {
            super(project);
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
                    KeyboardControllerInput keyboardControllerInput = new KeyboardControllerInput();
                    keyboardControllerInput.setKeyEvent(e);
                    keyboardControllerInput.setPressed(true);
                    MainMenuAction.getInstance().inputAction(keyboardControllerInput,MainMenu.this);
                }
                public void keyReleased(KeyEvent e) {
                    //Translate Keyboard Event into InputType
                    KeyboardControllerInput keyboardControllerInput = new KeyboardControllerInput();
                    keyboardControllerInput.setKeyEvent(e);
                    keyboardControllerInput.setPressed(false);
                    MainMenuAction.getInstance().inputAction(keyboardControllerInput,MainMenu.this);
                }
            });

            window.setVisible(true);
            window.requestFocusInWindow();

            //temp
        	this.multiplePlay=multiplePlay;
            getContentPane().setBackground(Color.white);

            //double buffering
            dim = getSize();
            buffer = createImage(dim.width, dim.height);
            gc = buffer.getGraphics();

            //loading image
            loadImage();

            //Sound Controller
            this.soundController = soundController;
            //setup joystick and register joystic event listener
            this.device = device;
            if (device != null) {
                // The SimpleXInputDeviceListener allows us to implement only the methods we actually need
                this.listener = new MainMenuXInputDeviceListener(this);

                //add listener
                device.addListener(listener);

            } else {
                System.err.println("System have no input devices");
                throw new RuntimeException("JXInputDevice is null");
            }
            //JXInputEventManager.setTriggerIntervall( 5 );
            if (!multiplePlay)//???O??@????
            {
                //soundControl.play_beginSound(2);
                paintInitial(0);//
                try {
                    Thread.sleep(2500);
                } catch (java.lang.InterruptedException e) {
                }
                soundController.playMainMenuSound(0);

                int paintIndex = 0;
                while (controlFlow == 1) {
                            try {
                                if (device.poll()) {
                                    // 輪詢控制器狀態，觸發事件
                                    MainMenuXInputDeviceListener.calculateAxis(device);
                                }
                                Thread.sleep(50);
                                paintInitial(paintIndex++);
                            } catch (java.lang.InterruptedException e) {
                            }

                            if(controlFlow == 4) // leave game directly
                                System.exit(0);

                            paintIndex %= 20;


                }
                mark = null;
            }

            if (controlFlow == 1) controlFlow = 2;
            while (controlFlow == 2) {
                try {
                    if (device.poll()) {
                        // 輪詢控制器狀態，觸發事件
                        MainMenuXInputDeviceListener.calculateAxis(device);
                    }
                    Thread.sleep(50);
                    menuscreen();
                    //controlFlow=3;
                } catch (java.lang.InterruptedException e) {
                }

            }

            this.device.removeListener(this.listener);

        }

        
        public void update(Graphics g)
        {
                paint(g);
        }

        public void paint(Graphics g) 
        {
                try{
                    //?p?G?s?Fsuper.paint?e???|?{?{,?]??super.paint()?|?h????M???e??????@
                    g.drawImage(buffer,0,0,dim.width,dim.height,this);

                }catch(java.lang.NullPointerException f){}
        }
        
        //loadImage what paint need
        public void loadImage()
        {                
                Toolkit kit=Toolkit.getDefaultToolkit();
                mark=kit.getImage("img/mark.jpg");
                background=kit.getImage("img/background.jpg");
                menutitle=kit.getImage("img/menutitle.jpg");
                
                option[0]=kit.getImage("img/option1.jpg");
                option[1]=kit.getImage("img/option2.jpg");
                option[2]=kit.getImage("img/option3.jpg");
                option[3]=kit.getImage("img/option4.jpg");
                optionSelected[0]=kit.getImage("img/option1selected.jpg");
                optionSelected[1]=kit.getImage("img/option2selected.jpg");
                optionSelected[2]=kit.getImage("img/option3selected.jpg");
                optionSelected[3]=kit.getImage("img/option4selected.jpg");
        }
        
        
        //?e?X?}?Y?e??
        public void paintInitial(int paintIndex)
        {
                //-- clear background -->
                        gc.setColor( Color.black );
                        gc.fillRect( 0, 0, dim.width, dim.height );
                //-- clear background -->
        	                        
                        gc.drawImage(mark,112,60,800,600,this);
                                              
                        gc.setColor(Color.white);
                        gc.setFont(new Font("verdana",Font.PLAIN,20));
                        if(paintIndex > 5)  {gc.drawString("Press Start Button",440,600);}

                repaint();
                        
        }
        
        //?e?X?D???
        public void menuscreen()
        {
                        gc.drawImage(background,0,0,1024,768,this);
                        gc.drawImage(menutitle,255,80,555,60,this);
                        
                        switch(musicOptionIndex)
                        {			
                        	case 0:	this.whichmusic=0;//??whichmusic?M?w????
                        		this.y_movement=15;//??y_movement?M?w????t??
                        		this.BPM=400;//??BPM?M?w?`???
                        		gc.drawImage(option[1],255,260,555,60,this);
                        		gc.drawImage(option[2],255,320,555,60,this);
                        		gc.drawImage(option[3],255,380,555,60,this);
                        		gc.drawImage(optionSelected[0],213,195,640,69,this);
                        		break;
                        	case 1: this.whichmusic=1;//??whichmusic?M?w????
                        		this.y_movement=7;//??y_movement?M?w????t??
                        		this.BPM=120;//??BPM?M?w?`???
                        		gc.drawImage(option[0],255,200,555,60,this);
                        		gc.drawImage(option[2],255,320,555,60,this);
                        		gc.drawImage(option[3],255,380,555,60,this);
                        		gc.drawImage(optionSelected[1],213,255,640,69,this);
                        		break;
                        	case 2:	this.whichmusic=2;//??whichmusic?M?w????
                        		this.y_movement=5;//??y_movement?M?w????t??
                        		this.BPM=180;//??BPM?M?w?`???
                        		gc.drawImage(option[0],255,200,555,60,this);
                        		gc.drawImage(option[1],255,260,555,60,this);
                        		gc.drawImage(option[3],255,380,555,60,this);
                        		gc.drawImage(optionSelected[2],213,315,640,69,this);
                        		break;
                        	case 3:	this.whichmusic=3;//??whichmusic?M?w????
                        		this.y_movement=12;//??y_movement?M?w????t??
                        		this.BPM=300;//??BPM?M?w?`???
                        		gc.drawImage(option[0],255,200,555,60,this);
                        		gc.drawImage(option[1],255,260,555,60,this);
                        		gc.drawImage(option[2],255,320,555,60,this);
                        		gc.drawImage(optionSelected[3],213,375,640,69,this);
                        		break;
                        }
                        repaint();
        }
        
        //return controlflow
        public int getcontrolFlow()
        {
            return this.controlFlow;
        }
        
        //return music no.
        public int getwhichMusic()
        {
        	return this.whichmusic;
        }
        
        //return y_movement
        public int getMovement()
        {
        	return this.y_movement;
        }

        //return BPM
        public int getBPM()
        {
        	return this.BPM;
        }
}


