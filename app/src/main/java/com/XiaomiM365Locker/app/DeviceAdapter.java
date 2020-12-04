package com.XiaomiM365Locker.app;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.polidea.rxandroidble2.RxBleConnection;

import java.util.List;

public class DeviceAdapter extends ArrayAdapter<Device> {
    private static final String PREFIX_RSSI = "RSSI:";
    private final List<Device> mList;
    private final LayoutInflater mInflater;
    private final int mResId;
    private final Context context;

    public DeviceAdapter(Context context, int resId, List<Device> objects) {
        super(context, resId, objects);
        mResId = resId;
        mList = objects;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.context = context;
    }

    @NonNull
    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Device item = getItem(position);
        TextView name = null;
        if (convertView != null) {
            name = convertView.findViewById(R.id.device_name);
        }

        if (convertView == null) {
            convertView = mInflater.inflate(mResId, null);

        }


        if (item != null) {
            if (item.isInitName()) {
                item.setDisplayName(item.getDisplayName());
            } else {
                item.setDisplayName(item.getOtherArgument() + item.getOriginalName());
            }
        }

        if (item != null) {
            if (name != null) {
                name.setText(item.getDisplayName());
            }
        }
        TextView address = convertView.findViewById(R.id.device_address);
        if (item != null) {
            address.setText(item.getDevice().getAddress());
        }
        TextView rssi = convertView.findViewById(R.id.device_rssi);
        if (item != null) {
            rssi.setText(PREFIX_RSSI + item.getRssi());
        }

        TextView state = convertView.findViewById(R.id.device_state);
        if (item != null) {
            state.setText(item.getState().name());
        }



            TextInputEditText excludedMacs = ((MainActivity) context).findViewById(R.id.macToExclude);

        View finalConvertView = convertView;
        convertView.setOnClickListener(OnClickListener -> {
            if (excludedMacs != null) {
                if (excludedMacs.getText().toString().contains(address.getText())) {

                    Toast.makeText(getContext(), "Deleted " + address.getText(), Toast.LENGTH_LONG).show();
                    excludedMacs.setText(excludedMacs.getText().toString().replace(address.getText()+"/",""));
                    finalConvertView.setBackgroundColor(Color.WHITE);

                }else{
                    Toast.makeText(getContext(), "Added " + address.getText(), Toast.LENGTH_LONG).show();
                    excludedMacs.setText(excludedMacs.getText().toString()+address.getText()+"/");
                    finalConvertView.setBackgroundColor(Color.GREEN);


                }
            }

        });

        if (excludedMacs != null) {
            if (excludedMacs.getText().toString().contains(address.getText())) {

                finalConvertView.setBackgroundColor(Color.GREEN);


            }
        }


        return convertView;
    }

    public Device getDeviceByAddress(String address) {
        for (Device device : mList) {
            if (address.equals(device.getDevice().getAddress())) {
                return device;
            }
        }
        return null;
    }

    public void updateDeviceConnection(String address, RxBleConnection.RxBleConnectionState state) {
        Device device = getDeviceByAddress(address);
        if (device != null) {
            device.setState(state);
            notifyDataSetChanged();
        }
    }

    /**
     * add or update BluetoothDevice
     */
    public void update(BluetoothDevice newDevice, int rssi, RxBleConnection.RxBleConnectionState state) {
        if ((newDevice == null) || (newDevice.getAddress() == null)) {
            return;
        }

        Device device = getDeviceByAddress(newDevice.getAddress());
        if (device == null) {
            mList.add(new Device(newDevice, rssi));

        } else {
            device.setRssi(rssi);
            device.setState(state);
        }

        notifyDataSetChanged();
    }

}
