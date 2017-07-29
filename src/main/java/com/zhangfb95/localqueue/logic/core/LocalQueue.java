package com.zhangfb95.localqueue.logic.core;

/**
 * @author zhangfb
 */
public interface LocalQueue {

    void init();

    void offer(byte[] e);

    byte[] poll();

    void close();
}
