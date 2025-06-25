package com.lin1000.justdance.controller;

import com.lin1000.justdance.gamepanel.Dance;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.Vector;
import javax.sound.sampled.*;
import javax.swing.*;

//=============================================================
//音樂控制中心，即時反應出目前的任何condition
//
//=============================================================
public class SoundController extends Object implements Runnable
{
    private boolean status=false;//代表目前中心是否已經把音樂準備好

    private Clip danceClip;
    private Clip effectClip;
    private Clip mainMenuClip;
    //private static Clip[] effectClips (should be useful in the future, but not now);

    private int music;

    //Generic JWindow control the main game screen repainting process
    private Dance mainTargetWindow = null;
    private java.util.Timer fpsTimer = null;
    //exact timing of main dance background music clip start
    private long startTimeMicros;

    private static File effectbox[] = null;
    private static File mainmenubox[] = null;
    private static File musicbox[] = null;
    static{
        File soundFolder = new File("./sound/musicbox");
        Vector<String> soundFilePaths = new Vector<String>();
        if (soundFolder.exists() && soundFolder.isDirectory()) {
            File[] files = soundFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        soundFilePaths.add(file.getPath());
                    } else if (file.isDirectory()) {
                        System.out.println("Directory: " + file.getName());
                    }
                }
            } else {
                System.out.println("The sound folder is empty or cannot be read. There will be no music in the game!");
            }
        } else {
            throw new RuntimeException("The folder './sound' does not exist or is not a directory.");
        }

        musicbox = new File[soundFilePaths.size()];
        for (int i = 0; i < soundFilePaths.size(); i++) {
            musicbox[i] = new File(soundFilePaths.get(i));
            System.out.println("soundbox[" + i + "]=" + musicbox[i]);
        }
    }

    //mainmenu sound preloading
    static{
        File soundFolder = new File("./sound/mainmenubox");
        Vector<String> soundFilePaths = new Vector<String>();
        if (soundFolder.exists() && soundFolder.isDirectory()) {
            File[] files = soundFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        soundFilePaths.add(file.getPath());
                    } else if (file.isDirectory()) {
                        System.out.println("Directory: " + file.getName());
                    }
                }
            } else {
                System.out.println("The sound/mainmenu folder is empty or cannot be read. There will be no intro music!");
            }
        } else {
            //throw new RuntimeException("The folder './sound' does not exist or is not a directory.");
        }

        mainmenubox = new File[soundFilePaths.size()];
        for (int i = 0; i < soundFilePaths.size(); i++) {
            mainmenubox[i] = new File(soundFilePaths.get(i));
            System.out.println("mainmenubox[" + i + "]=" + mainmenubox[i]);
        }
    }

    static{
        File soundFolder = new File("./sound/effectbox");
        Vector<String> soundFilePaths = new Vector<String>();
        if (soundFolder.exists() && soundFolder.isDirectory()) {
            File[] files = soundFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        soundFilePaths.add(file.getPath());
                    } else if (file.isDirectory()) {
                        System.out.println("Directory: " + file.getName());
                    }
                }
            } else {
                System.out.println("The sound/mainmenu folder is empty or cannot be read. There will be no intro music!");
            }
        } else {
            //throw new RuntimeException("The folder './sound' does not exist or is not a directory.");
        }

        effectbox = new File[soundFilePaths.size()];
        for (int i = 0; i < soundFilePaths.size(); i++) {
            effectbox[i] = new File(soundFilePaths.get(i));
            System.out.println("mainmenubox[" + i + "]=" + effectbox[i]);
        }
    }

    //constructor
    public SoundController()
    {
    }

    public void playEffectSound(int condition)
    {
        // 啟動聲音播放
        if (effectClip != null) {
            effectClip.stop();
            //effectClip.close();
            effectClip=null;
        }

        double temp=(Math.random()*5);

        // 載入並開啟音效檔
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(effectbox[condition]);
            effectClip = AudioSystem.getClip();
            effectClip.open(audioIn); // 將音頻資料載入Clip
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 啟動聲音播放
        if (effectClip != null) {
            effectClip.start();
        }
    }

    public void playMainMenuSound(int shortmusic) {
        // 停止目前的主選單音效
        if (mainMenuClip != null) {
            mainMenuClip.stop();
            mainMenuClip.close();
            mainMenuClip = null;
        }

        // 載入並開啟音效檔
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(mainmenubox[shortmusic]);
            mainMenuClip = AudioSystem.getClip();
            mainMenuClip.open(audioIn); // 將音頻資料載入 Clip
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 啟動聲音播放
        if (mainMenuClip != null) {
            mainMenuClip.start();
        }
    }

    public boolean getStatus()
    {
        return this.status;
    }

    public void run(){

        //playBackgroundSound(0);

    }

    public void playBackgroundSound(int music, boolean isPreview )
    {
        // 啟動聲音播放
        if (danceClip != null) {
            danceClip.stop();
            danceClip.close();
            danceClip=null;
        }

        // 載入並開啟音效檔
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(musicbox[music]);
            danceClip = AudioSystem.getClip();
            danceClip.open(audioIn); // 將音頻資料載入Clip
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 啟動聲音播放
        if (danceClip != null) {
            danceClip.start();
            //record the exact timing of clip start
            if(!isPreview){
                System.out.println("entering is not preview : isPreview="+isPreview);
                setStartTimeMicros(System.nanoTime() / 1000);
                // Schedule task to run every 16 milliseconds after an initial 0 second delay
                fpsTimer = new java.util.Timer("FPSTimer");
                FPSTimerTask fpsTimerTask =  new FPSTimerTask(mainTargetWindow);
                fpsTimer.scheduleAtFixedRate(fpsTimerTask, 0, 16);
            }
        }
        this.status=true;
    }

    public void stop_all()
    {
        try{
            if(danceClip!=null) {danceClip.stop();danceClip.close();}
            if(effectClip!=null) {effectClip.stop(); effectClip.close();}
            if(mainMenuClip !=null) {mainMenuClip.stop(); mainMenuClip.close();}

        }catch(java.lang.NullPointerException e){
            e.printStackTrace();
        }
        finally{
            danceClip = null;
            effectClip = null;
            mainMenuClip = null;
            if(fpsTimer != null) {
                fpsTimer.cancel();
                fpsTimer = null;
            }
            startTimeMicros = 0; // Reset start time
        }
        return;

    }

    public void stop_mainmenu_sound()
    {
        try{
            //mainMenuClip.stop();
        }catch(java.lang.NullPointerException e){e.printStackTrace();}
        return;

    }

    public long getStartTimeMicros() {
        return startTimeMicros;
    }

    public void setStartTimeMicros(long startTimeMicros) {
        this.startTimeMicros = startTimeMicros;
    }

    public JWindow getMainTargetWindow() {
        return mainTargetWindow;
    }

    public void setMainTargetWindow(Dance mainTargetWindow) {
        this.mainTargetWindow = mainTargetWindow;
    }

    public Timer getFpsTimer() {
        return fpsTimer;
    }
}