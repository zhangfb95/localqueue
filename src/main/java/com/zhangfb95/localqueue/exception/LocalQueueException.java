package com.zhangfb95.localqueue.exception;

/**
 * @author zhangfb
 */
public class LocalQueueException extends RuntimeException {

    public LocalQueueException(String message) {
        super(message);
    }

    public LocalQueueException(String message, Throwable cause) {
        super(message, cause);
    }
}
