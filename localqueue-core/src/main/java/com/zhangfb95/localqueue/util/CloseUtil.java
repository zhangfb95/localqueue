package com.zhangfb95.localqueue.util;

import lombok.extern.slf4j.Slf4j;

/**
 * @author zhangfb
 */
@Slf4j
public class CloseUtil {

    public static void closeQuietly(AutoCloseable x) {
        if (x == null) {
            return;
        }
        try {
            x.close();
        } catch (Exception e) {
            log.error("close resource error, " + e.getLocalizedMessage(), e);
        }
    }
}
