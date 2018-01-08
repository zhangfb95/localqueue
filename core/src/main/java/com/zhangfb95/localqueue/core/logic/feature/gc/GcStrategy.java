package com.zhangfb95.localqueue.core.logic.feature.gc;

/**
 * @author zhangfb
 */
public interface GcStrategy {

    /**
     * 资源释放
     */
    void release();

    /**
     * 停止gc
     */
    void stop();
}
