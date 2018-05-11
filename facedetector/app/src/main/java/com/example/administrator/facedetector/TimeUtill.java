package com.example.administrator.facedetector;

import android.util.Log;

/**
 * Created by Administrator on 2018/4/9.
 */

public class TimeUtill {

    private static final String TAG = "TimeUtil";
    private static long startTime;
    private static long endTime;
    public static void start() {
        startTime = System.currentTimeMillis();
    }

    public static long stop(int b) {
        endTime = System.currentTimeMillis();
        long time = endTime - startTime;
        Log.i(TAG, "stop: " + time);
        return time;

    }
}
