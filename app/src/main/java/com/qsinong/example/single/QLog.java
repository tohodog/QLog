package com.qsinong.example.single;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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
    private final static QLog INSTANCE = new QLog();//搞啥懒汉么意义

    public static void init(Application context) {
        INSTANCE.qLogConfig = QLogConfig.Build(context).build();
    }

    public static void init(QLogConfig qLogConfig) {
        INSTANCE.qLogConfig = qLogConfig;
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


    private QLog() {
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
            stact = " ~ " + Util.getStack(5, 1);
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

        String fileName = date + ".txt";
        if (tag != null && !tag.isEmpty())
            fileName = date + "_" + tag + ".txt";

        LogInfo logInfo = map.get(fileName);
        if (logInfo == null) {
            logInfo = new LogInfo(qLogConfig, fileName);
            LogInfo old = map.put(fileName, logInfo);
            if (old != null) logInfo = old;
        }
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
                if (System.currentTimeMillis() - lastWriteTime > qLogConfig.delay() || buff.size() > qLogConfig.buffSize()) {
//                    ExecutorManager.execute(flushRun);
                    flushRun.run();
                } else {
                    scheduledFuture = ExecutorManager.schedule(flushRun, qLogConfig.delay());
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
                if (buff.size() > 0 && Util.writeData(folder, fileName, buff.toByteArray())) {
//                    if (qLogConfig.debug()) {
                    long use = System.currentTimeMillis() - temp;
                    Log.d(TAG, "flush->logName:" + fileName + " len:" + buff.size() + " useTime:" + use);
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

    //=============================================内部类===========================================

    public static class QLogConfig {

        //两个条件符合一个即会写入磁盘

        //触发缓存写入硬盘时间间隔
        public static final int TIMESPACE = 10_000;
        //触发缓存写入硬盘缓存大小
        public static final int BUFFSIZE = 256 * 1024;//256k

        private Application application;
        private boolean debug;
        private String path;
        private int delay;
        private int buffSize;
        private int methodCount;

        public Application application() {
            return application;
        }

        public boolean debug() {
            return debug;
        }

        public String path() {
            return path;
        }

        public int delay() {
            return delay;
        }

        public int buffSize() {
            return buffSize;
        }

        public int methodCount() {
            return methodCount;
        }

        private QLogConfig() {
        }

        public static Builder Build(Application application) {
            return new Builder(application);
        }

        public static final class Builder {

            private Application application;
            private boolean debug = true;
            private String path;
            private int delay = TIMESPACE;
            private int buffSize = BUFFSIZE;
            private int methodCount;

            private Builder(Application application) {
                this.application = application;
                this.path = application.getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/Qlog";
            }

            public QLogConfig build() {
                QLogConfig qsHttpConfig = new QLogConfig();
                qsHttpConfig.application = application;
                qsHttpConfig.debug = debug;
                qsHttpConfig.path = path;
                qsHttpConfig.delay = delay;
                qsHttpConfig.buffSize = buffSize;
                qsHttpConfig.methodCount = methodCount;
                return qsHttpConfig;
            }

            public QLogConfig.Builder debug(boolean debug) {
                this.debug = debug;
                return this;
            }

            public QLogConfig.Builder application(Application application) {
                this.application = application;
                return this;
            }

            public QLogConfig.Builder path(String path) {
                this.path = path;
                return this;
            }

            public QLogConfig.Builder delay(int delay) {
                this.delay = delay;
                return this;
            }

            public QLogConfig.Builder buffSize(int buffSize) {
                this.buffSize = buffSize;
                return this;
            }

            //打印方法名
            public QLogConfig.Builder methodCount(int methodCount) {
                this.methodCount = methodCount;
                return this;
            }
        }
    }

    private enum Level {
        DEBUG,
        INFO,
        WARING,
        ERROR,
        VERBOSE
    }

    private static class ExecutorManager {

        private static final int DEFAULT_THREAD_POOL_SIZE = 10;

        public static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors
                .newScheduledThreadPool(DEFAULT_THREAD_POOL_SIZE);

        public static void execute(Runnable runnable) {
            SCHEDULED_EXECUTOR_SERVICE.execute(runnable);
        }

        public static ScheduledFuture<?> schedule(Runnable runnable, long delay) {
            return SCHEDULED_EXECUTOR_SERVICE.schedule(runnable, delay, TimeUnit.MILLISECONDS);
        }
    }

    private static class Util {

        private static ThreadLocal<SimpleDateFormat> threadLocal = new ThreadLocal<>();

        @SuppressLint("SimpleDateFormat")
        public static String formatTime() {//new SimpleDateFormat这个东西太费性能了,ThreadLocal优化下
            SimpleDateFormat sdf = threadLocal.get();
            if (sdf == null) {
                sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                threadLocal.set(sdf);
            }
            return sdf.format(new Date());
        }

        public static boolean writeData(String folder, String fileName, byte[] bytes) {
            File file = new File(folder);
            if (!file.exists()) file.mkdirs();
            try {
//            PrintWriter pw = new PrintWriter(file);
//            FileOutputStream fos = new FileOutputStream(new File(file, fileName));
//            OutputStreamWriter osw = new OutputStreamWriter(fos);
//            fos.write(bytes);
//            fos.close();
                // 打开一个随机访问文件流，按读写方式
                RandomAccessFile randomFile = new RandomAccessFile(new File(file, fileName), "rw");
                randomFile.seek(randomFile.length());
                randomFile.write(bytes);
                randomFile.close();
                return true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }

        private static String getSimpleClassName(String name) {
            int lastIndex = name.lastIndexOf(".");
            return name.substring(lastIndex + 1);
        }

        public static String getStack(int stackOffset, int methodCount) {
            StackTraceElement[] trace = Thread.currentThread().getStackTrace();//[0]为本方法

//        int methodCount = 1;//打印方法数
//        int stackOffset = 4;

            if (methodCount + stackOffset > trace.length) {
                methodCount = trace.length - stackOffset;
            }

            StringBuilder builder = new StringBuilder();

            for (int i = methodCount; i > 0; i--) {
                int stackIndex = i + stackOffset;
                if (stackIndex >= trace.length) {
                    continue;
                }
                StackTraceElement element = trace[stackIndex];
                if (builder.length() > 0) builder.append("\n");
                builder.append(getSimpleClassName(element.getClassName()))
                        .append(".")
                        .append(element.getMethodName())
                        .append("(")
                        .append(element.getFileName())
                        .append(":")
                        .append(element.getLineNumber())
                        .append(")");
            }
            return builder.toString();
        }

    }
}
