package com.zhangfb95.localqueue.exception;

/**
 * @author zhangfb
 */
public class FileCreateFailException extends LocalQueueException {

    public FileCreateFailException(String message) {
        super(message);
    }

    public FileCreateFailException(String message, Throwable cause) {
        super(message, cause);
    }
}
