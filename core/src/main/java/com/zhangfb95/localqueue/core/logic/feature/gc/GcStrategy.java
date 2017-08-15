package com.zhangfb95.localqueue.core.logic.feature.gc;

/**
 * @author zhangfb
 */
public interface GcStrategy {

    void release();

    void stop();
}
