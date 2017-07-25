package com.zhangfb95.localqueue.logic.core;

import com.zhangfb95.localqueue.logic.bean.IdxBean;
import com.zhangfb95.localqueue.logic.bean.InputBean;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

/**
 * @author zhangfb
 */
@Slf4j
public class Initializer {

    private static final String IDX_FILE_NAME = "localqueue_idx.db";
    private static final String DATA_FILE_PREFIX = "localqueue_data_";
    private static final String DATA_FILE_SUFFIX = ".db";

    IdxBean loadIdxBean(InputBean inputBean, IdxFileFacade idxFileFacade) {
        String storageDir = inputBean.getStorageDir();
        makeDir(new File(storageDir));
        String idxFilePath = storageDir + File.separator + IDX_FILE_NAME;
        makeFile(new File(idxFilePath));
        idxFileFacade.init();
        return idxFileFacade.poll();
    }

    private String getDataFileName(long idx) {
        return DATA_FILE_PREFIX + idx + DATA_FILE_SUFFIX;
    }

    private void makeDir(File file) {
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

    private void makeFile(File file) {
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
        return;
    }
}
