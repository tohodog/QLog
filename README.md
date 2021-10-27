# QLog
[![qlog][qlogsvg]][star]  [![License][licensesvg]][license]  
Android Log Persistence Lightweight Framework 安卓日志持久化轻量级框架
<br/>
  * 短小精悍却五脏俱全,无任何依赖,简洁党的福音
  * 使用buff延迟写入,time+size两种条件触发
  * 按日期+TAG写入文件
  * 支持自动清理过期日志
  * 支持打印调用方法栈
  * 支持自定义日志格式
  * 支持写入拦截(自定义写入本地/上传服务器)
  * 支持记录崩溃信息
  * 非阻塞,线程安全,SimpleDateFormat优化

## DEMO
```
    QLog.init(getApplication());     //初始化,默认路径-> /Android/data/包名/files/QLog 

    QLog.i("info日志");              //写入-> 2020-10-20_QLog.log
    QLog.e("login", "error日志");    //写入-> 2020-10-20_login.log
    
    2020-10-20 08:27:00.360 INFO [main] info日志
    2020-10-20 08:27:00.360 ERROR [Thread-2] error日志
```
## 集成
### Gradle
```
allprojects {
    repositories {
        maven {
            url "https://jitpack.io"
        }
    }
}

dependencies {
    implementation 'com.github.tohodog:QLog:1.4'
}
```
也可以直接下载[QLog.java](https://raw.githubusercontent.com/tohodog/QLog/master/app/src/main/java/com/qsinong/example/single/QLog.java)单文件使用,够轻量!
<br/>
## 高级
```
QLog.init(QLogConfig.Build(getApplication())
        .path(getExternalFilesDir(null) + "/QLog")//日志目录,一般不要动安卓10限制了外部目录访问了
        .buffSize(128 * 1024)//buff大小
        .delay(10000)//延迟写入时间
        .day(30)//日志保留30天,默认无限制
        .methodCount(1)//打印调用方法名
        .debug(BuildConfig.DEBUG)//true会输出控制台,上线可关掉
        .logFormat(new LogFormat() {//自定义日记格式
             @Override
             public String format(Level level, String time, String log, String stact) {
                 return level + " " + time + " " + log + " ~" + stact;
             }
         })
         .writeData(new WriteData() {//写入拦截,可自定义写入/上传操作
             @Override
             public boolean writeData(String folder, String fileName, byte[] bytes) throws Exception {
                 return false;//false会继续执行写入, true不继续执行
             }
         })
        .build());

QLog.flush();//立即写入缓存
```

## Other
  * 有问题请Add [issues](https://github.com/tohodog/QLog/issues)
  * 如果项目对你有帮助的话欢迎[![star][starsvg]][star]

[starsvg]: https://img.shields.io/github/stars/tohodog/QLog.svg?style=social&label=Stars
[star]: https://github.com/tohodog/QLog

[qlogsvg]: https://img.shields.io/badge/Qlog-1.3-green.svg

[licensesvg]: https://img.shields.io/badge/License-Apache--2.0-red.svg
[license]: https://raw.githubusercontent.com/tohodog/QLog/master/LICENSE
