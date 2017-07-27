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
                initParams();
            }
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    private void initParams() {
        offerReadDataFileIdx(0);
        offerReadIdx(0);
        offerWriteDataFileIdx(0);
        offerWriteIdx(0);
    }

    public IdxBean poll() {
        lock.lock();
        try {
            IdxBean idxBean = new IdxBean();
            idxBean.setReadDataFileIdx(get(0));
            idxBean.setReadIdx(get(1));
            idxBean.setWriteDataFileIdx(get(2));
            idxBean.setWriteIdx(get(3));
            return idxBean;
        } finally {
            lock.unlock();
        }
    }

    public void offerReadDataFileIdx(Integer value) {
        lock.lock();
        try {
            put(0, value);
        } finally {
            lock.unlock();
        }
    }

    public void offerReadIdx(Integer value) {
        lock.lock();
        try {
            put(1, value);
        } finally {
            lock.unlock();
        }
    }

    public void offerWriteDataFileIdx(Integer value) {
        lock.lock();
        try {
            put(2, value);
        } finally {
            lock.unlock();
        }
    }

    public void offerWriteIdx(Integer value) {
        lock.lock();
        try {
            put(3, value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        CloseUtil.closeQuietly(fc);
        CloseUtil.closeQuietly(file);
    }

    private Integer get(int position) {
        int pos = position * Integer.BYTES;
        return mappedByteBuffer.getInt(pos);
    }

    private void put(int position, Integer value) {
        int pos = position * Integer.BYTES;
        mappedByteBuffer.position(pos);
        mappedByteBuffer.putInt(value);
        mappedByteBuffer.force();
    }
}
