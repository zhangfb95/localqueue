package com.zhangfb95.localqueue.extra.async;

import com.zhangfb95.localqueue.extra.async.bean.Operation;

/**
 * @author zhangfb
 */
public class Persistence {

    private Serialization serialization;
    private DeSerialization deSerialization;

    public Persistence(Serialization serialization, DeSerialization deSerialization) {
        this.serialization = serialization;
        this.deSerialization = deSerialization;
    }

    public void save(Operation operation) {

    }

    public Operation extract() {
        return null;
    }
}
