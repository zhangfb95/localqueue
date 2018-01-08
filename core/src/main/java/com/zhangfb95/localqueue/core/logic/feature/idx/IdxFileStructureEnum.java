package com.zhangfb95.localqueue.core.logic.feature.idx;

import lombok.Getter;

/**
 * @author zhangfb
 */
@Getter
public enum IdxFileStructureEnum {

    /**
     * 读数据文件索引
     */
    ReadDataFileIdx(0, Integer.BYTES),

    /**
     * 读索引
     */
    ReadIdx(calcPos(ReadDataFileIdx), Integer.BYTES),

    /**
     * 写数据文件索引
     */
    WriteDataFileIdx(calcPos(ReadIdx), Integer.BYTES),

    /**
     * 写索引
     */
    WriteIdx(calcPos(WriteDataFileIdx), Integer.BYTES),

    /**
     * 初始状态
     */
    InitStatus(calcPos(WriteIdx), Integer.BYTES);

    private int pos;
    private int length;

    IdxFileStructureEnum(int pos, int length) {
        this.pos = pos;
        this.length = length;
    }

    private static int calcPos(IdxFileStructureEnum prev) {
        return prev.pos + prev.length;
    }
}
