package com.example.mmg.bt_racecar;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity{

    //constants for message issue identification
    public static int REQUEST_ENABLE_BT = 1;
    public static int MSG_CONNECTED = 10;
    public static int MSG_CONNECTION_FAILED = 11;
    public static int MSG_CONNECTION_ERROR = 12;

    BTListFragment BTList = null;
    BTConFragment BTCon = null;
    private SensorReader sensor_reader = null;

    private Handler handler = null;

    private MakeConnectionThread connect_thread = null;
    private ManageConnectionThread bt_communication_thread = null;

    private enum Display_mode{NOT_CONNECTED, CONNECTED}
    Display_mode mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //create Handler for communication with background Threads
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if(msg.what == MSG_CONNECTED){
                    Log.d("BTCON", "Connected notification received ");
                    manageBTConnection((BluetoothSocket)msg.obj);
                }
                else if(msg.what == MSG_CONNECTION_FAILED){
                    Log.d("BTCON", "Connection fail notification received ");
                    notifyConFail();
                }
                else if(msg.what == MSG_CONNECTION_ERROR){
                    Log.d("BTCON", "Connection error notification received ");
                    notifyConLost();
                }
            }
        };

        setContentView(R.layout.main_activity_layout); //set MainActivity's UI before adding a Fragment

        mode = Display_mode.NOT_CONNECTED;

        //UI Fragments
        BTCon = new BTConFragment();
        BTList = new BTListFragment();

        //initially display the Fragment containing devices list and Bluetooth scan button
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.main_lin_layout, BTList).commit();
    }

    private void notifyConFail() {
        Toast.makeText(this, "Connection failed!", Toast.LENGTH_SHORT).show();
    }
    private void notifyConLost() {
        Toast.makeText(this, "Connection to device was lost!", Toast.LENGTH_SHORT).show();
        killBTConnection(); //reset and go back to devices list UI
    }

    private void switchMode(){

        //switch UI Fragment between devices list(unconnected) and device control screen(connected)
        if(mode == Display_mode.NOT_CONNECTED){     //set connected UI

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.main_lin_layout, BTCon).commit();

            mode = Display_mode.CONNECTED;
        }
        else if(mode == Display_mode.CONNECTED){    //set unconnected UI
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.main_lin_layout, BTList).commit();

            mode = Display_mode.NOT_CONNECTED;
        }
    }

    public void initBTConnection(BluetoothDevice device){
        Toast.makeText(this, "Attempting connection...", Toast.LENGTH_SHORT).show();

        //start a background thread to handle making connection with the device
        connect_thread = new MakeConnectionThread(device, handler);
        connect_thread.start();
    }

    public void killBTConnection(){

        sensor_reader.cancel(); //stop sensor readings

        if(connect_thread == null)
            return;

        //close socket and reset member threads to null
        bt_communication_thread.cancel();
        bt_communication_thread = null;
        connect_thread.cancel();
        connect_thread = null;

        switchMode(); //show device list
    }

    private void manageBTConnection(BluetoothSocket socket){

        switchMode(); //show device control screen

        Toast.makeText(this, "Successfully connected to device!", Toast.LENGTH_LONG).show();

        //start a background thread to handle the Bluetooth data streams
        bt_communication_thread = new ManageConnectionThread(socket, handler);
        if(sensor_reader ==null){

            sensor_reader = new SensorReader(this);
        }

        sensor_reader.start(); //begin reading user's movements
    }

    public void sendBluetoothMessage(int speed_us, int tilt_us){
        byte[] msg = new byte[5];

        msg[0] = (byte)'+';                      //message start indicator
        msg[1] = (byte)((speed_us >> 8) & 0xFF); //speed value high byte
        msg[2] = (byte)(speed_us & 0xFF);        //speed value low byte


        msg[3] = (byte)((tilt_us >> 8) & 0xFF);  //rotation value high byte
        msg[4] = (byte)(tilt_us & 0xFF);         //rotation value low byte

        //send message
        if(bt_communication_thread != null) {

            bt_communication_thread.write(msg);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //disconnect and reset background thread members
        if(connect_thread != null){
            connect_thread.cancel();
            connect_thread = null;
            bt_communication_thread = null;
        }
    }

}
