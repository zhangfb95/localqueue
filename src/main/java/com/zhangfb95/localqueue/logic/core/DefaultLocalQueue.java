package com.zhangfb95.localqueue.logic.core;

import com.zhangfb95.localqueue.logic.bean.IdxBean;
import com.zhangfb95.localqueue.logic.bean.Config;
import com.zhangfb95.localqueue.logic.core.idx.IdxFileFacade;
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

    private Lock lock = new ReentrantReadWriteLock().writeLock();
    private Config config;
    private IdxFileFacade idxFileFacade;

    private RandomAccessFile writeDataAccessFile = null;
    private FileChannel writeDataFileChannel = null;
    private MappedByteBuffer writeMappedByteBuffer;

    private RandomAccessFile readDataAccessFile = null;
    private FileChannel readDataFileChannel = null;
    private MappedByteBuffer readMappedByteBuffer;

    public DefaultLocalQueue(Config config) {
        this.config = config;
    }

    @Override public void init() {
        FileUtil.makeDir(new File(config.getStorageDir()));
        idxFileFacade = new IdxFileFacade(config.getIdxFilePath());
        idxFileFacade.init();

        try {
            IdxBean idxBean = idxFileFacade.poll();
            String writeDataFilePath = generateDataFilePath(idxBean.getWriteDataFileIdx());
            boolean newCreated = FileUtil.makeFile(new File(writeDataFilePath));
            writeDataAccessFile = new RandomAccessFile(writeDataFilePath, "rwd");
            writeDataFileChannel = writeDataAccessFile.getChannel();
            writeMappedByteBuffer = generateBuffer(writeDataFileChannel);

            if (newCreated) {
                offerFileCapacity();
                offerNextFileIdx();
                if (isReadAndWriteTheSameFile(idxBean)) {
                    offerWriteIdx4New();
                }
            }

            String readDataFilePath = generateDataFilePath(idxBean.getReadDataFileIdx());
            FileUtil.makeFile(new File(readDataFilePath));
            readDataAccessFile = new RandomAccessFile(readDataFilePath, "rwd");
            readDataFileChannel = readDataAccessFile.getChannel();
            readMappedByteBuffer = generateBuffer(readDataFileChannel);
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    @Override public boolean offer(byte[] e) {
        lock.lock();
        try {
            int writeIndex = idxFileFacade.poll().getWriteIdx();

            // 如果超过文件的容量，则需要另外开启一个文件
            if (writeIndex + Integer.BYTES + e.length > writeMappedByteBuffer.getInt(0)) {
                try {
                    int newWriteDataFileIdx = idxFileFacade.poll().getWriteDataFileIdx() + 1;
                    String writeDataFileName = generateDataFilePath(newWriteDataFileIdx);
                    boolean newCreated = FileUtil.makeFile(new File(writeDataFileName));
                    writeDataAccessFile = new RandomAccessFile(writeDataFileName, "rwd");
                    writeDataFileChannel = writeDataAccessFile.getChannel();
                    writeMappedByteBuffer = generateBuffer(writeDataFileChannel);
                    idxFileFacade.offerWriteDataFileIdx(newWriteDataFileIdx);
                    idxFileFacade.offerWriteIdx(0);

                    if (newCreated) {
                        offerFileCapacity();
                        offerNextFileIdx();
                        offerWriteIdx4New();
                    }
                    writeIndex = idxFileFacade.poll().getWriteIdx();
                } catch (IOException e1) {
                    log.error(e1.getLocalizedMessage(), e);
                }
            }

            writeMappedByteBuffer.position(writeIndex);
            writeMappedByteBuffer.putInt(e.length);
            writeMappedByteBuffer.put(e);
            idxFileFacade.offerWriteIdx(writeIndex + Integer.BYTES + e.length);
            offerWriteIdx();
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
                readIndex += 12;
            }

            int length = readMappedByteBuffer.getInt(readIndex);

            // 如果读取和写入的文件是同一个，且读索引比写索引大，则认为没有下一个可读的数据
            int writeIndex = readMappedByteBuffer.getInt(8);
            if (isReadAndWriteTheSameFile(idxFileFacade.poll()) && readIndex >= writeIndex) {
                return null;
            }

            // 如果超过文件的容量，则需要另外开启一个文件
            if (readIndex + length + Integer.BYTES > readMappedByteBuffer.getInt(8)) {
                try {
                    CloseUtil.closeQuietly(readDataFileChannel);
                    CloseUtil.closeQuietly(readDataAccessFile);

                    int nextFileIdx = readMappedByteBuffer.getInt(4);
                    String readDataFileName = generateDataFilePath(nextFileIdx);
                    readDataAccessFile = new RandomAccessFile(readDataFileName, "rwd");
                    readDataFileChannel = readDataAccessFile.getChannel();
                    readMappedByteBuffer = generateBuffer(readDataFileChannel);
                    idxFileFacade.offerReadIdx(0);
                    idxFileFacade.offerReadDataFileIdx(nextFileIdx);
                    readIndex = idxFileFacade.poll().getReadIdx();
                } catch (IOException e) {
                    log.error(e.getLocalizedMessage(), e);
                }
            }

            // 重新载入读索引和数据长度
            if (readIndex == 0) {
                readIndex += 12;
            }
            length = readMappedByteBuffer.getInt(readIndex);

            byte[] data = new byte[length];
            for (int i = 0; i < length; i++) {
                data[i] = readMappedByteBuffer.get(readIndex + Integer.BYTES + i);
            }
            idxFileFacade.offerReadIdx(readIndex + length + Integer.BYTES);
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
            writeMappedByteBuffer.putInt(config.getDataFileCapacity());
            idxFileFacade.offerWriteIdx(writeIndex + Integer.BYTES);
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
            idxFileFacade.offerWriteIdx(writeIndex + Integer.BYTES);
        } finally {
            lock.unlock();
        }
    }

    private void offerWriteIdx() {
        lock.lock();
        try {
            int writeIndex = idxFileFacade.poll().getWriteIdx();
            writeMappedByteBuffer.position(8);
            writeMappedByteBuffer.putInt(writeIndex);
        } finally {
            lock.unlock();
        }
    }

    private void offerWriteIdx4New() {
        lock.lock();
        try {
            offerWriteIdx();
            int writeIndex = idxFileFacade.poll().getWriteIdx();
            idxFileFacade.offerWriteIdx(writeIndex + Integer.BYTES);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 是否读写同一个文件
     *
     * @param idxBean 索引bean
     * @return true：同一文件，false：不同文件
     */
    private boolean isReadAndWriteTheSameFile(IdxBean idxBean) {
        return Objects.equals(idxBean.getReadDataFileIdx(), idxBean.getWriteDataFileIdx());
    }

    /**
     * 生成数据文件路径
     *
     * @param fileIdx 文件编号
     * @return 路径字符串
     */
    private String generateDataFilePath(Integer fileIdx) {
        String fileName = String.format(config.getDataFileName(), fileIdx);
        return config.getStorageDir() + File.separator + fileName;
    }

    /**
     * 生成buffer
     *
     * @param fileChannel 文件通道
     * @return buffer
     * @throws IOException 可能抛出的异常
     */
    private MappedByteBuffer generateBuffer(FileChannel fileChannel) throws IOException {
        final Long initPosition = 0L;
        return fileChannel.map(FileChannel.MapMode.READ_WRITE, initPosition, config.getDataFileCapacity());
    }
}
