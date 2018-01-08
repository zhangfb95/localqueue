package com.zhangfb95.localqueue.core.logic.feature.data;

import lombok.Getter;

/**
 * @author zhangfb
 */
@Getter
public enum DataFileStructureEnum {

    /**
     * 文件容量
     */
    FileCapacity(0, Integer.BYTES),

    /**
     * 下一个文件的索引
     */
    NextFileIdx(calcPos(FileCapacity), Integer.BYTES),

    /**
     * 写索引
     */
    WriteIdx(calcPos(NextFileIdx), Integer.BYTES);

    private int pos;
    private int length;

    private static int totalBytes = calcTotalBytes();

    DataFileStructureEnum(int pos, int length) {
        this.pos = pos;
        this.length = length;
    }

    public static int totalBytes() {
        return totalBytes;
    }

    private static int calcTotalBytes() {
        int bytes = 0;
        for (DataFileStructureEnum dataFileStructureEnum : DataFileStructureEnum.values()) {
            bytes += dataFileStructureEnum.length;
        }
        return bytes;
    }

    private static int calcPos(DataFileStructureEnum prev) {
        return prev.pos + prev.length;
    }
}
