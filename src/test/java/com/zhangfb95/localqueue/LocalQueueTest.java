package com.zhangfb95.localqueue;

import com.zhangfb95.localqueue.logic.bean.InputBean;
import com.zhangfb95.localqueue.logic.core.DefaultLocalQueue;
import org.junit.Test;

/**
 * @author zhangfb
 */
public class LocalQueueTest {

    @Test
    public void test() throws Exception {
        InputBean inputBean = new InputBean();
        inputBean.setStorageDir("/Users/pro/ws/learn/localqueue/src/main/resources");
        DefaultLocalQueue localQueue = new DefaultLocalQueue(inputBean);

        try {
            localQueue.init();
            //localQueue.offer("nimei张付兵3".getBytes("utf-8"));
            //localQueue.offer("nimei张付兵4".getBytes("utf-8"));
            System.out.println(new String(localQueue.poll(), "utf-8"));
        } finally {
            localQueue.close();
        }

    }
}
