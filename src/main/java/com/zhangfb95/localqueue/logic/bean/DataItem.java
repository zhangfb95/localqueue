package com.zhangfb95.localqueue.logic.bean;

import lombok.Data;

/**
 * @author zhangfb
 */
@Data
public class DataItem {

    private Long length; // 内容长度
    private byte[] content; // 内容
}
