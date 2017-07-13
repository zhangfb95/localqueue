package com.zhangfb95.localqueue;

import com.zhangfb95.localqueue.exception.FileCreateFailException;
import com.zhangfb95.localqueue.util.Strings;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author zhangfb
 */
@Slf4j
public class LocalQueueStartup {

    private ConfigBean configBean;

    public LocalQueueStartup(ConfigBean configBean) {
        this.configBean = configBean;
    }

    public void start() {
        RandomAccessFile accessFile = readFile();
    }

    /**
     * read file, if file is still not existed, throw an Exception
     *
     * @return nio access file
     */
    private RandomAccessFile readFile() {
        String name = configBean.getFileName();
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
