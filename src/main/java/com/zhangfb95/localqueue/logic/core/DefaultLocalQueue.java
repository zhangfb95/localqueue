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
                boolean newCreated = FileUtil.makeFile(new File(writeDataFileName));
                writeDataAccessFile = new RandomAccessFile(writeDataFileName, "rwd");
                writeDataFileChannel = writeDataAccessFile.getChannel();
                writeMappedByteBuffer = writeDataFileChannel.map(FileChannel.MapMode.READ_WRITE, 0L,
                                                                 inputBean.getDataFileCapacity());

                if (newCreated) {
                    offerFileCapacity();
                    offerNextFileIdx();
                }

                readDataAccessFile = writeDataAccessFile;
                readDataFileChannel = writeDataFileChannel;
                readMappedByteBuffer = writeMappedByteBuffer;
            } else {
                String writeDataFileName = inputBean.getStorageDir() + File.separator
                                           + "localqueue_data_" + idxBean.getWriteDataFileIdx() + ".db";
                boolean newCreated = FileUtil.makeFile(new File(writeDataFileName));
                writeDataAccessFile = new RandomAccessFile(writeDataFileName, "rwd");
                writeDataFileChannel = writeDataAccessFile.getChannel();
                writeMappedByteBuffer = writeDataFileChannel.map(FileChannel.MapMode.READ_WRITE, 0L,
                                                                 inputBean.getDataFileCapacity());

                if (newCreated) {
                    offerFileCapacity();
                    offerNextFileIdx();
                }

                String readDataFileName = inputBean.getStorageDir() + File.separator
                                          + "localqueue_data_" + idxBean.getReadDataFileIdx() + ".db";
                FileUtil.makeFile(new File(readDataFileName));
                readDataAccessFile = new RandomAccessFile(readDataFileName, "rwd");
                readDataFileChannel = readDataAccessFile.getChannel();
                readMappedByteBuffer = readDataFileChannel.map(FileChannel.MapMode.READ_WRITE, 0L,
                                                               inputBean.getDataFileCapacity());
            }
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    @Override public boolean offer(byte[] e) {
        lock.lock();
        try {
            int writeIndex = idxFileFacade.poll().getWriteIdx();

            // 如果超过文件的容量，则需要另外开启一个文件
            if (writeIndex + 4 + e.length > writeMappedByteBuffer.getInt(0)) {
                try {
                    int newWriteDataFileIdx = idxFileFacade.poll().getWriteDataFileIdx() + 1;
                    String writeDataFileName = inputBean.getStorageDir() + File.separator
                                               + "localqueue_data_" + newWriteDataFileIdx + ".db";
                    boolean newCreated = FileUtil.makeFile(new File(writeDataFileName));
                    writeDataAccessFile = new RandomAccessFile(writeDataFileName, "rwd");
                    writeDataFileChannel = writeDataAccessFile.getChannel();
                    writeMappedByteBuffer = writeDataFileChannel.map(FileChannel.MapMode.READ_WRITE, 0L,
                                                                     inputBean.getDataFileCapacity());
                    idxFileFacade.offerWriteDataFileIdx(newWriteDataFileIdx);
                    idxFileFacade.offerWriteIdx(0);

                    if (newCreated) {
                        offerFileCapacity();
                        offerNextFileIdx();
                    }
                    writeIndex = idxFileFacade.poll().getWriteIdx();
                } catch (IOException e1) {
                    log.error(e1.getLocalizedMessage(), e);
                }
            }

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
            if (readIndex == 0) {
                readIndex += 8;
            }

            int length = readMappedByteBuffer.getInt(readIndex);

            // 如果超过文件的容量，则需要另外开启一个文件
            if (readIndex + length + 4 > readMappedByteBuffer.getInt(0)) {
                try {
                    CloseUtil.closeQuietly(readDataFileChannel);
                    CloseUtil.closeQuietly(readDataAccessFile);

                    int nextFileIdx = readMappedByteBuffer.getInt(4);
                    String readDataFileName = inputBean.getStorageDir() + File.separator
                                              + "localqueue_data_" + nextFileIdx + ".db";
                    readDataAccessFile = new RandomAccessFile(readDataFileName, "rwd");
                    readDataFileChannel = readDataAccessFile.getChannel();
                    readMappedByteBuffer = readDataFileChannel.map(FileChannel.MapMode.READ_WRITE, 0L,
                                                                   inputBean.getDataFileCapacity());
                    idxFileFacade.offerReadIdx(0);
                    idxFileFacade.offerReadDataFileIdx(nextFileIdx);
                } catch (IOException e) {
                    log.error(e.getLocalizedMessage(), e);
                }
            }

            // 如果读取和写入的文件是同一个，且读索引比写索引大，则认为没有下一个可读的数据
            int writeIndex = idxFileFacade.poll().getWriteIdx();
            if (Objects.equals(idxFileFacade.poll().getReadDataFileIdx(),
                               idxFileFacade.poll().getWriteDataFileIdx()) &&
                readIndex >= writeIndex) {
                return null;
            }

            // 重新载入读索引和数据长度
            readIndex = idxFileFacade.poll().getReadIdx();
            if (readIndex == 0) {
                readIndex += 8;
            }
            length = readMappedByteBuffer.getInt(readIndex);

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

    private void offerFileCapacity() {
        lock.lock();
        try {
            int writeIndex = idxFileFacade.poll().getWriteIdx();
            writeMappedByteBuffer.position(0);
            writeMappedByteBuffer.putInt(inputBean.getDataFileCapacity());
            idxFileFacade.offerWriteIdx(writeIndex + 4);
        } finally {
            lock.unlock();
        }
    }

    private void offerNextFileIdx() {
        lock.lock();
        try {
            int writeIndex = idxFileFacade.poll().getWriteIdx();
            writeMappedByteBuffer.position(4);
            writeMappedByteBuffer.putInt(idxFileFacade.poll().getWriteDataFileIdx() + 1);
            idxFileFacade.offerWriteIdx(writeIndex + 4);
        } finally {
            lock.unlock();
        }
    }
}
