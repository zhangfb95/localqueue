package com.zhangfb95.localqueue.logic.core;

import com.zhangfb95.localqueue.logic.bean.Config;
import com.zhangfb95.localqueue.logic.core.data.ReadDataFileFacade;
import com.zhangfb95.localqueue.logic.core.data.WriteDataFileFacade;
import com.zhangfb95.localqueue.logic.core.gc.DefaultGcStrategy;
import com.zhangfb95.localqueue.logic.core.gc.GcCondition;
import com.zhangfb95.localqueue.logic.core.gc.GcStrategy;
import com.zhangfb95.localqueue.logic.core.idx.IdxFileFacade;
import com.zhangfb95.localqueue.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author zhangfb
 */
@Slf4j
public class DefaultLocalQueue implements LocalQueue, GcCondition {

    private Lock lock = new ReentrantReadWriteLock().writeLock();
    private Config config;
    private IdxFileFacade idxFileFacade;
    private WriteDataFileFacade writeDataFileFacade;
    private ReadDataFileFacade readDataFileFacade;
    private GcStrategy gcStrategy;

    public DefaultLocalQueue(Config config) {
        this.config = config;
    }

    @Override public void init() {
        initStorageDir();
        initIdxFile();
        initDataFile();
        initGcOperation();
    }

    @Override public void offer(byte[] data) {
        lock.lock();
        try {
            // 如果超过文件的容量，则需要另外开启一个文件
            if (isCrossFileCapacity(data)) {
                try {
                    int newWriteDataFileIdx = idxFileFacade.pollWriteDataFileIdx() + 1;
                    writeDataFileFacade.generateWriteDataResource(newWriteDataFileIdx);
                    if (writeDataFileFacade.isNewWriteFile()) {
                        idxFileFacade.resetNewFileWriteIdx(newWriteDataFileIdx);
                    }
                } catch (IOException e) {
                    log.error(e.getLocalizedMessage(), e);
                }
            }

            int writeIndex = idxFileFacade.pollWriteIdx();
            writeDataFileFacade.putData(writeIndex, data);

            int increasedWriteIdx = writeIndex + Integer.BYTES + data.length;
            idxFileFacade.offerWriteIdx(increasedWriteIdx);
            writeDataFileFacade.offerWriteIdxInDataFile(increasedWriteIdx);
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
                    readDataFileFacade.close();
                    int nextFileIdx = readDataFileFacade.getNextFileIdx();
                    readDataFileFacade.generateReadDataResource(nextFileIdx);
                    idxFileFacade.resetNewFileReadIdx(nextFileIdx);
                } catch (IOException e) {
                    log.error(e.getLocalizedMessage(), e);
                }
            }

            int readIndex = idxFileFacade.pollReadIdx();
            byte[] data = readDataFileFacade.readData(readIndex);
            idxFileFacade.offerReadIdx(readIndex + Integer.BYTES + data.length);
            return data;
        } finally {
            lock.unlock();
        }
    }

    @Override public List<byte[]> poll(int count) {
        List<byte[]> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            byte[] data = poll();
            // 数据已经读取完毕，跳出循环以便返回
            if (data == null) {
                break;
            }
            list.add(data);
        }
        return list;
    }

    @Override public void close() {
        lock.lock();
        try {
            writeDataFileFacade.close();
            readDataFileFacade.close();
            idxFileFacade.close();
            gcStrategy.stop();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 当满足以下条件时，可以删除文件，
     * 1. 不是索引文件
     * 2. 不是写文件
     * 3. 文件索引号小于读文件的文件索引号
     *
     * @param file 待删除的文件
     * @return true：可以删除，false：不可以删除
     */
    @Override public boolean canGc(File file) {
        String fileName = file.getName();
        int fileNameLength = fileName.length();
        String dataFileName = config.getDataFileName();
        int dataFileNameLength = dataFileName.length();

        int startIndex = 0;
        for (int i = 0; i < dataFileNameLength; i++) {
            if (!Objects.equals(fileName.charAt(i), dataFileName.charAt(i))) {
                startIndex = i;
                break;
            }
        }
        int endIndex = 0;
        for (int i = dataFileNameLength - 1, j = fileNameLength - 1; i >= 0; i--, j--) {
            if (!Objects.equals(dataFileName.charAt(i), fileName.charAt(j))) {
                endIndex = j;
                break;
            }
        }
        int idx = Integer.parseInt(fileName.substring(startIndex, endIndex + 1));
        return !Objects.equals(idxFileFacade.getFileName(), fileName) &&
               !Objects.equals(writeDataFileFacade.getFileName(), fileName) &&
               idx < idxFileFacade.pollReadDataFileIdx();
    }

    /**
     * 初始化存储目录
     */
    private void initStorageDir() {
        FileUtil.makeDir(config.getStorageDir());
    }

    /**
     * 初始化索引文件
     */
    private void initIdxFile() {
        idxFileFacade = new IdxFileFacade(config.getIdxFilePath());
        idxFileFacade.init();
    }

    /**
     * 初始化数据文件
     */
    private void initDataFile() {
        writeDataFileFacade = new WriteDataFileFacade(config);
        writeDataFileFacade.init(idxFileFacade.pollWriteDataFileIdx());
        if (writeDataFileFacade.isNewWriteFile()) {
            idxFileFacade.resetNewFileWriteIdx(idxFileFacade.pollWriteDataFileIdx());
        }
        readDataFileFacade = new ReadDataFileFacade(config);
        readDataFileFacade.init(idxFileFacade.pollReadDataFileIdx());
    }

    /**
     * 初始化gc操作
     */
    private void initGcOperation() {
        gcStrategy = new DefaultGcStrategy(config, this);
        gcStrategy.release();
    }

    /**
     * 写索引超过了文件的容量
     *
     * @param data 写入的数据
     * @return true：写入到了文件尽头，false：还可以继续写入
     */
    private boolean isCrossFileCapacity(final byte[] data) {
        final int writeIndex = idxFileFacade.pollWriteIdx();
        final int fileCapacity = writeDataFileFacade.pollFileCapacity();
        return writeIndex + Integer.BYTES + data.length > fileCapacity;
    }

    /**
     * 读索引超过了写入的容量
     *
     * @return true：读取到了文件尽头，false：还有数据可读
     */
    private boolean isCrossWriteCapacity() {
        int readIndex = idxFileFacade.pollReadIdx();
        int writeCapacity = readDataFileFacade.pollWriteIdx();
        return readIndex >= writeCapacity;
    }

    /**
     * 是否读完所有写入的数据
     */
    private boolean haveReadAllFile() {
        return isReadAndWriteTheSameFile() && isCrossWriteCapacity();
    }

    /**
     * 是否读写同一个文件
     *
     * @return true：同一文件，false：不同文件
     */
    private boolean isReadAndWriteTheSameFile() {
        return Objects.equals(idxFileFacade.pollReadDataFileIdx(), idxFileFacade.pollWriteDataFileIdx());
    }
}
