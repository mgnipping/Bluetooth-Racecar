package com.example.mmg.bt_racecar;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Copied from source: https://developer.android.com/guide/topics/connectivity/bluetooth.html#ConnectingDevices
 * some modifications made
 */
public class ManageConnectionThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        private Handler uiHandler = null;

        public ManageConnectionThread(BluetoothSocket socket, Handler handler) {
            mmSocket = socket;
            uiHandler = handler;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e("BTConnection", "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("BTConnection", "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = uiHandler.obtainMessage(
                            3, numBytes, -1,
                            mmBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d("BTConnection", "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {

            try {
                mmOutStream.write(bytes);

            } catch (IOException e) {
                Log.d("BTCON", "Error occurred when sending data");

                Message m = new Message();

                    m.what = MainActivity.MSG_CONNECTION_ERROR;

                //notify MainActivity through it's Handler that the connection was broken
                uiHandler.sendMessage(m);

            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("BTConnection", "Could not close the connect socket", e);
            }
        }
    }


