package com.qsinong.example;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.qsinong.qlog.Level;
import com.qsinong.qlog.LogFormat;
import com.qsinong.qlog.QLog;
import com.qsinong.qlog.QLogConfig;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);

        QLog.init(QLogConfig.Build(getApplication())
                .buffSize(256 * 1024)//buff大小
                .delay(10000)//延迟写入时间
                .day(30)//日志保留30天,默认无限制
                .methodCount(0)//打印调用方法名
                .debug(false)//true会输出控制台,上线可关掉
                .logFormat(new LogFormat() {
                    @Override
                    public String format(Level level, String time, String log, String stact) {
                        return level + " " + time + " " + log + " --" + stact;
                    }
                })
                .build());


        findViewById(R.id.tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //测试性能把 init(...debug(false)) ,安卓自带的日志打印需要损耗性能

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        long t = System.currentTimeMillis();
                        for (int i = 0; i < 10000; i++)
                            QLog.i("info日志info日志info日志info日志info日志info日志info日志info日志");
                        Log.e("子线程耗时", "" + (System.currentTimeMillis() - t));
                    }
                }).start();

                long t = System.currentTimeMillis();
                for (int i = 0; i < 10000; i++)
                    QLog.e("login", "error日志error日志error日志error日志error日志error日志error日志error日志");
                Log.e("主线程耗时", "" + (System.currentTimeMillis() - t));
            }
        });

        QLog.e("RuntimeException", "出错啦", new RuntimeException("xxxxxxxxxxx"));

//        QLog.flush();
    }
}
