package com.zhangfb95.localqueue.core.logic.feature.data;

import com.zhangfb95.localqueue.core.util.CloseUtil;
import com.zhangfb95.localqueue.core.logic.bean.Config;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author zhangfb
 */
@Slf4j
public class ReadDataFileFacade implements AutoCloseable {

    private Config config;
    private RandomAccessFile readDataAccessFile = null;
    private FileChannel readDataFileChannel = null;
    private MappedByteBuffer readMappedByteBuffer;

    public ReadDataFileFacade(Config config) {
        this.config = config;
    }

    public void init(int readDataFileIdx) {
        try {
            generateReadDataResource(readDataFileIdx);
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    @Override public void close() {
        CloseUtil.closeQuietly(readDataFileChannel);
        CloseUtil.closeQuietly(readDataAccessFile);
    }

    public void generateReadDataResource(int fileIdx) throws IOException {
        String readDataFilePath = generateDataFilePath(fileIdx);
        readDataAccessFile = new RandomAccessFile(readDataFilePath, "rwd");
        readDataFileChannel = readDataAccessFile.getChannel();
        readMappedByteBuffer = generateBuffer(readDataFileChannel);
    }

    public int getNextFileIdx() {
        return readMappedByteBuffer.getInt(DataFileStructureEnum.NextFileIdx.getPos());
    }

    /**
     * 读取内容数据
     *
     * @param readIndex 读索引
     * @return 内容数据
     */
    public byte[] readData(int readIndex) {
        int length = readMappedByteBuffer.getInt(readIndex);
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = readMappedByteBuffer.get(readIndex + Integer.BYTES + i);
        }
        return data;
    }

    public int pollWriteIdx() {
        return readMappedByteBuffer.getInt(DataFileStructureEnum.WriteIdx.getPos());
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
