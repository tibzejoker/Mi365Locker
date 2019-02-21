package com.XiaomiM365Locker.app;

import android.bluetooth.BluetoothDevice;

import com.polidea.rxandroidble2.RxBleConnection;

public class Device {
    private static final String UNKNOWN = "Unknown";
    /**
     * BluetoothDevice
     */
    private BluetoothDevice mDevice;
    /**
     * RSSI
     */
    private int mRssi;
    /**
     * Display Name
     */
    private String otherArgument = "";
    private String mDisplayName;

    public boolean isInitName() {
        return initName;
    }

    boolean initName = true;

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    private String originalName;
    private RxBleConnection.RxBleConnectionState state;

    public Device(BluetoothDevice device, int rssi) {
        if (device == null) {
            throw new IllegalArgumentException("BluetoothDevice is null");
        }
        mDevice = device;
        mDisplayName = device.getName();
        if ((mDisplayName == null) || (mDisplayName.length() == 0)) {
            mDisplayName = UNKNOWN;
        }
        mRssi = rssi;
        this.state = RxBleConnection.RxBleConnectionState.DISCONNECTED;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public int getRssi() {
        return mRssi;
    }



    public void setRssi(int rssi) {
        mRssi = rssi;
    }

    public void setState(RxBleConnection.RxBleConnectionState newstate)
    {
        this.state = newstate;
    }

    public RxBleConnection.RxBleConnectionState getState() {
        return this.state;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
        if (initName){
            originalName = displayName;
            initName = false;
        }
    }

    public String getOtherArgument() {
        return otherArgument;
    }

    public void setOtherArgument(String stringRecue) {
        otherArgument = stringRecue;
    }

}
