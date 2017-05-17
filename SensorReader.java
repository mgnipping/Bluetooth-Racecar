package com.example.mmg.bt_racecar;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by mmg on 2017-04-24.
 */
public class SensorReader implements SensorEventListener {

    private static final double GRAVITATIONAL_ACCELERATION = 9.80665;
    private static final int MAX_DEVIATION_SPEED = 500;
    private static final int MAX_DEVIATION_ROTATION = 500;

    private Context context;
    //  private Handler mainhandler;
    private SensorManager sensormanager;
    private Sensor sensor = null;

    private int speed_us, tilt_us;

    public SensorReader(Context c){

        context = c;

        sensormanager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        sensor = sensormanager.getDefaultSensor(Sensor.TYPE_GRAVITY);

    }

    public void start(){

        sensormanager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
    }

    public void cancel(){

        sensormanager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        float valY = event.values[1];
        float valX = event.values[0];

        //convert (tilt/angle)*max_value into a microseconds value
        speed_us = 1500 + (int) ((valY/ GRAVITATIONAL_ACCELERATION )* MAX_DEVIATION_SPEED);
        tilt_us = 1500 + (int) ((valX/ (Math.sin(Math.toRadians(45.0)) * GRAVITATIONAL_ACCELERATION))* MAX_DEVIATION_ROTATION);

        ((MainActivity)context).sendBluetoothMessage(speed_us, tilt_us);
        Log.d("DATA", " "+speed_us); //for debugging
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //required implement method
    }
}