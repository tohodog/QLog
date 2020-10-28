package com.qsinong.qlog;

import android.annotation.SuppressLint;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by QSong
 * Contact github.com/tohodog
 * Date 2020/10/23
 */
public class Util {

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

        try {
            File file = new File(folder);
            if (!file.exists()) file.mkdirs();
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private static String getSimpleClassName(String name) {
        int lastIndex = name.lastIndexOf(".");
        return name.substring(lastIndex + 1);
    }


    public static String getStack(int methodCount) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();//[0][1]系统方法 [2]本方法

        StringBuilder builder = new StringBuilder();

        for (int i = 3; i < trace.length; i++) {
            StackTraceElement element = trace[i];
            String name = getSimpleClassName(element.getClassName());
            if (name.startsWith(QLog.class.getSimpleName()))
                continue;
            if (methodCount == 0) break;
//            if (builder.length() > 0)
            builder.append("\n");
            builder.append(name)
                    .append(".")
                    .append(element.getMethodName())
                    .append("(")
                    .append(element.getFileName())
                    .append(":")
                    .append(element.getLineNumber())
                    .append(")");
            methodCount--;
        }
        return builder.toString();
    }

    /**
     * 检测日志删除过期的
     */
    public static void checkLog(final QLogConfig qLogConfig) {
        if (qLogConfig.day() <= 0) return;

        File f = new File(qLogConfig.path());
        if (!f.isDirectory()) return;
        File[] files = f.listFiles();
        if (files == null) return;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -qLogConfig.day() + 1);
        String date = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());

        for (File file : files) {
            if (file.getName().compareTo(date) < 0) {
                Log.i(QLog.TAG, "Del log:" + file.getName());
                file.delete();
            }
        }
    }

}
