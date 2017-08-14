package com.zhangfb95.localqueue.logic.core.data;

import lombok.Getter;

/**
 * @author zhangfb
 */
@Getter
public enum DataFileStructureEnum {

    FileCapacity(0, Integer.BYTES),
    NextFileIdx(calcPos(FileCapacity), Integer.BYTES),
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
