package com.qsinong.example;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.qsinong.qlog.QLog;
import com.qsinong.qlog.QLogConfig;
import com.qsinong.qlog.WriteData;

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
                .path(getExternalFilesDir(null) + "/QLog")//日志目录,一般不要动安卓10限制了外部目录访问了
                .buffSize(128 * 1024)//buff大小
                .delay(10000)//延迟写入时间
                .day(30)//日志保留30天,默认无限制
                .methodCount(1)//打印调用方法名
                .debug(false)//true会输出控制台,上线可关掉
//                .logFormat(new LogFormat() {//自定义日记格式
//                    @Override
//                    public String format(Level level, String time, String log, String stact) {
//                        return level + " " + time + " " + log + " ~" + stact;
//                    }
//                })
                .writeData(new WriteData() {//写入拦截,可自定义写入/上传操作
                    @Override
                    public boolean writeData(String folder, String fileName, byte[] bytes) throws Exception {
                        return false;//false会继续执行写入, true不继续执行
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
                        throw new RuntimeException("测试崩溃子线程");

                    }
                }).start();

                long t = System.currentTimeMillis();
                for (int i = 0; i < 10000; i++)
                    QLog.e("login", "error日志error日志error日志error日志error日志error日志error日志error日志");
                Log.e("主线程耗时", "" + (System.currentTimeMillis() - t));
            }
        });

        QLog.e("RuntimeException", "出错啦", new RuntimeException("RuntimeException"));
    }

    public void onDestroy() {
        super.onDestroy();
        QLog.flush();
    }
}
