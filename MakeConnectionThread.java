package com.example.mmg.bt_racecar;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import java.util.UUID;

/**
 * Copied from source: https://developer.android.com/guide/topics/connectivity/bluetooth.html#ConnectingDevices
 * some modifications made
 */

public class MakeConnectionThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private Handler uiHandler = null;
        private String default_uuid = "00001101-0000-1000-8000-00805F9B34FB";

        public MakeConnectionThread(BluetoothDevice device, Handler handler) {
            // Use a temporary object that is later assigned to mmSocket because mmSocket is final.
            BluetoothSocket tmp = null;
            uiHandler = handler;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(default_uuid));

            } catch (IOException e) {
                Log.d("BTCON", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();

            } catch (IOException exception) {
                // Unable to connect; close the socket and return.
                Log.d("BTCON", "Could not connect to the client socket");
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.d("BTCON", "Could not close the client socket", closeException);
                }
            }

            Message m = new Message();

            if(mmSocket.isConnected()){
                m.what = MainActivity.MSG_CONNECTED;
                m.obj = mmSocket;
            }
            else{
                m.what = MainActivity.MSG_CONNECTION_FAILED;
            }

            //notify MainActivity through it's Handler whether the connection succeeded
            uiHandler.sendMessage(m);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("MakeConnection", "Could not close the client socket", e);
            }
        }
    }