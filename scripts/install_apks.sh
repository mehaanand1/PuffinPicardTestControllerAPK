#!/bin/bash
# Author: Chase Johnson

# Target directory containing APKs (defaults to current directory if not provided)
APK_DIR="${1:-.}"

# Verify the directory exists
if [ ! -d "$APK_DIR" ]; then
    echo "Error: Directory '$APK_DIR' not found."
    exit 1
fi

# Check if ADB is installed
if ! command -v adb &> /dev/null; then
    echo "Error: adb is not installed or not in your PATH."
    exit 1
fi

# Gather all APKs in the directory into an array (handles spaces in filenames safely)
shopt -s nullglob
apks=("$APK_DIR"/*.apk)
if [ ${#apks[@]} -eq 0 ]; then
    echo "Error: No .apk files found in '$APK_DIR'."
    exit 1
fi

echo "Found ${#apks[@]} APK(s) ready for installation."

# Main loop
while true; do
    echo "----------------------------------------"
    echo "Scanning for connected devices..."
    
    # Get a list of device serial numbers, ignoring "List of devices attached" and offline/unauthorized devices
    devices=$(adb devices | grep -w "device" | grep -v "List" | awk '{print $1}')

    if [ -z "$devices" ]; then
        echo "No authorized devices found."
    else
        for device in $devices; do
            echo ">>> Starting installation for device: $device"
            
            for apk in "${apks[@]}"; do
                filename=$(basename "$apk")
                echo "    Installing $filename..."
                # Use -r to replace/reinstall if the app already exists
                adb -s "$device" install -r "$apk"
            done
            
            echo ">>> Finished installing on $device."
        done
    fi

    echo "----------------------------------------"
    read -p "Would you like to connect more devices and install again? (y/n): " choice
    
    case "$choice" in
        y|Y )
            echo "Waiting for new devices. (Make sure to authorize USB debugging on the device screen if prompted)..."
            # Brief pause to let adb daemon register newly plugged-in devices
            sleep 3 
            ;;
        n|N )
            echo "Exiting script."
            break
            ;;
        * )
            echo "Invalid input. Exiting script."
            break
            ;;
    esac
done