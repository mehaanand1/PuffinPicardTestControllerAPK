# Puffin / Picard Test Controller

An Android test controller application designed for Flock Safety hardware targets (**Puffin** and **Picard**).

---

## Features

* **Dynamic Hardware Auto-Detection:** 

---

## Quick Start: Building the APK
1. Open the project in **Android Studio**.
2. Press **`Shift` + `Shift`**.
3. Search **`Build APK`** and click on "Generate APK" as shown in the image below. <img width="667" height="25" alt="image" src="https://github.com/user-attachments/assets/a8ba1a87-7575-4cdc-9f81-286ae3c143fa" />
4. Locate the generated binary in:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   <img width="376" height="96" alt="image" src="https://github.com/user-attachments/assets/4cd7c2da-983c-406c-90b8-5fd716f726e2" />
5. Transfer this `.apk` file to the host/jumpbox machine connected to the target node you want to test.
6. Follow the **Sideloading & Installation** steps below to deploy and launch the app on the target node via ADB.
---

## Sideloading & Installation Instructions
Use these steps to transfer, sideload, and execute the application on your target board via ADB.
1. Verify ADB Connection - Ensure your target hardware is powered on and recognized by your host system:
  ```bash
  adb devices
2. Move APK to Workspace (If using ScreenConnect / Remote PC). If transferring the APK via ScreenConnect or remote desktop to your lab machine, copy it to your local workspace and update permissions:
  Bash
  sudo cp /root/ScreenConnect/Files/app-debug.apk ~/Downloads/app-debug.apk
  sudo chown $USER:$USER ~/Downloads/app-debug.apk
3. Uninstall Old Build & Sideload New APK. Remove the previous installation to clear cached state, then install the fresh binary:
  Bash
  adb -s <DEVICE_SERIAL> uninstall com.example.puffintestcontroller
  adb -s <DEVICE_SERIAL> install ~/Downloads/app-debug.apk
4. Launch Application with Dynamic Target Serial. Start the MainActivity on the node while passing the target serial number as an intent extra:
  Bash
  adb -s <DEVICE_SERIAL> shell am start \
    -n com.example.puffintestcontroller/.MainActivity \
    --es "SERIAL_NO" "<DEVICE_SERIAL>"
5. Launch Live Screen Mirroring (scrcpy). To view and interact with the target device's display remotely on your host machine:
  ```bash
  scrcpy -s <DEVICE_SERIAL>
