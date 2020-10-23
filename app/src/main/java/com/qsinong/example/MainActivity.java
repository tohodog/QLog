package com.qsinong.example;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.qsinong.example.single.QLog;
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

        QLog.init(QLogConfig.Build(getApplication()).debug(true).build());
        findViewById(R.id.tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                long t = System.currentTimeMillis();
                for (int i = 0; i < 10000; i++) {
                    QLog.d("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxfdsfsfsdfsfsdfsdfsfssd");
                }
                Log.e("耗时", "" + (System.currentTimeMillis() - t));

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        long t = System.currentTimeMillis();
                        for (int i = 0; i < 10000; i++) {
                            QLog.d("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx3猜猜菜菜错错错错错3");
                        }
                        Log.e("耗时", "" + (System.currentTimeMillis() - t));
                    }
                }).start();
            }
        });

        QLog.e("cccc","hhhh",new RuntimeException("xxxxxxxxxxx"));

    }
}
