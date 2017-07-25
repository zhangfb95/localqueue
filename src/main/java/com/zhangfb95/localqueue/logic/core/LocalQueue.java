package com.zhangfb95.localqueue.logic.core;

/**
 * @author zhangfb
 */
public interface LocalQueue {

    boolean offer(byte[] e);

    byte[] poll();

    void close();
}
