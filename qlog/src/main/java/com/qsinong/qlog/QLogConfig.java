package com.qsinong.qlog;

import android.app.Application;

/**
 * Created by song on 2020/10/22
 * QLog框架全局配置
 */
public class QLogConfig {

    //两个条件符合一个即会写入磁盘

    //触发缓存写入硬盘时间间隔
    public static final int TIMESPACE = 10_000;
    //触发缓存写入硬盘缓存大小
    public static final int BUFFSIZE = 128 * 1024;//128k

    private Application application;
    private boolean debug;
    private String path;
    private int delay;
    private int buffSize;
    private int methodCount;
    private int day;
    private LogFormat logFormat;
    private WriteData writeData;

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

    public int day() {
        return day;
    }

    public LogFormat logFormat() {
        return logFormat;
    }

    public WriteData writeData() {
        return writeData;
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
        private int day;
        private LogFormat logFormat;
        private WriteData writeData;

        private Builder(Application application) {
            this.application = application;
            this.path = application.getExternalFilesDir(null) + "/Qlog";
        }

        public QLogConfig build() {
            QLogConfig qsHttpConfig = new QLogConfig();
            qsHttpConfig.application = application;
            qsHttpConfig.debug = debug;
            qsHttpConfig.path = path;
            qsHttpConfig.delay = delay;
            qsHttpConfig.buffSize = buffSize;
            qsHttpConfig.methodCount = methodCount;
            qsHttpConfig.day = day;
            qsHttpConfig.logFormat = logFormat;
            qsHttpConfig.writeData = writeData;
            return qsHttpConfig;
        }

        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Builder application(Application application) {
            this.application = application;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder delay(int delay) {
            this.delay = delay;
            return this;
        }

        public Builder buffSize(int buffSize) {
            this.buffSize = buffSize;
            return this;
        }

        public Builder methodCount(int methodCount) {
            this.methodCount = methodCount;
            return this;
        }

        public Builder day(int day) {
            this.day = day;
            return this;
        }

        public Builder logFormat(LogFormat logFormat) {
            this.logFormat = logFormat;
            return this;
        }

        public Builder writeData(WriteData writeData) {
            this.writeData = writeData;
            return this;
        }

    }
}
