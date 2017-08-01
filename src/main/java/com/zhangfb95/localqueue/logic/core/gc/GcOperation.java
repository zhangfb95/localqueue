package com.zhangfb95.localqueue.logic.core.gc;

import com.zhangfb95.localqueue.logic.bean.Config;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @author zhangfb
 */
@Slf4j
public class GcOperation {

    private Config config;
    private Long gcExceedTime = 2L; // 回收超时时间
    private TimeUnit gcTimeUnit = TimeUnit.DAYS; // 回收超时时间粒度，
    private GcCondition gcCondition;
    private volatile boolean stoped = false;

    public GcOperation(Config config, GcCondition gcCondition) {
        this.gcCondition = gcCondition;
        if (config.getGcExceedTime() != null && config.getGcTimeUnit() != null) {
            gcExceedTime = config.getGcExceedTime();
            gcTimeUnit = TimeUnit.valueOf(config.getGcTimeUnit());
        }
        this.config = config;
    }

    public void release() {
        final File storageDir = new File(config.getStorageDir());
        new Thread(() -> {
            while (true) {
                try {
                    File[] files = storageDir.listFiles(
                            pathname -> gcCondition.canGc(pathname) && greatThanGivenTime(pathname));
                    if (files != null && files.length > 0) {
                        for (File file : files) {
                            boolean flag = file.delete();
                            if (flag) {
                                log.info("file '{}' great than exceed time, have been deleted", file.getName());
                            }
                        }
                    } else {
                        Thread.sleep(10000L);
                    }
                } catch (Exception e) {
                    log.error(e.getLocalizedMessage(), e);
                }

                if (stoped) {
                    return;
                }
            }
        }).start();
    }

    public void stop() {
        stoped = true;
    }

    private boolean greatThanGivenTime(File file) {
        return System.currentTimeMillis() - file.lastModified() > gcTimeUnit.toMillis(gcExceedTime);
    }
}
