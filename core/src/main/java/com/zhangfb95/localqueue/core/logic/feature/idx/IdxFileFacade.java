package com.zhangfb95.localqueue.core.logic.feature.idx;

import com.zhangfb95.localqueue.core.logic.feature.data.DataFileStructureEnum;
import com.zhangfb95.localqueue.core.util.CloseUtil;
import com.zhangfb95.localqueue.core.util.FileUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

import static com.zhangfb95.localqueue.core.logic.feature.idx.IdxFileStructureEnum.InitStatus;
import static com.zhangfb95.localqueue.core.logic.feature.idx.IdxFileStructureEnum.ReadDataFileIdx;
import static com.zhangfb95.localqueue.core.logic.feature.idx.IdxFileStructureEnum.ReadIdx;
import static com.zhangfb95.localqueue.core.logic.feature.idx.IdxFileStructureEnum.WriteDataFileIdx;
import static com.zhangfb95.localqueue.core.logic.feature.idx.IdxFileStructureEnum.WriteIdx;

/**
 * 索引文件用于存储当前本地队列的进度信息，其"按照顺序"存储以下数据
 * <ul>
 * <li>读取文件序号，读取到哪个文件</li>
 * <li>读取序号，读取到文件的哪个位置</li>
 * <li>写入文件序号，写入到哪个文件</li>
 * <li>写入序号，写入到文件的哪个位置</li>
 * </ul>
 *
 * @author zhangfb
 */
@Slf4j
public class IdxFileFacade implements AutoCloseable {

    @Getter
    private volatile String fileName;
    private String filePath;
    private RandomAccessFile file;
    private FileChannel fc;
    private MappedByteBuffer mappedByteBuffer;

    public IdxFileFacade(String filePath) {
        this.filePath = filePath;
    }

    public void init() {
        try {
            FileUtil.makeFile(filePath);
            fileName = new File(filePath).getName();
            file = new RandomAccessFile(filePath, "rwd");
            fc = file.getChannel();
            mappedByteBuffer = fc.map(FileChannel.MapMode.READ_WRITE, 0L, 1024L * 1024L * 10L);
            if (!Objects.equals(pollInitStatus(), IdxFileInitStatus.INITIALIZED.getCode())) {
                initData();
            }
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    public Integer pollReadDataFileIdx() {
        return get(ReadDataFileIdx);
    }

    public Integer pollReadIdx() {
        return get(ReadIdx);
    }

    public Integer pollWriteDataFileIdx() {
        return get(WriteDataFileIdx);
    }

    public Integer pollWriteIdx() {
        return get(WriteIdx);
    }

    public void offerReadDataFileIdx(Integer value) {
        put(ReadDataFileIdx, value);
    }

    public void offerReadIdx(Integer value) {
        put(ReadIdx, value);
    }

    public void offerWriteDataFileIdx(Integer value) {
        put(WriteDataFileIdx, value);
    }

    public void offerWriteIdx(Integer value) {
        put(WriteIdx, value);
    }

    /**
     * 重置新文件读序号，包括文件序号，读取序号
     *
     * @param nextFileIdx 下一文件序号
     */
    public void resetNewFileReadIdx(int nextFileIdx) {
        offerReadDataFileIdx(nextFileIdx);
        offerReadIdx(DataFileStructureEnum.totalBytes());
    }

    /**
     * 重置新文件写序号，包括文件序号，写入序号
     *
     * @param nextFileIdx 下一文件序号
     */
    public void resetNewFileWriteIdx(int nextFileIdx) {
        offerWriteDataFileIdx(nextFileIdx);
        offerWriteIdx(DataFileStructureEnum.totalBytes());
    }

    /**
     * 释放资源
     */
    @Override
    public void close() {
        CloseUtil.closeQuietly(fc);
        CloseUtil.closeQuietly(file);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        final int fileIdxDefaultValue = 0;
        offerReadDataFileIdx(fileIdxDefaultValue);
        offerReadIdx(DataFileStructureEnum.totalBytes());
        offerWriteDataFileIdx(fileIdxDefaultValue);
        offerWriteIdx(DataFileStructureEnum.totalBytes());
        offerInitStatus(IdxFileInitStatus.INITIALIZED.getCode());
    }

    private Integer pollInitStatus() {
        return get(InitStatus);
    }

    private void offerInitStatus(int value) {
        put(InitStatus, value);
    }

    /**
     * 获取值
     */
    private Integer get(IdxFileStructureEnum structure) {
        return mappedByteBuffer.getInt(structure.getPos());
    }

    /**
     * 设置值
     */
    private void put(IdxFileStructureEnum structure, Integer value) {
        mappedByteBuffer.position(structure.getPos());
        mappedByteBuffer.putInt(value);
        mappedByteBuffer.force();
    }
}
