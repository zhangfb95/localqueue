package com.zhangfb95.localqueue.core.logic.feature;

import java.util.List;

/**
 * @author zhangfb
 */
public interface LocalQueue {

    void init();

    void offer(byte[] e);

    byte[] poll();

    List<byte[]> poll(int count);

    void close();
}
