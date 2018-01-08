package com.zhangfb95.localqueue.core.logic.feature.gc;

import com.zhangfb95.localqueue.core.logic.bean.Config;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @author zhangfb
 */
@Slf4j
public class DefaultGcStrategy implements GcStrategy {

    private Config config;
    private Long gcExceedTime = 2L; // 回收超时时间
    private TimeUnit gcTimeUnit = TimeUnit.DAYS; // 回收超时时间粒度，
    private GcCondition gcCondition;
    private volatile boolean stopped = false;

    public DefaultGcStrategy(Config config, GcCondition gcCondition) {
        this.gcCondition = gcCondition;
        if (config.getGcExceedTime() != null && config.getGcTimeUnit() != null) {
            gcExceedTime = config.getGcExceedTime();
            gcTimeUnit = TimeUnit.valueOf(config.getGcTimeUnit());
        }
        this.config = config;
    }

    @Override
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
                            String fileName = file.getName();
                            if (flag) {
                                log.info("file '{}' great than exceed time, have been deleted", fileName);
                            } else {
                                log.info("delete file '{}' fail", fileName);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error(e.getLocalizedMessage(), e);
                }

                sleep();

                if (stopped) {
                    return;
                }
            }
        }).start();
    }

    @Override
    public void stop() {
        stopped = true;
    }

    private boolean greatThanGivenTime(File file) {
        return System.currentTimeMillis() - file.lastModified() > gcTimeUnit.toMillis(gcExceedTime);
    }

    private void sleep() {
        try {
            Thread.sleep(10000L);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }
}
