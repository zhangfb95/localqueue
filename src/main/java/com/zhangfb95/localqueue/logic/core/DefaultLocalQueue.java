package com.zhangfb95.localqueue.logic.core;

import com.zhangfb95.localqueue.logic.bean.IdxBean;
import com.zhangfb95.localqueue.logic.bean.InputBean;
import com.zhangfb95.localqueue.util.CloseUtil;
import com.zhangfb95.localqueue.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;
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
    private RandomAccessFile writeDataAccessFile = null;
    private FileChannel writeDataFileChannel = null;
    private MappedByteBuffer writeMappedByteBuffer;


    private RandomAccessFile readDataAccessFile = null;
    private FileChannel readDataFileChannel = null;
    private MappedByteBuffer readMappedByteBuffer;

    private long fileSize = 1024L * 1024L * 10L;

    private InputBean inputBean;
    private Context context;

    public DefaultLocalQueue(InputBean inputBean) {
        this.inputBean = inputBean;
        context = new Context();
        context.setInputBean(this.inputBean);
    }

    @Override
    public void init() {
        String idxFilePath = inputBean.getStorageDir() + File.separator + IDX_FILE_NAME;
        idxFileFacade = new IdxFileFacade(idxFilePath);
        context.setIdxBean(new Initializer().loadIdxBean(inputBean, idxFileFacade));

        try {
            IdxBean idxBean = idxFileFacade.poll();
            if (Objects.equals(idxBean.getReadDataFileIdx(), idxBean.getWriteDataFileIdx())) {
                String writeDataFileName = inputBean.getStorageDir() + File.separator
                                           + "localqueue_data_" + idxBean.getWriteDataFileIdx() + ".db";
                FileUtil.makeFile(new File(writeDataFileName));
                writeDataAccessFile = new RandomAccessFile(writeDataFileName, "rwd");
                writeDataFileChannel = writeDataAccessFile.getChannel();
                writeMappedByteBuffer = writeDataFileChannel.map(FileChannel.MapMode.READ_WRITE, 0L,
                                                                 fileSize);

                readDataAccessFile = writeDataAccessFile;
                readDataFileChannel = writeDataFileChannel;
                readMappedByteBuffer = writeMappedByteBuffer;
            } else {
                String writeDataFileName = inputBean.getStorageDir() + File.separator
                                           + "localqueue_data_" + idxBean.getWriteDataFileIdx() + ".db";
                FileUtil.makeFile(new File(writeDataFileName));
                writeDataAccessFile = new RandomAccessFile(writeDataFileName, "rwd");
                writeDataFileChannel = writeDataAccessFile.getChannel();
                writeMappedByteBuffer = writeDataFileChannel.map(FileChannel.MapMode.READ_WRITE, 0L,
                                                                 fileSize);

                String readDataFileName = inputBean.getStorageDir() + File.separator
                                          + "localqueue_data_" + idxBean.getReadDataFileIdx() + ".db";
                FileUtil.makeFile(new File(readDataFileName));
                readDataAccessFile = new RandomAccessFile(readDataFileName, "rwd");
                readDataFileChannel = readDataAccessFile.getChannel();
                readMappedByteBuffer = readDataFileChannel.map(FileChannel.MapMode.READ_WRITE, 0L,
                                                               fileSize);
            }
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    @Override public boolean offer(byte[] e) {
        lock.lock();
        try {
            int writeIndex = idxFileFacade.poll().getWriteIdx();
            writeMappedByteBuffer.position(writeIndex);
            writeMappedByteBuffer.putInt(e.length);
            writeMappedByteBuffer.put(e);
            idxFileFacade.offerWriteIdx(writeIndex + 4 + e.length);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override public byte[] poll() {
        lock.lock();
        try {
            int readIndex = idxFileFacade.poll().getReadIdx();
            int length = readMappedByteBuffer.getInt(readIndex);
            byte[] data = new byte[length];
            for (int i = 0; i < length; i++) {
                data[i] = readMappedByteBuffer.get(readIndex + 4 + i);
            }
            idxFileFacade.offerReadIdx(readIndex + length + 4);
            return data;
        } finally {
            lock.unlock();
        }
    }

    @Override public void close() {
        CloseUtil.closeQuietly(writeDataFileChannel);
        CloseUtil.closeQuietly(writeDataAccessFile);
        CloseUtil.closeQuietly(readDataFileChannel);
        CloseUtil.closeQuietly(readDataAccessFile);
        idxFileFacade.close();
    }
}
