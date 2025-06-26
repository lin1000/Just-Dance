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
import com.lin1000.justdance.gamepanel.effect.EffectManager;
import com.lin1000.justdance.gamepanel.input.KeyboardControllerInput;

public class Dance extends JWindow
{
    //Windows variable
    private Window window;
    private boolean isSuperPaint = true;
    private Project project = null;

    //�n�±���b�Y���L�ȡA�ϥΦbGui����
    public boolean direct[] = new boolean[4];

    //Joystick Device passed into Main Menu
    private XInputDevice device = null;

    //Joystick listener cache
    XInputDeviceListener listener = null;

    //paint�n�Ψ쪺
    public Dimension dim;
    public Image buffer;
    public Graphics gc;
    //�ܦh�Ϥ�
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


    //���ͽb�Y��class producer
    public BeatsProducer producer;
    //���p�����
    public ConditionController conditionControl;
    //���ֱ����
    public SoundController soundController;

    //Game Area Offset
    public final int g_off_x = 450; // �C���ϰ�X�����q
    public final int g_off_y = 0; // �C���ϰ�Y�����q

    //����W�ƽb�Y����m
    public int arrow_y_position = 50;
    //public int width = 1280, height = 720;
    public int width = 2560, height = 1440;
    private final int dropSpeed = 300; // �C����300����
    private final int judgeLineY = 500; // �P�w�uY�y��

    //�q���Ѽ�
    //music���ح�,���ʳt�׭�
    public int music;
    public int y_movement;
    public int BPM;

    //GAMEOVER�M���|�Ψ쪺ø�ϰѼ�
    public int gameover_left = 1;
    public int gameover_right = 500;

    //�ͩRLIFE��ܰ϶�
    public int life_x = 20;
    public int life_y = 670;

    //Game Special Effects
    public final EffectManager effectManager = new EffectManager(g_off_x,g_off_y);

