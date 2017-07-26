package com.zhangfb95.localqueue.logic.core;

import com.zhangfb95.localqueue.logic.bean.InputBean;
import lombok.Getter;
import lombok.Setter;
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
public class DefaultLocalQueue implements LocalQueue {

    private static final String IDX_FILE_NAME = "localqueue_idx.db";
    private IdxFileFacade idxFileFacade;
    private Lock lock = new ReentrantReadWriteLock().writeLock();
    private RandomAccessFile dataAccessFile = null;
    private MappedByteBuffer mappedByteBuffer;
    private long fileSize = 1024L * 1024L * 10L;

    @Getter
    @Setter
    private InputBean inputBean;
    private Context context;

    public void init() {
        context = new Context();
        context.setInputBean(inputBean);
        String idxFilePath = inputBean.getStorageDir() + File.separator + IDX_FILE_NAME;
        idxFileFacade = new IdxFileFacade(idxFilePath);
        context.setIdxBean(new Initializer().loadIdxBean(inputBean, idxFileFacade));

        try {
            mappedByteBuffer = dataAccessFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0L,
                                                               fileSize);
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    @Override public boolean offer(byte[] e) {
        lock.lock();
        try {
            mappedByteBuffer.position(idxFileFacade.poll().getWriteIdx());
            mappedByteBuffer.putInt(e.length);
            mappedByteBuffer.put(e);
            idxFileFacade.offerWriteIdx(4 + e.length);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override public byte[] poll() {
        lock.lock();
        try {
            int readIndex = idxFileFacade.poll().getReadIdx();
            int length = mappedByteBuffer.getInt(readIndex);
            byte[] data = new byte[length];
            for (int i = 0; i < length; i++) {
                data[i] = mappedByteBuffer.get(readIndex + 4 + i);
            }
            idxFileFacade.offerReadIdx(length + 4);
            return data;
        } finally {
            lock.unlock();
        }
    }

    @Override public void close() {

    }
}
