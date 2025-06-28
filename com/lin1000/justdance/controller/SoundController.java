package com.lin1000.justdance.controller;

import com.lin1000.justdance.beats.BeatMapGenerator;
import com.lin1000.justdance.gamepanel.Dance;

import java.io.File;
import java.util.Timer;
import java.util.Vector;
import javax.sound.sampled.*;
import javax.swing.*;

//=============================================================
//音樂控制中心，即時反應出目前的任何condition
//
//=============================================================
public class SoundController implements Runnable
{
    private boolean status=false;//代表目前中心是否已經把音樂準備好

    private Clip danceClip;
    private Clip effectClip;
    private Clip mainMenuClip;
    //private static Clip[] effectClips (should be useful in the future, but not now);

    private int music;

    //SourceDataLine parameters
    //frame rate refers to the number of audio frames processed per second, ypically measured in Hertz (Hz), indicating frames per second.
    private float frameRate;
    //frame size refers to the number of samples within each frame
    private int frameSize;
    private long frameLength;
    private AudioFormat.Encoding soundEcoding;
    private float sampleRate;
    private int sampleSizeInBits;
    private int audioChannelSize;
    private volatile long bufferedSample = 0;



    //Generic JWindow control the main game screen repainting process
    private Dance mainTargetWindow = null;
    private java.util.Timer fpsTimer = null;
    //exact timing of main dance background music clip start
    private long startTimeNanos;

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
            System.out.println("musicbox[" + i + "]=" + musicbox[i]);
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
            System.out.println("effectbox[" + i + "]=" + effectbox[i]);
        }
    }




    //constructor
    public SoundController()
    {
    }

    public void playEffectSound(int condition)
    {
        /* *
         * TODO:
         *  資源關閉與重播： 使用 Clip 時如果需要反覆播放，應避免頻繁建立和關閉音效資源。
         * 可以呼叫 clip.setFramePosition(0) 將播放位置重設到開頭，再次 start() 來重播音效，而不必每次重新開啟檔案。
         * 當遊戲結束或不再需要音效時，記得呼叫 clip.stop() 並關閉資源以釋放記憶體和音頻通道。
         * */
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
                setStartTimeNanos(System.nanoTime());
                // Schedule task to run every 16 milliseconds after an initial 0 second delay
                fpsTimer = new java.util.Timer("FPSTimer");
                FPSTimerTask fpsTimerTask =  new FPSTimerTask(mainTargetWindow);
                fpsTimer.scheduleAtFixedRate(fpsTimerTask, 0, 16);
            }
        }
        this.status=true;
    }

    public void initiateAudioDrivenMainTheadGameLoop(int music)
    {
        // 啟動聲音播放
        if (danceClip != null) {
            danceClip.stop();
            danceClip.close();
            danceClip=null;
        }

        // initiate a new thread to play the audio driven game loop engine
        try {
            this.music = music;//must set before starting the thread

            /**
             * Audio BPM Analysis by FFT
             */
            BeatMapGenerator beatMapGenerator = new BeatMapGenerator();
            beatMapGenerator.generate(musicbox[music]);


            /**
             * handover major game loop engine to Audio Driven Thead
             */
            new Thread(this).start();
        } catch (Exception e) {
            e.printStackTrace();
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
            startTimeNanos = 0; // Reset start time
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

    public long getStartTimeNanos() {
        return startTimeNanos;
    }

    public void setStartTimeNanos(long startTimeNanos) {
        this.startTimeNanos = startTimeNanos;
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

    public static File[] getMusicbox() {
        return musicbox;
    }

    @Override
    public void run() {

        // 載入並開啟音效檔 Steaming Mode
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(musicbox[music]);
            AudioFormat format = audioIn.getFormat();
            frameLength = audioIn.getFrameLength();
            frameRate = format.getFrameRate();
            frameSize = format.getFrameSize();
            soundEcoding = format.getEncoding();
            sampleRate = format.getSampleRate();
            sampleSizeInBits = format.getSampleSizeInBits();
            audioChannelSize = format.getChannels();

            System.out.println("musicbox[music]: " + musicbox[music].getName());
            System.out.println("frameLength: " + frameLength);
            System.out.println("format.getFrameRate(): " + frameRate);
            System.out.println("format.getFrameSize(): " + frameSize);
            System.out.println("format.getEncoding(): " + soundEcoding.toString());
            System.out.println("format.getSampleSizeInBits(): " + sampleSizeInBits);
            System.out.println("format.getChannels(): " + audioChannelSize);

            //if(!isPreview){
            System.out.println("entering game play");
            setStartTimeNanos(System.nanoTime());
            // Schedule task to run every 16 milliseconds after an initial 0 second delay
            fpsTimer = new java.util.Timer("FPSTimer");
            FPSTimerTask fpsTimerTask =  new FPSTimerTask(mainTargetWindow);
            fpsTimer.scheduleAtFixedRate(fpsTimerTask, 0, 16);
            //}

            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            /**
             * 執行緒協調： 若聲音播放和畫面更新在不同執行緒，注意執行緒間的協作。例如在主執行緒啟動音效執行緒後，可以使用同步機制等待音效真正開始再進行遊戲計時。
             * 此外，不要在音效回呼（如 LineListener）中執行太耗時的操作，這可能阻塞音效執行緒導致聲音延遲。
             * line.addLineListener(new SourceDataLineEventListener());
             */
            line.open(format);
            line.start();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = audioIn.read(buffer)) != -1) {
                line.write(buffer, 0, bytesRead);
                bufferedSample += bytesRead / frameSize;
                //System.out.println("bufferedSample="+ bufferedSample);
                /* *
                 * magic done by 1000 (ms/per second) / FrameRate(e.g. 16000, a.k.a 16Hz/per second) * sending-frame-byte-array(4096) / framesize(e.g. 4)
                 * result is number of milliseconds can take rest while sending enough (4096 bytes) to SourceDataLine Buffer Area.
                 * i.e. if the program sleep longer than the result (ms), the buffer will be cleared out by targetDataLine processor.
                 * format.getFrameRate(): 16000.0
                 * format.getFrameSize(): 4
                 * format.getEncoding(): PCM_SIGNED
                 * format.getSampleSizeInBits(): 16
                 * the result should be 64
                 * if the frameate is increase to 32000, the result will be 32
                 * */
                Thread.sleep(32);
                if(mainTargetWindow.conditionControl.getGameOver()){
                    break;
                }
            }

            line.drain();
            line.close();
            audioIn.close();


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}