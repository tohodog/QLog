package com.qsinong.qlog;

import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 线程池
 */
public final class ExecutorManager {

    private static final int DEFAULT_THREAD_POOL_SIZE = 8;


    public static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors
            .newScheduledThreadPool(DEFAULT_THREAD_POOL_SIZE);


    public static void execute(Runnable runnable) {
        SCHEDULED_EXECUTOR_SERVICE.execute(runnable);
    }

    public static Future<?> submit(Runnable runnable) {
        return SCHEDULED_EXECUTOR_SERVICE.submit(runnable);
    }

    public static ScheduledFuture<?> schedule(Runnable runnable, long delay) {
        return SCHEDULED_EXECUTOR_SERVICE.schedule(runnable, delay, TimeUnit.MILLISECONDS);
    }

    public static ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long delay, long space) {
        return SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(runnable, delay, space, TimeUnit.MILLISECONDS);
        // scheduleAtFixedRate固定时间点执行 scheduleWithFixedDelay固定执行间隔
    }

    public static void sleep(int seconds) {
        try {
            Thread.sleep(seconds);
        } catch (InterruptedException e) {
            Log.d("Error when sleeping", "");
        }
    }
}