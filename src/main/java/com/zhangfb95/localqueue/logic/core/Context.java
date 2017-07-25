package com.zhangfb95.localqueue.logic.core;

import com.zhangfb95.localqueue.logic.bean.IdxBean;
import com.zhangfb95.localqueue.logic.bean.InputBean;
import lombok.Data;

/**
 * @author zhangfb
 */
@Data
public class Context {

    private InputBean inputBean;
    private IdxBean idxBean;
}
