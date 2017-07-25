package com.zhangfb95.localqueue.logic.bean;

import lombok.Data;

/**
 * @author zhangfb
 */
@Data
public class IdxBean {

    private Long readDataFileIdx; // 数据文件读索引
    private Long readIdx; // 读序号
    private Long writeDataFileIdx; // 数据文件写索引
    private Long writeIdx; // 写序号
}
