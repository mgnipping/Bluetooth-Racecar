package com.example.mmg.bt_racecar;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

public class BTListFragment extends ListFragment implements ListView.OnItemClickListener, Button.OnClickListener{

    Context context_activity = null;

    BluetoothAdapter BTadapter = null;
    BTDevicesAdapter adapter = null;

    View view = null;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        context_activity = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_connect, container, false);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getContext().unregisterReceiver(mReceiver);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ListView list = (ListView)view.findViewById(android.R.id.list);
        list.setOnItemClickListener(this);

        Button btn_scan = (Button)view.findViewById(R.id.btn_scan);
        btn_scan.setOnClickListener(this);

        BTadapter = BluetoothAdapter.getDefaultAdapter();

        //ask user to enable Bluetooth for device discovery
        if (!BTadapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, MainActivity.REQUEST_ENABLE_BT);
        }
        else{
            //Bluetooth already enabled, set adapter
            adapter = new BTDevicesAdapter(context_activity, android.R.layout.simple_list_item_1);
            setListAdapter(adapter);

        }

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getContext().registerReceiver(mReceiver, filter);
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    //BroadcastReceiver subclass copied from source: https://developer.android.com/guide/topics/connectivity/bluetooth.html#ConnectingDevices
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d("BTCON", "Broadcastreceiver received Intent");
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("BTCON", "Adding "+ device.getName()+ " to list");
                adapter.add(device);
            }
        }
    };


    @Override
    public void onClick(View v) {

        //start discovery
        BTadapter.startDiscovery();

        if(!BTadapter.isDiscovering()) {//if failed to start discovery

           checkBTPermissions();
            BTadapter.startDiscovery();
            if(!BTadapter.isDiscovering()) {
                Log.d("BTCON", "Discovery started");
            }
        }
        else{
            Log.d("BTCON", "Discovery started (no-permission)");
        }
    }

    //this method checks permissions needed to receive Intents for discovered BluetoothDevices
    //the method was copied in full from external source:
    /*https://github.com/mitchtabian/Bluetooth---Discover-Devices/blob/master/Bluetooth-DiscoverDevices/app/src/main/java/com/example/user/bluetooth_discoverdevices/MainActivity.java*/
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            Log.d("BTCON", "SDK version > Lollipop)");
            int permissionCheck = getContext().checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += getContext().checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                Log.d("BTCON", "Needs new permissions");
                getActivity().requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d("BTCON", "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //device clicked in ListView

        //stop scanning for devices
       if(BTadapter.isDiscovering()){
           BTadapter.cancelDiscovery();
       }

        //get the device and pass to MainActivity for starting connection
        BluetoothDevice device = adapter.getItem(position);
        ((MainActivity)context_activity).initBTConnection(device);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        adapter = new BTDevicesAdapter(context_activity, android.R.layout.simple_list_item_1);
        setListAdapter(adapter);
    }
}
