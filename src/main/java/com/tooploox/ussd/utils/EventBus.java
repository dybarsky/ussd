package com.tooploox.ussd.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import java.util.EnumMap;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 02/03/2017 17:38
 */
public class EventBus {

    public enum Event {
        USSD_RESULT_RECEIVED
    }

    @FunctionalInterface
    public interface Subscriber {
        void eventReceived(Event event);
    }

    private LocalBroadcastManager localBroadcastManager;
    private EnumMap<Event, BroadcastReceiver> receiverMap = new EnumMap<>(Event.class);

    public EventBus(Context context) {
        this.localBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    public void sendEvent(Event event) {
        Intent intent = new Intent(event.name());
        localBroadcastManager.sendBroadcast(intent);
    }

    public void subscribe(Event event, Subscriber subscriber) {
        IntentFilter intentFilter = new IntentFilter(event.name());
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Event received = Event.valueOf(intent.getAction());
                subscriber.eventReceived(received);
            }
        };
        localBroadcastManager.registerReceiver(receiver, intentFilter);
        receiverMap.put(event, receiver);
    }

    public void unsubscribe(Event event) {
        localBroadcastManager.unregisterReceiver(receiverMap.get(event));
    }
}
