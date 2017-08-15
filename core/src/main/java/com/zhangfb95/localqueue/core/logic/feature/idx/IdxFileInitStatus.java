package com.zhangfb95.localqueue.core.logic.feature.idx;

import lombok.Getter;

/**
 * @author zhangfb
 */
@Getter
public enum IdxFileInitStatus {

    UNINITIALIZED(0, "未初始化"),
    INITIALIZED(1, "已初始化");

    private int code;
    private String name;

    IdxFileInitStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }
}
