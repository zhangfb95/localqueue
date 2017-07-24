package com.zhangfb95.localqueue.exception;

/**
 * @author zhangfb
 */
public class MappedByteBufferCreateException extends LocalQueueException {

    public MappedByteBufferCreateException(String message) {
        super(message);
    }

    public MappedByteBufferCreateException(String message, Throwable cause) {
        super(message, cause);
    }
}
