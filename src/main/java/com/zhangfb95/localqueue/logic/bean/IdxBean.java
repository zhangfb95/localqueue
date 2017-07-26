package com.zhangfb95.localqueue.logic.bean;

import lombok.Data;

/**
 * @author zhangfb
 */
@Data
public class IdxBean {

    private Integer readDataFileIdx; // 数据文件读索引
    private Integer readIdx; // 读序号
    private Integer writeDataFileIdx; // 数据文件写索引
    private Integer writeIdx; // 写序号
}
