package com.zhangfb95.localqueue.extra.async.generic;

import lombok.Data;

import java.util.List;

/**
 * @author zhangfb
 */
@Data
public class GenericType {

    private String rawType;
    private List<GenericType> actualTypeArguments;
}
