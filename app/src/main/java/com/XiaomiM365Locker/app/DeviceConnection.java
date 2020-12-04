package com.XiaomiM365Locker.app;

import android.annotation.SuppressLint;
import android.util.Log;

import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.reactivex.disposables.Disposable;

import android.app.Activity;

public class DeviceConnection {
    private final RxBleDevice device;
    private RxBleConnection connection;
    private Disposable connection_disposable;
    private boolean written_success = false;
    private final ConcurrentLinkedQueue<IRequest> command_to_execute;
    private final DeviceAdapter deviceAdapter;
    private final Activity activity;

    DeviceConnection(RxBleDevice device, DeviceAdapter deviceAdapter, Activity activity) {
        this.device = device;
        this.command_to_execute = new ConcurrentLinkedQueue<>();

        this.activity = activity;
        this.deviceAdapter = deviceAdapter;

        this.setupConnection();
    }

    public void addCommand(IRequest request) {
        this.command_to_execute.add(request);
    }

    public IRequest get_first_command() {
        return this.command_to_execute.peek();
    }

    public void runNextCommand() {
        try {

            if (this.device.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED && this.connection != null) {

                IRequest command = this.command_to_execute.remove();
                if (command != null) {
                    this.sendCommand(command);
                }
            }
        }catch (Exception ignored){

        }
    }


    public RxBleConnection.RxBleConnectionState getState() {
        return this.device.getConnectionState();
    }

    @SuppressLint("CheckResult")
    public void setupConnection() {
        this.connection_disposable = this.device.establishConnection(false).doFinally(this::dispose)
                .subscribe(this::onConnectReceived, this::onConnectionFailure);

        this.device.observeConnectionStateChanges().subscribe(this::onObserveState,
                this::onObserveStateFailure);
    }

    private void onObserveState(RxBleConnection.RxBleConnectionState newState) {
        this.updateDeviceStatus();
    }

    private void onObserveStateFailure(Throwable e) {
        Log.e(Constants.TAG, "ObserveFailed", e);
        this.connection = null;
    }

    private void onConnectReceived(RxBleConnection connection) {
        this.connection = connection;
    }

    private void updateDeviceStatus() {
        String address = this.device.getMacAddress();
        RxBleConnection.RxBleConnectionState state = this.device.getConnectionState();

        Log.d(Constants.TAG, address + " : " + state);

        activity.runOnUiThread(() -> {
            deviceAdapter.updateDeviceConnection(address, state);
        });
    }

    private void onConnectionFailure(Throwable e) {
        Log.e(Constants.TAG, "Connection failure", e);
        this.connection = null;
    }

    public void dispose() {

        if (this.connection_disposable != null) {
            this.connection_disposable.dispose();
            this.connection_disposable = null;
        }
    }

    private void onWriteSuccess() {
        this.written_success = true;
    }

    private void onWriteFailure(Throwable throwable) {
        Log.e(Constants.TAG, "Error writing", throwable);
    }

    @SuppressLint("CheckResult")
    private void sendCommand(IRequest request) {
        try {
            if (device.getConnectionState() != RxBleConnection.RxBleConnectionState.CONNECTED) {
                return;
            }

            String command = request.getRequestString();

            this.written_success = false;


            this.connection.writeCharacteristic(UUID.fromString(Constants.CHAR_WRITE), HexString.hexToBytes(command)).subscribe((byte[] bytes) -> this.onWriteSuccess(), this::onWriteFailure);
        } catch (Exception ignored) {
        }

    }

    public void deleteNextCommand() {

        this.command_to_execute.remove();

    }
}
