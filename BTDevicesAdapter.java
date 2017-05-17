package com.example.mmg.bt_racecar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

public class BTDevicesAdapter extends ArrayAdapter<BluetoothDevice> {

    BluetoothAdapter bluetoothAdapter = null;
    ArrayList<BluetoothDevice> bluetoothDevices;


    public BTDevicesAdapter(Context context, int resource){

        super(context, resource);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothDevices = new ArrayList<BluetoothDevice>();

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                bluetoothDevices.add(device);
            }
        }

    }

    @Override
    public void add(BluetoothDevice device) {

        //check that device is not a duplicate
        for (BluetoothDevice d: bluetoothDevices
             ) {
            if(d.getAddress().equals(device.getAddress())){
                Log.d("BTCON", "Device not added - already in list");
                return;
            }

        }

        //add device and update UI
        bluetoothDevices.add(device);
        this.notifyDataSetChanged();
    }

    @Override
    public BluetoothDevice getItem(int position) {
        return bluetoothDevices.get(position);
    }

    @Override
    public int getCount() {
        return bluetoothDevices.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View listitem;

        if(convertView != null){
            listitem = convertView;
        }
        else{

            listitem = inflater.inflate(R.layout.bt_list_element, parent, false);
        }

        //fetch UI components in list element View
        TextView name = (TextView)listitem.findViewById(R.id.text_BTname);
        TextView info = (TextView)listitem.findViewById(R.id.text_BTinfo);

        //set device data to View
        BluetoothDevice btd = bluetoothDevices.get(position);
        name.setText(btd.getName());
        info.setText(btd.getAddress());

        return listitem;
    }
}
