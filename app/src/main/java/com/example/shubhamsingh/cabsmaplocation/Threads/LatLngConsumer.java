package com.example.shubhamsingh.cabsmaplocation.Threads;

import com.example.shubhamsingh.cabsmaplocation.Activities.CabLocationActivity;
import com.example.shubhamsingh.cabsmaplocation.Listeners.ConsumerListener;
import com.example.shubhamsingh.cabsmaplocation.Objects.SimpleLatLngObject;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by shubhamsingh on 30/06/16.
 */
public class LatLngConsumer implements Runnable {

    private LinkedBlockingQueue<SimpleLatLngObject> queue;
    private static String TAG = LatLngConsumer.class.getSimpleName();
    private ConsumerListener consumerListener;

    public LatLngConsumer(LinkedBlockingQueue<SimpleLatLngObject> queue,
                          ConsumerListener consumerListener) {
        this.queue = queue;
        this.consumerListener = consumerListener;
    }

    @Override
    public void run() {
        try {
            consumerListener.onTakenFromQueue(queue.take());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
