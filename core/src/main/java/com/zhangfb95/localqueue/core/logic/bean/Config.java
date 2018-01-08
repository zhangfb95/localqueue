package com.zhangfb95.localqueue.core.logic.bean;

import lombok.Data;

import java.io.File;

/**
 * @author zhangfb
 */
@Data
public class Config {

    /**
     * 存储位置
     */
    private String storageDir;

    /**
     * 数据文件容量
     */
    private Integer dataFileCapacity;

    /**
     * 索引文件名称
     */
    private String idxFileName = "localqueue_idx.db";

    /**
     * 数据文件名称，其中"%d"为文件自增序号
     */
    private String dataFileName = "localqueue_data_%d.db";

    /**
     * 回收超时时间
     */
    private Long gcExceedTime;

    /**
     * 回收超时时间粒度
     */
    private String gcTimeUnit;

    /**
     * 获取索引文件路径
     *
     * @return 索引文件路径字符串
     */
    public String getIdxFilePath() {
        return storageDir + File.separator + idxFileName;
    }
}
