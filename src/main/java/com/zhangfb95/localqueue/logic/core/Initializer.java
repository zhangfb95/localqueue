package com.zhangfb95.localqueue.logic.core;

import com.zhangfb95.localqueue.logic.bean.IdxBean;
import com.zhangfb95.localqueue.logic.bean.InputBean;
import com.zhangfb95.localqueue.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * @author zhangfb
 */
@Slf4j
public class Initializer {

    private static final String IDX_FILE_NAME = "localqueue_idx.db";

    IdxBean loadIdxBean(InputBean inputBean, IdxFileFacade idxFileFacade) {
        String storageDir = inputBean.getStorageDir();
        FileUtil.makeDir(new File(storageDir));
        String idxFilePath = storageDir + File.separator + IDX_FILE_NAME;
        FileUtil.makeFile(new File(idxFilePath));
        idxFileFacade.init();
        return idxFileFacade.poll();
    }
}
