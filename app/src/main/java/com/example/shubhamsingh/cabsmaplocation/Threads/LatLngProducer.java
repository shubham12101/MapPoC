package com.example.shubhamsingh.cabsmaplocation.Threads;

import android.util.Log;

import com.example.shubhamsingh.cabsmaplocation.Objects.SimpleLatLngObject;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by shubhamsingh on 30/06/16.
 */
public class LatLngProducer implements Runnable{

    private LinkedBlockingQueue<SimpleLatLngObject> queue;
    private boolean running;
    private SimpleLatLngObject simpleLatLng;
    private static String TAG = LatLngProducer.class.getSimpleName();

    public LatLngProducer(LinkedBlockingQueue queue, SimpleLatLngObject simpleLatLng) {
        this.queue = queue;
        this.running = true;
        this.simpleLatLng = simpleLatLng;
    }

    public boolean isRunning(){
        return running;
    }

    @Override
    public void run() {
        try {
            queue.put(simpleLatLng);
            Log.d(TAG, "Inserting element: "+simpleLatLng.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.running = false;
    }
}