    //constructor�ǤJ�Ȭ�����
    public Dance(Project project, int whichmusic, int y_movement, int BPM, XInputDevice device, SoundController soundController, GraphicsDevice activeScreen) {
        super(project);
        this.project = project;
        //�q���Ѽ�
        this.music = whichmusic;
        this.y_movement = y_movement;
        this.BPM = BPM;

        System.out.println("whichmusic=" + whichmusic);
        System.out.println("y_movement=" + y_movement);
        System.out.println("BPM=" + BPM);

        //JFrame
        window = this;
        if (activeScreen != null) {
            activeScreen.setFullScreenWindow(this);
            Rectangle bounds = activeScreen.getDefaultConfiguration().getBounds();
            int x = bounds.x + (bounds.width - this.getWidth()) / 2;
            int y = bounds.y + (bounds.height - this.getHeight()) / 2;
            bounds.setLocation(x,y);
            this.setBounds(bounds);
            life_x = 20;
            life_y = bounds.height - 98;
            System.out.println("bounds.width="+ bounds.width);
            System.out.println("bounds.height="+bounds.height);
            System.out.println("this.getWidth()="+ this.getWidth());
            System.out.println("this.getHeight()="+ this.getHeight());
            System.out.println("bounds.x="+ bounds.x);
            System.out.println("bounds.y="+ bounds.y);

        } else {
            // �S���ĤG�ù��N��ܦb�D�ù�����
            this.setLocationRelativeTo(null);
            setSize(1024,768);
            life_x = 20;
            life_y = 670;
            activeScreen.setFullScreenWindow(this);
        }

        // �[�W KeyListener�]�ݳ]�w focusable�^
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

        //��l�Ʊ��p����� (must before window visible paint or it will not work)
        conditionControl = new ConditionController(this);
        conditionControl.setCondition(7);

        window.setVisible(true);
        window.requestFocus();
        getContentPane().setBackground(Color.white);

        //double buffering
        dim = getSize();
        System.out.println("dim.width=" + dim.width);
        System.out.println("dim.height=" + dim.height);



        //loading image
        loadImage();

        //��l�ƫ��s���A
        direct[0] = false;
        direct[1] = false;
        direct[2] = false;
        direct[3] = false;

        //��l�Ʒn��setup joystick
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

        producer = new BeatsProducer(30, 130, 230, 330, this.BPM);//produce�O�@��thread�|���ͽb�Y��ABPM�q���ѼƸ`���

        //Setting up and start counting the rhythm nanos
        this.soundController = soundController;
        //�Ȯɮ���20250510: device.removeListener(listener);

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

                                        //�]�w���������
                                        new ButtonListener(dev.getButton(2))
                                        {                                                
                                                public void changed(JXInputButtonEvent ev) 
                                                {                                                                                                        
                                                     	
                                                        	if(ev.getButton().getState())//true
                                                        	{
                                                        		if(conditionControl.getGameOver())
                                                        		{
                                                         			conditionControl.setCondition(5);//5�N��exit
                                                         		}
                                                               	}
                                                        	                                                                                                                                                                        
                                                        //repaint();
        
                                                }        
                                        }; 
                                        //�]�wesc����
                                        new ButtonListener(dev.getButton(8))
                                        {                                                
                                                public void changed(JXInputButtonEvent ev) 
                                                {                                                                                                        
                                                     	
                                                        	if(ev.getButton().getState())//true
                                                        	{
                                                        		
                                                         			conditionControl.setCondition(5);//5�N��exit
                                                              	}
                                                        	                                                                                                                                                                        
                                                        //repaint();
        
                                                }        
                                        }; 
                                        
                                        //�]�w�T�w�����
                                        new ButtonListener(dev.getButton(1))
                                        {                                                
                                                public void changed(JXInputButtonEvent ev) 
                                                {                                                                                                        
                                                     	
                                                        	if(ev.getButton().getState())//true
                                                        	{
                                                        		if(conditionControl.getGameOver())
                                                        		{
                                                         			conditionControl.setCondition(6);//6�N��playagain
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
                System.out.println("nowMicros=" + nowMicros);
                System.out.println("elapsedSeconds=" + elapsedSeconds);


                // screen control logic 20250510
                try {
                    int removecondition = 0;
                    //while (true) {
                    if (device.poll()) {
                        // ���߱�����A�AĲ�o�ƥ�
                        DanceXInputDeviceListener.calculateAxis(device);
                    }

                    //�w�ɧ�r������
                    if ((removecondition %= 200) == 0) {
                        //conditionControl.setCondition(7);
                    }
                    removecondition++;

                    //tohandle:
                    producer.move(conditionControl, this.y_movement);//�q���Ѽ�y_movement
                    //tohandle:repaint();



                    //}

//                if (device.poll()) {
//                    // ���߱�����A�AĲ�o�ƥ�
//                    com.lin1000.justdance.XInputDevice.DanceXInputDeviceListener.calculateAxis(device);
//                }

//                while (!(conditionControl.getExit())) {
//                    if (device.poll()) {
//                        // ���߱�����A�AĲ�o�ƥ�
//                        com.lin1000.justdance.XInputDevice.DanceXInputDeviceListener.calculateAxis(device);
//                    }
//                    Thread.sleep(20);
//                    repaint();
//
//                } //���}���q
                    //�����Ҧ�����
//                this.soundController.stop_all();
                } catch (Exception e) {
                    //throw new RuntimeException(e);
                    e.printStackTrace();
                }

            //double buffering of entire screen
            //dim = getSize();
            buffer = createImage(width, height);
            gc = buffer.getGraphics();

//            catch (java.lang.InterruptedException e) {
//            }

            try {
                //�p�G�s�Fsuper.paint�e���|�{�{,�]��super.paint()�|�h����M���e�����ʧ@
                if (isSuperPaint) {
                    super.paint(g);
                    isSuperPaint = false;

                }

                //-- clear background -->
                gc.setColor(Color.white);
                gc.fillRect(0, 0, width, height);
                //-- clear background -->

                //-- �b�Y������ -->
                if (!this.direct[0]) gc.drawImage(image_left, g_off_x+30, g_off_y+arrow_y_position, this);
                else gc.drawImage(image_leftfill, g_off_x+30, g_off_y+arrow_y_position, this);
                if (!this.direct[1]) gc.drawImage(image_down, g_off_x+130, g_off_y+arrow_y_position, this);
                else gc.drawImage(image_downfill, g_off_x+130, g_off_y+arrow_y_position, this);
                if (!this.direct[2]) gc.drawImage(image_up, g_off_x+230, g_off_y+arrow_y_position, this);
                else gc.drawImage(image_upfill, g_off_x+230, g_off_y+arrow_y_position, this);
                if (!this.direct[3]) gc.drawImage(image_right, g_off_x+330, g_off_y+arrow_y_position, this);
                else gc.drawImage(image_rightfill, g_off_x+330, g_off_y+arrow_y_position, this);
                //-- �b�Y������ -->

                if (!(conditionControl.getGameOver())) {
                    gc.setColor(Color.black);
                    //��e���W�Ҧ����b�Y�e�X�ӡA�@�|��vec[0,1,2,3]
                    for (int vec_index = 0; vec_index < 4; vec_index++) {
                        for (int element_index = 0; element_index < producer.vec[vec_index].size(); element_index++) {
                            arrow myarrow = (com.lin1000.justdance.beats.arrow) producer.vec[vec_index].get(element_index);
                            gc.drawImage(arrow[vec_index], g_off_x+myarrow.x, g_off_y+myarrow.y, null);
                        }
                    }

                    //�����Xjudgementor--conditionControl���p����ߪ����p
                    //gc.setFont(new Font("verdana", Font.PLAIN, 80));
                    if (conditionControl.getCondition() == 0)
                        effectManager.addTextFlashEffect("PERFECT",g_off_x+((width-g_off_x)/6), g_off_y+(height/2));
                    if (conditionControl.getCondition() == 1)
                        effectManager.addTextFlashEffect(conditionControl.getperfectCounter() + "GOOD",g_off_x+((width-g_off_x)/6), g_off_y+(height/2));
                    //gc.drawString(conditionControl.getperfectCounter() + "COMBO", 120, conditionControl.getY());
                    if (conditionControl.getCondition() == 2)
                        effectManager.addTextFlashEffect("GOOD",g_off_x+((width-g_off_x)/6), g_off_y+(height/2));
                        //gc.drawString("  GOOD", 120, conditionControl.getY());
                    if (conditionControl.getCondition() == 3) {
                        effectManager.addTextFlashEffect("Miss", g_off_x + ((width - g_off_x) / 6), g_off_y + (height / 2));
                        conditionControl.setCondition(7);
                    }
                        //gc.drawString("  Miss", 120, conditionControl.getY());


                    //�������
                    gc.setFont(new Font("verdana", Font.PLAIN, 18));
                    gc.drawString("SCORE:", g_off_x+20, g_off_y+20);
                    gc.drawString(conditionControl.getScore() + "", g_off_x+100, g_off_y+20);

                    //�ͩR���
                    gc.drawString("LIFE", g_off_x+life_x, g_off_y+life_y);
                    gc.setColor(Color.red);
                    gc.drawRect(g_off_x+life_x, g_off_y+life_y + 10, 300, 25);
                    gc.fillRect(g_off_x+life_x, g_off_y+life_y+10, conditionControl.getLife() * 3, 25);

                } else {
                    gc.setColor(Color.black);
                    gc.fillRect(0, 0, gameover_left, height);
                    gc.fillRect(gameover_right, 0, width, height);
                    gameover_left *= 2.8;
                    gameover_right /= 1.1;
                    if (gameover_left >= 480 && gameover_right <= 50) {
                        gameover_left = 500;
                        gameover_right = 0;
                        gc.setColor(Color.white);
                        gc.setFont(new Font("�ж���", Font.PLAIN, 26));
                        gc.drawString("GAME OVER", 155, (height / 2) - 20);
                        gc.drawString("(A) DANCE!!!  (X) EXIT", 110, (height / 2) + 15);
                    }
                }


                //FPS Info Block
                int fps_x = g_off_x+300;
                int fps_y = g_off_y+20;
                int fps_w = 150;
                int fps_h = 20;
                gc.setColor(Color.white);
                gc.fillRect(fps_x,fps_y,fps_w,fps_h);
                gc.setColor(Color.blue);
                gc.drawString("Seconds:", fps_x, fps_y);
                gc.drawString(elapsedSeconds + "", fps_x+80, fps_y);
                gc.setColor(Color.BLACK);
                gc.drawRect(1,1,width-2, height-2);
                //�e���䪺�H
                gc.drawImage(image_leftman, 0, 0, 360, 669, null);
                //Special Effects
                effectManager.drawAll((Graphics2D) gc);
                g.drawImage(buffer, 0, 0, width, height, this);


            } catch (java.lang.NullPointerException f) {
                f.printStackTrace();
            }

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





