package com.zhangfb95.localqueue.logic.core.idx;

import lombok.Getter;

/**
 * @author zhangfb
 */
@Getter
public enum IdxFileStructureEnum {

    ReadDataFileIdx(0, Integer.BYTES),
    ReadIdx(calcPos(ReadDataFileIdx), Integer.BYTES),
    WriteDataFileIdx(calcPos(ReadIdx), Integer.BYTES),
    WriteIdx(calcPos(WriteDataFileIdx), Integer.BYTES),
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
