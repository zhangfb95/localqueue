package com.zhangfb95.localqueue;

import com.zhangfb95.localqueue.exception.FileCreateFailException;
import com.zhangfb95.localqueue.util.Strings;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author zhangfb
 */
@Slf4j
public class LocalQueueStartup {

    private ConfigBean configBean;
    private RandomAccessFile accessFile = null;

    public LocalQueueStartup(ConfigBean configBean) {
        this.configBean = configBean;
    }

    public void start() throws Exception {
        accessFile = readFile(configBean.getFileName());

        long position = 0;
        long count = 1024 * 1024 * 200;
        MappedByteBuffer out = accessFile.getChannel().map(FileChannel.MapMode.READ_WRITE, position, count);

        // Writing into Memory Mapped File
        for (int i = 0; i < count; i++) {
            out.put((byte) 'A');
        }
        System.out.println("Writing to Memory Mapped File is completed");


        // reading from memory file in Java
        for (int i = 0; i < 10; i++) {
            System.out.print((char) out.get(i));
        }
        System.out.println("Reading from Memory Mapped File is completed");

        accessFile.close();
    }

    public static void main(String[] args) throws Exception {
        ConfigBean configBean = new ConfigBean();
        configBean.setFileName("/Users/pro/ws/learn/localqueue/src/main/resources/abc.txt");
        new LocalQueueStartup(configBean).start();
    }

    /**
     * read file, if file is still not existed, throw an Exception
     *
     * @return nio access file
     */
    public RandomAccessFile readFile(String name) {
        createFile(name);

        try {
            return new RandomAccessFile(name, "rwd");
        } catch (FileNotFoundException e) {
            log.error(Strings.format("file [%s] not exist", name));
            throw new FileCreateFailException(Strings.format("file [%s] create fail", name), e);
        }
    }

    /**
     * create file by config
     *
     * @param name file's name
     */
    private void createFile(String name) {
        File file = new File(name);
        if (!file.exists()) {
            if (!configBean.isCreateIfNotExist()) {
                throw new FileCreateFailException(
                        Strings.format("file [%s] not exist, and createIfNotExist is false", name));
            }

            try {
                boolean result = file.createNewFile();
                if (result) {
                    log.info(Strings.format("file [%s] create success", name));
                } else {
                    log.info(Strings.format("file [%s] exist, not created", name));
                }
            } catch (IOException ioE) {
                throw new FileCreateFailException(Strings.format("file [%s] create fail", name), ioE);
            }
        }
    }
}
