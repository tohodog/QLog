package com.qsinong.qlog;

/**
 * Created by QSong
 * Contact github.com/tohodog
 * Date 2020/12/8
 */
public interface LogFormat {
    String format(Level level, String time, String log, String stact);
}
