package com.zhangfb95.localqueue.core.logic.feature.gc;

import java.io.File;

/**
 * @author zhangfb
 */
public interface GcCondition {

    boolean canGc(File file);
}
