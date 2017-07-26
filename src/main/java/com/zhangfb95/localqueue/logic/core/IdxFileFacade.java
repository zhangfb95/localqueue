package com.zhangfb95.localqueue.logic.core;

import com.zhangfb95.localqueue.logic.bean.IdxBean;
import com.zhangfb95.localqueue.util.CloseUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author zhangfb
 */
@Slf4j
public class IdxFileFacade {

    private Lock lock = new ReentrantReadWriteLock().writeLock();

    private String filePath;
    private RandomAccessFile file;
    private FileChannel fc;
    private MappedByteBuffer mappedByteBuffer;

    public IdxFileFacade(String filePath) {
        this.filePath = filePath;
    }

    void init() {
        try {
            file = new RandomAccessFile(filePath, "rwd");
            fc = file.getChannel();
            mappedByteBuffer = fc.map(FileChannel.MapMode.READ_WRITE, 0L, 1024L * 1024L * 10L);
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    IdxBean poll() {
        lock.lock();
        try {
            IdxBean idxBean = new IdxBean();
            idxBean.setReadDataFileIdx(get(0, 0));
            idxBean.setReadIdx(get(1, 0));
            idxBean.setWriteDataFileIdx(get(2, 0));
            idxBean.setWriteIdx(get(3, 0));
            return idxBean;
        } finally {
            lock.unlock();
        }
    }

    void offer(IdxBean idxBean) {
        lock.lock();
        try {
            put(0, idxBean.getReadDataFileIdx());
            put(1, idxBean.getReadIdx());
            put(2, idxBean.getWriteDataFileIdx());
            put(3, idxBean.getWriteIdx());
        } finally {
            lock.unlock();
        }
    }

    void offerReadDataFileIdx(Integer value) {
        lock.lock();
        try {
            IdxBean idxBean = poll();
            put(0, value);
            put(1, idxBean.getReadIdx());
            put(2, idxBean.getWriteDataFileIdx());
            put(3, idxBean.getWriteIdx());
        } finally {
            lock.unlock();
        }
    }

    void offerReadIdx(Integer value) {
        lock.lock();
        try {
            put(1, value);
        } finally {
            lock.unlock();
        }
    }

    void offerWriteDataFileIdx(Integer value) {
        lock.lock();
        try {
            put(2, value);
        } finally {
            lock.unlock();
        }
    }

    void offerWriteIdx(Integer value) {
        lock.lock();
        try {
            put(3, value);
        } finally {
            lock.unlock();
        }
    }

    void close() {
        CloseUtil.closeQuietly(fc);
        CloseUtil.closeQuietly(file);
    }

    private Integer get(int position, Integer defaultValue) {
        int pos = position * 4;

        try {
            return mappedByteBuffer.getInt(pos);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            mappedByteBuffer.position(pos);
            mappedByteBuffer.putInt(defaultValue);
        }
        return defaultValue;
    }

    private void put(int position, Integer value) {
        int pos = position * 4;
        mappedByteBuffer.position(pos);
        mappedByteBuffer.putInt(value);
        mappedByteBuffer.force();
    }
}
