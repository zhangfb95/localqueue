package com.zhangfb95.localqueue.logic.bean;

import lombok.Data;

/**
 * @author zhangfb
 */
@Data
public class InputBean {

    private String storageDir; // 存储位置
    private Integer dataFileCapacity; // 数据文件容量
}
