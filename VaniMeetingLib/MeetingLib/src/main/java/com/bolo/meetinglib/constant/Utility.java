package com.bolo.meetinglib.constant;

import android.os.Bundle;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class Utility {


    public static void logEventNew(final String category, String event, Tracker mTracker) {


        new Thread(new Runnable() {
            @Override
            public void run() {



                try {

                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory(category)
                            .setAction(event)
                            .build());
                }
                catch (Exception e){

                }

            }
        }).start();





    }
}
