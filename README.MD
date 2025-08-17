# Just Dance (Rhythm Game)
Fundamental Game Design
It's a music game developed by Java. Features include:
1. Music selection
2. XBox Gamepad controller support
3. Keyboard controller support
4. Game scoring system
5. Game special effects (lite)
6. Audio BPM detection (experimental)
7. Audio synchronization (to do)
8. Hot plug for XBox Gamepad controller support
9. MIDI Device Support - Piano Keyboard Component 88 Keys
10. MP4 frame capture (jcodec)
11.Auto Beats Generation (to do)
12. [HID(Human Interface Devices) Usage Tables 1.6](https://usb.org/document-library/hid-usage-tables-16) as of 2025/01/03 
13. 3D Room Support (with walks).

## Screenshots
2025, July
![image](https://github.com/lin1000/Just-Dance/blob/main/readme/screen-start-02.png?raw=true)
![image](https://github.com/lin1000/Just-Dance/blob/main/readme/screen-musicbox-02.png?raw=true)
![image](https://github.com/lin1000/Just-Dance/blob/main/readme/screen-gameplay-02.png?raw=true)
![image](https://github.com/lin1000/Just-Dance/blob/main/readme/xbox-gamepad.jpg?raw=true)

2025, June
![image](https://github.com/lin1000/Just-Dance/blob/main/readme/screen-start.png?raw=true)
![image](https://github.com/lin1000/Just-Dance/blob/main/readme/screen-musicbox.png?raw=true)
![image](https://github.com/lin1000/Just-Dance/blob/main/readme/screen-gameplay-1.png?raw=true)
![image](https://github.com/lin1000/Just-Dance/blob/main/readme/xbox-gamepad.jpg?raw=true)

# Note

## HID(Human Interface Devices) 開啟HID Device後為什麼 HID 讀不到
Xbox Controller 原生採 XInput 路線：主要按鍵/軸的資料不走標準 HID Input Report，所以 hid4java.read() 只會一直逾時（回 0）。
有些第三方手把可切 DInput/HID 模式（長按某組鍵開機切換），那種切到 DInput 才能用 HID 讀得到；原廠 Xbox 手把沒有這個切換。