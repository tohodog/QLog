package com.qsinong.qlog;

import android.app.Application;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by QSong
 * Contact github.com/tohodog
 * Date 2020/10/21
 * 持久化日记,延迟写入缓存机制
 * 根据(日期+TAG)建立日记文件
 */
public class QLog {

    public static final String TAG = "QLog";
    private final static QLog INSTANCE = new QLog();//不用懒汉么意义

    public static void init(Application context) {
        init(QLogConfig.Build(context).build());
    }

    public static void init(final QLogConfig qLogConfig) {
        INSTANCE.qLogConfig = qLogConfig;

        ExecutorManager.execute(new Runnable() {
            @Override
            public void run() {
                Util.checkLog(qLogConfig);
            }
        });
    }

    public static void i(String log) {
        i(TAG, log);
    }

    public static void i(String tag, String log) {
        INSTANCE.log(Level.INFO, tag, log);
    }

    public static void e(String log) {
        e(TAG, log);
    }

    public static void e(String tag, String log) {
        INSTANCE.log(Level.ERROR, tag, log);
    }

    public static void e(Throwable tr) {
        e("", tr);
    }

    public static void e(String log, Throwable tr) {
        e(TAG, log, tr);
    }

    public static void e(String tag, String log, Throwable tr) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream p = new PrintStream(bos);
        tr.printStackTrace(p);
        INSTANCE.log(Level.ERROR, tag, log + "\n" + bos.toString());
    }

    public static void w(String log) {
        w(TAG, log);
    }

    public static void w(String tag, String log) {
        INSTANCE.log(Level.WARING, tag, log);
    }

    public static void d(String log) {
        d(TAG, log);
    }

    public static void d(String tag, String log) {
        INSTANCE.log(Level.DEBUG, tag, log);
    }

    public static void v(String log) {
        v(TAG, log);
    }

    public static void v(String tag, String log) {
        INSTANCE.log(Level.VERBOSE, tag, log);
    }

    //立即持久化日志,阻塞
    public static void flush() {
        INSTANCE.flushAll();
    }

    //获取日记路径
    public static String getPath() {
        return INSTANCE.qLogConfig == null ? "" : INSTANCE.qLogConfig.path();
    }

    private Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    private QLog() {
        //崩溃监听,清空缓存
        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    e("Crash", Util.dumpPhoneInfo(qLogConfig.application()), e);
                    flush();
                } finally {
                    defaultUncaughtExceptionHandler.uncaughtException(t, e);
                }
            }
        });
    }

    private QLogConfig qLogConfig;

    private Map<String, LogInfo> map = new ConcurrentHashMap<>();

    private void flushAll() {
        for (Map.Entry<String, LogInfo> e : map.entrySet()) e.getValue().flush();
    }

    private void log(Level level, String tag, String log) {
        if (qLogConfig == null) {
            Log.e(TAG, "请先初始化QLog.init()");
            return;
        }

        String timeSSS = Util.formatTime();
        String date = timeSSS.substring(0, 10);
        String thread = Thread.currentThread().getName();
        String stact = "";
        if (qLogConfig.methodCount() > 0) {
            stact = Util.getStack(qLogConfig.methodCount());
        }

        if (qLogConfig.debug()) {
            switch (level) {
                case ERROR:
                    Log.e(tag, log + stact);
                    break;
                case INFO:
                    Log.i(tag, log + stact);
                    break;
                case DEBUG:
                    Log.d(tag, log + stact);
                    break;
                case WARING:
                    Log.w(tag, log + stact);
                    break;
                case VERBOSE:
                    Log.v(tag, log + stact);
                    break;
            }
        }

        String fileName = date + ".log";
        if (tag != null && !tag.isEmpty())
            fileName = date + "_" + tag + ".log";

        LogInfo logInfo = map.get(fileName);
        if (logInfo == null) {
            logInfo = new LogInfo(qLogConfig, fileName);
            LogInfo old = map.put(fileName, logInfo);
            if (old != null) logInfo = old;//线程安全,java8可以直接computeIfAbsent
        }

        if (logInfo.qLogConfig.logFormat() != null) {
            //自定义日记格式
            logInfo.apply(logInfo.qLogConfig.logFormat().format(level, timeSSS, log, stact) + "\n");
        } else {
            //先组装好一整条日志,不用sbuild了,自动优化
            String sb = timeSSS +
                    " " +
                    level +
                    " [" +
                    thread +
                    "] " +
                    log +
                    stact +
                    "\n";
            logInfo.apply(sb);
        }
    }

    private static class LogInfo {

        private QLogConfig qLogConfig;
        private String folder, fileName;

        private ByteArrayOutputStream buff = new ByteArrayOutputStream();//日记写入缓存
        private volatile long lastWriteTime = System.currentTimeMillis();//最后一次写入时间

        private volatile ScheduledFuture scheduledFuture;//可见性
        private ReentrantLock reentrantLock = new ReentrantLock();

        LogInfo(QLogConfig qLogConfig, String fileName) {
            this.qLogConfig = qLogConfig;
            this.folder = qLogConfig.path();
            this.fileName = fileName;
        }

        //此方法不阻塞
        void apply(final String log) {
            //优化锁机制,解决以下2个问题
            // 1.直接无脑开线程费性能
            // 2.直接写入buff有可能正在flush操作而需要加锁等待阻塞
            if (reentrantLock.tryLock()) {
                //拿到锁表示没有flush操作
                try {
                    write(log);
                } finally {
                    reentrantLock.unlock();
                }
            } else {
                //正在flush/write操作,开线程写入buff
                ExecutorManager.execute(new Runnable() {
                    @Override
                    public void run() {
                        write(log);
                    }
                });
            }
        }

        //日记写入缓存,线程安全,防止多线程日记乱了
        void write(String log) {
            try {
                reentrantLock.lock();
                try {
                    buff.write(log.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                cancel();
                long space = System.currentTimeMillis() - lastWriteTime;
                if (space > qLogConfig.delay() || buff.size() > qLogConfig.buffSize()) {
//                    ExecutorManager.execute(flushRun);
                    flushRun.run();
                } else {
                    scheduledFuture = ExecutorManager.schedule(flushRun, qLogConfig.delay() - space);
                }
            } finally {
                reentrantLock.unlock();
            }
        }

        //取消上一个任务
        private void cancel() {
            ScheduledFuture temp = scheduledFuture;
            if (temp != null && !temp.isCancelled() && !temp.isDone())
                temp.cancel(false);
            scheduledFuture = null;
        }

        private Runnable flushRun = new Runnable() {
            @Override
            public void run() {
                flush();
                lastWriteTime = System.currentTimeMillis();
            }
        };

        //日记持久化,加锁,阻塞,防止线程安全问题重复写入
        void flush() {
            try {
                reentrantLock.lock();
                long temp = System.currentTimeMillis();
                if (buff.size() > 0 && Util.writeData(qLogConfig.writeData(), folder, fileName, buff.toByteArray())) {
//                    if (qLogConfig.debug()) {
                    long use = System.currentTimeMillis() - temp;
                    Log.d(TAG, "flush->logName:" + fileName + " ,len:" + buff.size() + " ,useTime:" + use);
//                    }
                    if (buff.size() > qLogConfig.buffSize())//如果缓存过大重置,比如写入一个大log(MB),之后buff就一直很大了
                        buff = new ByteArrayOutputStream();
                    else
                        buff.reset();
                }
            } finally {
                reentrantLock.unlock();
            }
        }
    }

}
