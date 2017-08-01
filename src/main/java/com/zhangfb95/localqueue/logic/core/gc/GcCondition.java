package com.zhangfb95.localqueue.logic.core.gc;

import java.io.File;

/**
 * @author zhangfb
 */
public interface GcCondition {

    boolean canGc(File file);
}
