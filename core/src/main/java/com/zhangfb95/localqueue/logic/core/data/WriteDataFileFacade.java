package com.zhangfb95.localqueue.logic.core.data;

import com.zhangfb95.localqueue.logic.bean.Config;
import com.zhangfb95.localqueue.util.CloseUtil;
import com.zhangfb95.localqueue.util.FileUtil;
import lombok.Getter;
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
public class WriteDataFileFacade implements AutoCloseable {

    private Config config;
    @Getter
    private volatile String fileName;
    private RandomAccessFile writeDataAccessFile = null;
    private FileChannel writeDataFileChannel = null;
    private MappedByteBuffer writeMappedByteBuffer;

    public WriteDataFileFacade(Config config) {
        this.config = config;
    }

    public void init(int writeDataFileIdx) {
        try {
            generateWriteDataResource(writeDataFileIdx);
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
    }

    public void generateWriteDataResource(int writeDataFileIdx) throws IOException {
        String writeDataFilePath = generateDataFilePath(writeDataFileIdx);
        boolean newCreated = FileUtil.makeFile(writeDataFilePath);
        fileName = new File(writeDataFilePath).getName();
        writeDataAccessFile = new RandomAccessFile(writeDataFilePath, "rwd");
        writeDataFileChannel = writeDataAccessFile.getChannel();
        writeMappedByteBuffer = generateBuffer(writeDataFileChannel);

        if (newCreated) {
            initOfferFileCapacity();
            initOfferNextFileIdx(writeDataFileIdx);
            initOfferWriteIdx(DataFileStructureEnum.totalBytes());
        }
    }

    public int pollFileCapacity() {
        return writeMappedByteBuffer.getInt(DataFileStructureEnum.FileCapacity.getPos());
    }

    public void putData(int writeIndex, byte[] data) {
        writeMappedByteBuffer.position(writeIndex);
        writeMappedByteBuffer.putInt(data.length);
        writeMappedByteBuffer.put(data);
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
