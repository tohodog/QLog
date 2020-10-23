package com.qsinong.qlog;

import android.annotation.SuppressLint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
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
