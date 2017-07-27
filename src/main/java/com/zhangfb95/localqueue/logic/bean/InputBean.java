package com.zhangfb95.localqueue.logic.bean;

import lombok.Data;

/**
 * @author zhangfb
 */
@Data
public class InputBean {

    private String storageDir; // 存储位置
    private Integer dataFileCapacity; // 数据文件容量
    private String idxFileName = "localqueue_idx.db"; // 索引文件名称
    private String dataFileName = "localqueue_data_%d.db";  // 数据文件名称，其中"%d"为文件自增序号
}
