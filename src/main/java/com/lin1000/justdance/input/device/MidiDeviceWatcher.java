package com.lin1000.justdance.input.device;

import com.lin1000.justdance.gamepanel.MainMenu;
import com.lin1000.justdance.gamepanel.inputdevice.MainMenuMidiDeviceListener;
import com.lin1000.justdance.gamepanel.inputdevice.MainMenuXInputDeviceListener;
import com.lin1000.justdance.player.JXInputPlayer;
import com.lin1000.justdance.player.MidiPlayer;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Transmitter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MidiDeviceWatcher {

    private MainMenu mainTargetWindow = null;
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private HashMap<String, MidiPlayer> midiDeviceHash = null;
    private HashMap<String, Boolean> midiDeviceHeartbeatHash = null;

    public interface MidiDeviceListener {
        void onDeviceDiscovered(MidiDevice device);
        void onDeviceConnected(MidiDevice device);
        void onDeviceDisconnected(MidiDevice device);
    }

    private MidiDeviceListener listener;

    public MidiDeviceWatcher(){
        this.listener = new MidiDeviceWatcher.MidiDeviceListener() {

            //🎹
            @Override
            public void onDeviceDiscovered(MidiDevice device) {
                System.out.println("\uD83C\uDFB9, Midi Device ["+ device+ "] is Discovered.");
            }

            @Override
            public void onDeviceConnected(MidiDevice device) {
                System.out.println("\uD83C\uDFB9 Midi Device ["+ device+ "] is Connected");
            }

            @Override
            public void onDeviceDisconnected(MidiDevice device) {
                System.out.println("\uD83C\uDFB9 Midi Device ["+ device + "] is Disconnected");
            }
        };
    }

    public MidiDeviceWatcher(MidiDeviceListener listener) {
        this.listener = listener;
    }

    public MainMenu getMainTargetWindow() {
        return mainTargetWindow;
    }

    public void setMainTargetWindow(MainMenu mainTargetWindow) {
        this.mainTargetWindow = mainTargetWindow;
    }

    private MidiPlayer extractDeviceIntoPlayer(MidiDevice device){
        MidiPlayer p = new MidiPlayer();
        p.setName("Player Midi");
        p.setPlayerNum(0);
        p.setAge((int)(Math.random()*18));
        p.setConnected(device.isOpen());
        p.setControllerID(device.toString());
        return p;
    }

    public void start() {

        if (MidiSystem.getMidiDeviceInfo()==null) {
            System.err.println("MidiDevice is not available in MidiDeviceWatcher.");
            return;
        }

        /***
         * Structure of MidiDevice is
         * MidiDevice.Info [] = a list of MidiDevices
         * MidiDevice.Info = a specific of Midi Device
         * MidiDevice = MidiSystem.getMidiDevice(info)
         */
        //Scan and register MidiDevice first
        MidiDevice.Info[] infos = null;
        MidiDevice.Info info =null;
        MidiDevice device = null;
        try {
            infos = MidiSystem.getMidiDeviceInfo();
            int count = infos != null ? infos.length : 0;
            midiDeviceHash = new HashMap<>();
            midiDeviceHeartbeatHash = new HashMap<>();

            // Midi Device Watcher logic.
            // iterator through the list of infos
            // and get the "device" from Info
            // and check whether the device is opened already
            // if opened, then count as connected device
            // if not opened, then count as disconnected device
            // either open or not,it will put into midiDeviceHash for identifying the player of Midi Device
            // the key is device.getClass().toString() hashcode.
            int connectedCount = 0;
            for (int i = 0; i < infos.length; i++) {
                info = infos[i];
                device = MidiSystem.getMidiDevice(info);
                if (device.isOpen()) {
                    connectedCount++;
                }
                MidiPlayer p = extractDeviceIntoPlayer(device);
                midiDeviceHash.put(device.getClass().toString(),p);
                midiDeviceHeartbeatHash.put(device.getClass().toString(), Boolean.TRUE);
            }

            //then schedule the pooling at Fixed Rate
            scheduler.scheduleAtFixedRate(this::scanDevices, 0, 2, TimeUnit.SECONDS);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanDevices() {

        MidiDevice.Info[] infos = null;
        MidiDevice device = null;
        boolean isMidiDeviceConnected = false;
        try {
            infos = MidiSystem.getMidiDeviceInfo();
            for (MidiDevice.Info info : infos) {
                device = MidiSystem.getMidiDevice(info);
                System.out.println("device.getDeviceInfo().getName()=" + device.getDeviceInfo().getName() +
                        ", device.getDeviceInfo().getVendor()=" + device.getDeviceInfo().getVendor() +
                        ", device.getDeviceInfo().getVersion()=" + device.getDeviceInfo().getVersion() +
                        ", device.getDeviceInfo().getMaxTransmitters()=" + device.getMaxTransmitters() +
                        ", device.getDeviceInfo().getMaxReceivers()=" + device.getMaxReceivers() +
                        ", device.toString()=" + device.toString() +
                        ", device.isOpen()=" + device.isOpen() +
                        ", device.getClass()=" + device.getClass() +
                        ", info.getDescription()=" + info.getDescription() +
                        ", device.getMicrosecondPosition()=" + device.getMicrosecondPosition()
                );

                MidiPlayer p = midiDeviceHash.get(device.getClass().toString());
                midiDeviceHeartbeatHash.put(device.getClass().toString(), Boolean.TRUE);

                /**
                 * IF p==null, then new device is discovered (additional condition && info.getName().equals("USB-MIDI") && device.getMaxTransmitters() !=0 && device.toString().contains("MidiInDevice"))
                 * IF p!=null and p.isConnected() == true and device.isOpen() == false, then device goes offline
                 * IF p!=null and p.isConnected() == true and device.isOpen() == true, then device is connected without change
                 * IF p!=null and p.isConnected() == false and device.isOpen() == false, then device is disconnected without change
                 * IF p!=null and p.isConnected() == false and device.isOpen() == true, then device comes online again
                 */
                if(p==null && info.getName().equals("USB-MIDI") && device.getMaxTransmitters() !=0 && device.toString().contains("MidiInDevice")){//New Device Join
                    p = extractDeviceIntoPlayer(device);
                    midiDeviceHash.put(device.getClass().toString(),p);
                    listener.onDeviceDiscovered(device);
                    if (info.getName().equals("USB-MIDI") && device.getMaxTransmitters() !=0 && device.toString().contains("MidiInDevice")){
                        System.out.println("New MidiDevice Join="+device);
                        device.open();
                        System.out.println("Connected to Midi Device：" + info.getName());
                        isMidiDeviceConnected = true;
                    }
                }else if( p!=null && p.isConnected() && !device.isOpen()){//Device become offline
                    p.setConnected(false); //which is false
                    MainMenuMidiDeviceListener mainMenuMidiDeviceListener = p.getMainMenuMidiDeviceListener();//take out the listener (for backup only)
                    p.setMainMenuMidiDeviceListener(null);
                    listener.onDeviceDisconnected(device);
                    System.out.println("MidiDevice become offline ="+device);
                } else if(p!=null && p.isConnected() &&  device.isOpen()){
                    //device is connected without change
                    //System.out.println("MidiDevice keep online ="+device);
                    isMidiDeviceConnected=true;
                    System.out.println("MidiDevice keep online ="+device);
                }else if(p!=null && !p.isConnected() && !device.isOpen()) {
                    //nothing changed
                    System.out.println("MidiDevice keep offline ="+device);
                }else if(p!=null && !p.isConnected() && device.isOpen()){//Device come online again
                    p.setConnected(device.isOpen());//which is true
                    MainMenuMidiDeviceListener mainMenuMidiDeviceListener = new MainMenuMidiDeviceListener(mainTargetWindow);
                    p.setMainMenuMidiDeviceListener(mainMenuMidiDeviceListener);
                    //add listener to enable game interactive by MidiDevice
                    Transmitter transmitter = device.getTransmitter();
                    transmitter.setReceiver(mainMenuMidiDeviceListener);
                    listener.onDeviceConnected(device);
                    isMidiDeviceConnected=true;
                    System.out.println("MidiDevice Device ["+ device.getClass().toString() +"] come online");
                }
            }

//            if(!isMidiDeviceConnected){
//                System.err.println("Cannot find KAWAI CN201 Piano MIDI Device.");
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ;
//        //System.out.println("Scanning Device...");
//        if (!XInputDevice.isAvailable()) {
//            System.out.println("XInput 不可用，請確認系統支援並已載入 DLL。");
//            return;
//        }
//        //System.out.println("Scanning Device...XInputDevice Driver is available..scanning.");
//
//        // 取得玩家 1 的控制器（0~3 對應 4 個可能控制器）
//        XInputDevice[] devices = null;
//        XInputDevice device = null;
//        //JXInputDevice
//        try {
//            devices = XInputDevice.getAllDevices();
//            int count = devices!=null?devices.length:0;
//            int connectedCount = 0;
//            for(int i=0; i < devices.length ;i++){
//                device = devices[i];
//                device.poll(); //Critical Step to update the controller state include isConnected.
//                Player p = xInputDeviceHash.get(device.toString());
//                if(p==null){//New Device Join
//                    p = extractDeviceIntoPlayer(device);
//                    xInputDeviceHash.put(device.toString(),p);
//                    listener.onDeviceDiscovered(device);
//                    System.out.println("New JXInputDevice Join="+device);
//                }else if(p!=null && p.isConnected() && !device.isConnected()){//Device become offline
//                    p.setConnected(device.isConnected());
//                    MainMenuXInputDeviceListener mainMenuXInputDeviceListener = p.getMainMenuXInputDeviceListener();
//                    device.removeListener(mainMenuXInputDeviceListener);
//                    p.setMainMenuXInputDeviceListener(null);
//                    listener.onDeviceDisconnected(device);
//                } else if(p!=null && p.isConnected() &&  device.isConnected()){
//                    //nothing changed
//                    System.out.println("JXInputDevice keep online ="+device);
//                }else if(p!=null && !p.isConnected() && !device.isConnected()) {
//                    //nothing changed
//                    System.out.println("JXInputDevice keep offline ="+device);
//                }else if(p!=null && !p.isConnected() && device.isConnected()){//Device come online again
//                    System.out.println("JXInput Device ["+device+"] come online");
//                    p.setConnected(device.isConnected());
//                    MainMenuXInputDeviceListener mainMenuXInputDeviceListener = new MainMenuXInputDeviceListener(mainTargetWindow);
//                    p.setMainMenuXInputDeviceListener(mainMenuXInputDeviceListener);
//                    // The SimpleXInputDeviceListener allows us to implement only the methods we actually need
//                    //add listener to enable game interactive by JXInputDevice
//                    device.addListener(mainMenuXInputDeviceListener);
//                    listener.onDeviceConnected(device);
//                }
//
//            }
//            //print out scanning and device is connected.
//            //System.out.println("Scanning Device...XInputDevice Driver is available..scanning..."+xInputDeviceHash.size()+" devices.." );
//            //xInputDeviceHash.values().stream().forEach(player->{ System.out.println(player.getControllerID() + "=" + player.isConnected());});
//
//        } catch (XInputNotLoadedException e) {
//            e.printStackTrace();
//            throw new RuntimeException(e);
//        }
    }

    public HashMap<String, MidiPlayer> getMidiDeviceHash() {
        return midiDeviceHash;
    }

    public MidiPlayer getMidiPlayer (int playerNum){
        Stream<Map.Entry<String, MidiPlayer>> entryStream = midiDeviceHash.entrySet().stream();
        //Stream<Player> valueStream = xInputDeviceHash.values().stream();
        Stream<Map.Entry<String, MidiPlayer>> playerStream = entryStream.filter(entry -> entry.getValue().getPlayerNum() == playerNum);
        List<Map.Entry<String, MidiPlayer>> playerCollection = playerStream.collect(Collectors.toList());
        return playerCollection.get(0).getValue().isConnected()?playerCollection.get(0).getValue():null;
    }

    public void stop() {
        scheduler.shutdownNow();
    }

}
