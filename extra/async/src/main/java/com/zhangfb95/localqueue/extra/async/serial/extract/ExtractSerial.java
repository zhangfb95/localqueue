package com.zhangfb95.localqueue.extra.async.serial.extract;

import com.zhangfb95.localqueue.extra.async.bean.Operation;

/**
 * @author zhangfb
 */
public interface ExtractSerial {

    Operation extract(String str);
}
