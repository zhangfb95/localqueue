package com.zhangfb95.localqueue.extra.async.generic;

import lombok.Data;

/**
 * @author zhangfb
 */
@Data
public class MethodInfo {

    private String methodName; // 方法名称
    private ParamInfo paramInfo; // 参数信息
    private ReturnInfo returnInfo; // 返回信息
}
