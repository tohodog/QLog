package com.qsinong.example;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

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

        QLog.init(QLogConfig.Build(getApplication()).day(30).methodCount(1).debug(true).build());

        findViewById(R.id.tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //测试性能把 init(...debug(false)) ,安卓自带的日志打印需要损耗性能
                long t = System.currentTimeMillis();
                for (int i = 0; i < 10; i++) {
                    QLog.d("log11111111111111111111111111111111111111111");
                }
                Log.e("耗时", "" + (System.currentTimeMillis() - t));

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        long t = System.currentTimeMillis();
                        for (int i = 0; i < 10; i++) {
                            QLog.d("log222222222222222222222222222222222222222");
                        }
                        Log.v("耗时", "" + (System.currentTimeMillis() - t));
                    }
                }).start();
            }
        });

        QLog.e("RuntimeException", "出错啦", new RuntimeException("xxxxxxxxxxx"));

    }
}
