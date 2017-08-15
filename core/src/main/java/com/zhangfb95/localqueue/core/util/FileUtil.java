package com.zhangfb95.localqueue.core.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

/**
 * @author zhangfb
 */
@Slf4j
public class FileUtil {

    public static boolean makeFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return false;
        }
        try {
            boolean flag = file.createNewFile();
            if (!flag) {
                log.error("file create error for return result, " + false);
            }
        } catch (IOException e) {
            log.error("file create error, " + e.getLocalizedMessage(), e);
        }
        return true;
    }

    public static void makeDir(String filePath) {
        makeDir(new File(filePath));
    }

    public static void makeDir(File file) {
        if (file.exists()) {
            return;
        }

        boolean flag;
        if (file.getParentFile().exists()) {
            flag = file.mkdir();
        } else {
            makeDir(file.getParentFile());
            flag = file.mkdir();
        }

        if (!flag) {
            log.error("directory create error for, " + file.getAbsolutePath());
        }
    }
}
