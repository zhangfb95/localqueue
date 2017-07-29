package com.zhangfb95.localqueue.logic.core.data;

import com.zhangfb95.localqueue.logic.bean.Config;
import com.zhangfb95.localqueue.util.CloseUtil;
import com.zhangfb95.localqueue.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

/**
 * @author zhangfb
 */
@Slf4j
public class DataFileFacade implements AutoCloseable {

    private Config config;

    private RandomAccessFile writeDataAccessFile = null;
    private FileChannel writeDataFileChannel = null;
    private MappedByteBuffer writeMappedByteBuffer;

    private RandomAccessFile readDataAccessFile = null;
    private FileChannel readDataFileChannel = null;
    private MappedByteBuffer readMappedByteBuffer;

    public DataFileFacade(Config config) {
        this.config = config;
    }

    public void init(int writeDataFileIdx, int readDataFileIdx) {
        try {
            generateWriteDataResource(writeDataFileIdx);
            generateReadDataResource(readDataFileIdx);
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    public void offerWriteIdxInDataFile(int writeIdx) {
        writeMappedByteBuffer.position(DataFileStructureEnum.WriteIdx.getPos());
        writeMappedByteBuffer.putInt(writeIdx);
    }

    /**
     * 如果写索引还是初始的大小，则认为是新创建的文件
     *
     * @return true：是新创建文件，false：不是新创建文件
     */
    public boolean isNewWriteFile() {
        return Objects.equals(DataFileStructureEnum.totalBytes(),
                              writeMappedByteBuffer.getInt(DataFileStructureEnum.WriteIdx.getPos()));
    }

    @Override public void close() {
        CloseUtil.closeQuietly(writeDataFileChannel);
        CloseUtil.closeQuietly(writeDataAccessFile);
        CloseUtil.closeQuietly(readDataFileChannel);
        CloseUtil.closeQuietly(readDataAccessFile);
    }

    public void closeReadResource() {
        CloseUtil.closeQuietly(readDataFileChannel);
        CloseUtil.closeQuietly(readDataAccessFile);
    }

    public void generateWriteDataResource(int writeDataFileIdx) throws IOException {
        String writeDataFilePath = generateDataFilePath(writeDataFileIdx);
        boolean newCreated = FileUtil.makeFile(writeDataFilePath);
        writeDataAccessFile = new RandomAccessFile(writeDataFilePath, "rwd");
        writeDataFileChannel = writeDataAccessFile.getChannel();
        writeMappedByteBuffer = generateBuffer(writeDataFileChannel);

        if (newCreated) {
            initOfferFileCapacity();
            initOfferNextFileIdx(writeDataFileIdx);
            initOfferWriteIdx(DataFileStructureEnum.totalBytes());
        }
    }

    public void generateReadDataResource(int fileIdx) throws IOException {
        String readDataFilePath = generateDataFilePath(fileIdx);
        readDataAccessFile = new RandomAccessFile(readDataFilePath, "rwd");
        readDataFileChannel = readDataAccessFile.getChannel();
        readMappedByteBuffer = generateBuffer(readDataFileChannel);
    }

    public int pollFileCapacity() {
        return writeMappedByteBuffer.getInt(DataFileStructureEnum.FileCapacity.getPos());
    }

    public void putData(int writeIndex, byte[] data) {
        writeMappedByteBuffer.position(writeIndex);
        writeMappedByteBuffer.putInt(data.length);
        writeMappedByteBuffer.put(data);
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

    private void initOfferFileCapacity() {
        writeMappedByteBuffer.position(DataFileStructureEnum.FileCapacity.getPos());
        writeMappedByteBuffer.putInt(config.getDataFileCapacity());
    }

    private void initOfferNextFileIdx(int writeDataFileIdx) {
        writeMappedByteBuffer.position(DataFileStructureEnum.NextFileIdx.getPos());
        writeMappedByteBuffer.putInt(writeDataFileIdx + 1);
    }

    private void initOfferWriteIdx(int writeIdx) {
        offerWriteIdxInDataFile(writeIdx);
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
