package com.zhangfb95.localqueue.extra.async;

import com.zhangfb95.localqueue.extra.async.generic.OperationInfo;

/**
 * @author zhangfb
 */
public interface Serialization {

    String serialize(OperationInfo methodInfo);
}
