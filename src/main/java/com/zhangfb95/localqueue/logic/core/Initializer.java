package com.zhangfb95.localqueue.logic.core;

import com.zhangfb95.localqueue.logic.bean.InputBean;
import com.zhangfb95.localqueue.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * @author zhangfb
 */
@Slf4j
public class Initializer {

    void loadIdxBean(InputBean inputBean, IdxFileFacade idxFileFacade) {
        String storageDir = inputBean.getStorageDir();
        FileUtil.makeDir(new File(storageDir));
        String idxFilePath = inputBean.getIdxFilePath();
        FileUtil.makeFile(new File(idxFilePath));
        idxFileFacade.init();
    }
}
