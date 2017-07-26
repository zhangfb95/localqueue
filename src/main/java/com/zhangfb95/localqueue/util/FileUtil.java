package com.zhangfb95.localqueue.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

/**
 * @author zhangfb
 */
@Slf4j
public class FileUtil {

    public static void makeFile(File file) {
        if (file.exists()) {
            return;
        }
        try {
            boolean flag = file.createNewFile();
            if (!flag) {
                log.error("file create error for return result, " + false);
            }
        } catch (IOException e) {
            log.error("file create error, " + e.getLocalizedMessage(), e);
        }
    }
}
