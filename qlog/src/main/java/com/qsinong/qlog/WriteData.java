package com.qsinong.qlog;

/**
 * 写入拦截器
 */
public interface WriteData {
    /**
     * return: false会继续执行写入本地, true不继续执行
     */
    boolean writeData(String folder, String fileName, byte[] bytes) throws Exception;
}