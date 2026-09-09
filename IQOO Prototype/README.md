# Project Chimera

Thermal-Aware Heterogeneous Offload Engine.

This prototype executes heavy SIMD vector math locally on an Android phone using ARM NEON when cool, and dynamically offloads the computation to a remote RISC-V Vector (RVV 1.0) runtime over a local TCP socket when device thermals spike.

## Prerequisites

1.  **Android Studio** (Koala or newer) with Android NDK and CMake installed via SDK Manager.
2.  **Host Machine (Laptop)** with Python 3, `qemu-riscv64`, and `gcc-riscv64-linux-gnu` cross-compiler.

## Build and Run Instructions

### Step 1: Set up the Host RISC-V Environment

On your laptop (Linux or WSL2):

1.  **Install dependencies:**
    ```bash
    sudo apt update
    sudo apt install -y qemu-user qemu-user-static gcc-riscv64-linux-gnu python3
    ```
2.  **Compile the RVV 1.0 Kernel:**
    ```bash
    cd host
    make
    ```
3.  **Start the TCP Server:**
    ```bash
    python3 server.py
    ```

### Step 2: Set up the Android Application

1.  Open **Android Studio**.
2.  Select **Open** and choose the `android` folder in this repository (`d:\IQOO Prototype\android`).
3.  Wait for Gradle sync to complete. If it asks to upgrade Gradle or the Android plugin, you can click "Accept" or use the suggested version.
4.  Ensure that your Android device is connected via USB and USB Debugging is enabled.

### Step 3: Connect the Devices

To ensure a low-latency, stable connection without relying on Wi-Fi routing, we use ADB Reverse Port Forwarding. This allows the Android app to talk to `127.0.0.1:8080`, which ADB forwards securely to `8080` on your laptop.

1.  Run the following command in your terminal:
    ```bash
    adb reverse tcp:8080 tcp:8080
    ```

*(Note: If you unplug your phone, you must re-run this command).*

### Step 4: Run the Prototype

1.  Click the **Run** button (Play icon) in Android Studio to deploy the `app` to your phone.
2.  Once opened, tap **"Trigger Heavy Vector Compute"** while the status is "LOCAL ARM NEON".
    - Watch the execution time (in ms).
3.  **Trigger Remote Offload:**
    - Either wait for the phone to heat up (play a heavy game in the background), OR
    - Toggle the **"Manual Override (Force Remote)"** switch in the UI.
4.  The UI badge will change to **"REMOTE RISC-V RVV 1.0"**.
5.  Tap **"Trigger Heavy Vector Compute"** again.
    - The payload will be serialized and sent over TCP.
    - Check the Host terminal running `server.py` to see QEMU RISC-V processing the vector workload.
    - Check the phone for the round-trip execution latency and math checksum validation.
