package com.zhangfb95.localqueue.extra.async.bean;

import lombok.Data;

import java.lang.reflect.Method;

/**
 * @author zhangfb
 */
@Data
public class Operation {

    private Object instance; //  当前操作的实例
    private Method method; // 当前操作的方法
}
