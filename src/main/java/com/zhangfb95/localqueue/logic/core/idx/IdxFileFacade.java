package com.zhangfb95.localqueue.logic.core.idx;

import com.zhangfb95.localqueue.logic.bean.IdxBean;
import com.zhangfb95.localqueue.util.CloseUtil;
import com.zhangfb95.localqueue.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
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
            boolean newCreated = FileUtil.makeFile(new File(filePath));
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

    public IdxBean poll() {
        lock.lock();
        try {
            IdxBean idxBean = new IdxBean();
            idxBean.setReadDataFileIdx(get(ReadDataFileIdx));
            idxBean.setReadIdx(get(ReadIdx));
            idxBean.setWriteDataFileIdx(get(WriteDataFileIdx));
            idxBean.setWriteIdx(get(WriteIdx));
            return idxBean;
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
        offerReadIdx(defaultValue);
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
