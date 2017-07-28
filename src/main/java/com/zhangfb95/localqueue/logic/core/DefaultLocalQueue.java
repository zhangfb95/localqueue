package com.zhangfb95.localqueue.logic.core;

import com.zhangfb95.localqueue.logic.bean.Config;
import com.zhangfb95.localqueue.logic.core.data.DataFileStructureEnum;
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
        FileUtil.makeDir(config.getStorageDir());
        idxFileFacade = new IdxFileFacade(config.getIdxFilePath());
        idxFileFacade.init();

        try {
            generateWriteDataResource(idxFileFacade.pollWriteDataFileIdx());
            generateReadDataResource(idxFileFacade.pollReadDataFileIdx());
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    @Override public boolean offer(byte[] e) {
        lock.lock();
        try {
            int writeIndex = idxFileFacade.pollWriteIdx();

            // 如果超过文件的容量，则需要另外开启一个文件
            if (writeIndex + Integer.BYTES + e.length >
                writeMappedByteBuffer.getInt(DataFileStructureEnum.FileCapacity.getPos())) {
                try {
                    int newWriteDataFileIdx = idxFileFacade.pollWriteDataFileIdx() + 1;
                    generateWriteDataResource(newWriteDataFileIdx);
                    writeIndex = idxFileFacade.pollWriteIdx();
                } catch (IOException e1) {
                    log.error(e1.getLocalizedMessage(), e);
                }
            }

            writeMappedByteBuffer.position(writeIndex);
            writeMappedByteBuffer.putInt(e.length);
            writeMappedByteBuffer.put(e);
            idxFileFacade.offerWriteIdx(writeIndex + Integer.BYTES + e.length);
            offerWriteIdxInDataFile();
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override public byte[] poll() {
        lock.lock();
        try {
            // 如果读取和写入的文件是同一个，且读索引比写索引大，则认为没有下一个可读的数据
            if (haveReadAllFile()) {
                return null;
            }

            // 如果超过文件的容量，则需要另外开启一个文件
            if (isCrossWriteCapacity()) {
                try {
                    closeReadFile();
                    int nextFileIdx = getNextFileIdx();
                    generateReadDataResource(nextFileIdx);
                    idxFileFacade.resetNewFileReadIdx(nextFileIdx);
                } catch (IOException e) {
                    log.error(e.getLocalizedMessage(), e);
                }
            }

            int readIndex = idxFileFacade.pollReadIdx();
            int length = readMappedByteBuffer.getInt(readIndex);
            byte[] data = readData(readIndex, length);
            idxFileFacade.offerReadIdx(readIndex + Integer.BYTES + length);
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

    private void generateWriteDataResource(int writeDataFileIdx) throws IOException {
        String writeDataFilePath = generateDataFilePath(writeDataFileIdx);
        boolean newCreated = FileUtil.makeFile(writeDataFilePath);
        writeDataAccessFile = new RandomAccessFile(writeDataFilePath, "rwd");
        writeDataFileChannel = writeDataAccessFile.getChannel();
        writeMappedByteBuffer = generateBuffer(writeDataFileChannel);

        if (newCreated) {
            idxFileFacade.resetNewFileWriteIdx(writeDataFileIdx);
            initOfferFileCapacity();
            initOfferNextFileIdx();
            initOfferWriteIdx();
        }
    }

    private void generateReadDataResource(int fileIdx) throws IOException {
        String readDataFilePath = generateDataFilePath(fileIdx);
        readDataAccessFile = new RandomAccessFile(readDataFilePath, "rwd");
        readDataFileChannel = readDataAccessFile.getChannel();
        readMappedByteBuffer = generateBuffer(readDataFileChannel);
    }

    private int getNextFileIdx() {
        return readMappedByteBuffer.getInt(DataFileStructureEnum.NextFileIdx.getPos());
    }

    /**
     * 读索引超过了写入的容量
     *
     * @return true：读取到了文件尽头，false：还有数据可读
     */
    private boolean isCrossWriteCapacity() {
        int readIndex = idxFileFacade.pollReadIdx();
        int writeCapacity = readMappedByteBuffer.getInt(DataFileStructureEnum.WriteIdx.getPos());
        return readIndex >= writeCapacity;
    }

    private boolean haveReadAllFile() {
        return isReadAndWriteTheSameFile() && isCrossWriteCapacity();
    }

    private void closeReadFile() {
        CloseUtil.closeQuietly(readDataFileChannel);
        CloseUtil.closeQuietly(readDataAccessFile);
    }

    private void offerWriteIdxInDataFile() {
        lock.lock();
        try {
            writeMappedByteBuffer.position(DataFileStructureEnum.WriteIdx.getPos());
            writeMappedByteBuffer.putInt(idxFileFacade.pollWriteIdx());
        } finally {
            lock.unlock();
        }
    }

    private void initOfferFileCapacity() {
        lock.lock();
        try {
            writeMappedByteBuffer.position(DataFileStructureEnum.FileCapacity.getPos());
            writeMappedByteBuffer.putInt(config.getDataFileCapacity());
        } finally {
            lock.unlock();
        }
    }

    private void initOfferNextFileIdx() {
        lock.lock();
        try {
            writeMappedByteBuffer.position(DataFileStructureEnum.NextFileIdx.getPos());
            writeMappedByteBuffer.putInt(idxFileFacade.pollWriteDataFileIdx() + 1);
        } finally {
            lock.unlock();
        }
    }

    private void initOfferWriteIdx() {
        offerWriteIdxInDataFile();
    }

    /**
     * 读取内容数据
     *
     * @param readIndex 读索引
     * @param length    内容数据长度
     * @return 内容数据
     */
    private byte[] readData(int readIndex, int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = readMappedByteBuffer.get(readIndex + Integer.BYTES + i);
        }
        return data;
    }

    /**
     * 是否读写同一个文件
     *
     * @return true：同一文件，false：不同文件
     */
    private boolean isReadAndWriteTheSameFile() {
        return Objects.equals(idxFileFacade.pollReadDataFileIdx(), idxFileFacade.pollWriteDataFileIdx());
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
