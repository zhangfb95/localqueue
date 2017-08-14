package com.zhangfb95.localqueue.logic.core.gc;

/**
 * @author zhangfb
 */
public interface GcStrategy {

    void release();

    void stop();
}
