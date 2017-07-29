package com.zhangfb95.localqueue.logic.core.idx;

import com.zhangfb95.localqueue.logic.core.data.DataFileStructureEnum;
import com.zhangfb95.localqueue.util.CloseUtil;
import com.zhangfb95.localqueue.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.zhangfb95.localqueue.logic.core.idx.IdxFileStructureEnum.ReadDataFileIdx;
import static com.zhangfb95.localqueue.logic.core.idx.IdxFileStructureEnum.ReadIdx;
import static com.zhangfb95.localqueue.logic.core.idx.IdxFileStructureEnum.WriteDataFileIdx;
import static com.zhangfb95.localqueue.logic.core.idx.IdxFileStructureEnum.WriteIdx;

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

    private Lock lock = new ReentrantReadWriteLock().writeLock();

    private String filePath;
    private RandomAccessFile file;
    private FileChannel fc;
    private MappedByteBuffer mappedByteBuffer;

    public IdxFileFacade(String filePath) {
        this.filePath = filePath;
    }

    public void init() {
        try {
            boolean newCreated = FileUtil.makeFile(filePath);
            file = new RandomAccessFile(filePath, "rwd");
            fc = file.getChannel();
            mappedByteBuffer = fc.map(FileChannel.MapMode.READ_WRITE, 0L, 1024L * 1024L * 10L);
            if (newCreated) {
                initData();
            }
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    public Integer pollReadDataFileIdx() {
        lock.lock();
        try {
            return get(ReadDataFileIdx);
        } finally {
            lock.unlock();
        }
    }

    public Integer pollReadIdx() {
        lock.lock();
        try {
            return get(ReadIdx);
        } finally {
            lock.unlock();
        }
    }

    public Integer pollWriteDataFileIdx() {
        lock.lock();
        try {
            return get(WriteDataFileIdx);
        } finally {
            lock.unlock();
        }
    }

    public Integer pollWriteIdx() {
        lock.lock();
        try {
            return get(WriteIdx);
        } finally {
            lock.unlock();
        }
    }

    public void offerReadDataFileIdx(Integer value) {
        lock.lock();
        try {
            put(ReadDataFileIdx, value);
        } finally {
            lock.unlock();
        }
    }

    public void offerReadIdx(Integer value) {
        lock.lock();
        try {
            put(ReadIdx, value);
        } finally {
            lock.unlock();
        }
    }

    public void offerWriteDataFileIdx(Integer value) {
        lock.lock();
        try {
            put(WriteDataFileIdx, value);
        } finally {
            lock.unlock();
        }
    }

    public void offerWriteIdx(Integer value) {
        lock.lock();
        try {
            put(WriteIdx, value);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 重置新文件读序号，包括文件序号，读取序号
     *
     * @param nextFileIdx 下一文件序号
     */
    public void resetNewFileReadIdx(int nextFileIdx) {
        lock.lock();
        try {
            offerReadDataFileIdx(nextFileIdx);
            offerReadIdx(DataFileStructureEnum.totalBytes());
        } finally {
            lock.unlock();
        }
    }

    /**
     * 重置新文件写序号，包括文件序号，写入序号
     *
     * @param nextFileIdx 下一文件序号
     */
    public void resetNewFileWriteIdx(int nextFileIdx) {
        lock.lock();
        try {
            offerWriteDataFileIdx(nextFileIdx);
            offerWriteIdx(DataFileStructureEnum.totalBytes());
        } finally {
            lock.unlock();
        }
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
        final int defaultValue = 0;
        offerReadDataFileIdx(defaultValue);
        offerReadIdx(DataFileStructureEnum.totalBytes());
        offerWriteDataFileIdx(defaultValue);
        offerWriteIdx(defaultValue);
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
