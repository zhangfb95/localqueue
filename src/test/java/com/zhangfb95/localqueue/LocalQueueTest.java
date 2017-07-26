package com.zhangfb95.localqueue;

import com.zhangfb95.localqueue.logic.bean.InputBean;
import com.zhangfb95.localqueue.logic.core.DefaultLocalQueue;
import com.zhangfb95.localqueue.logic.core.LocalQueue;
import org.junit.Test;

/**
 * @author zhangfb
 */
public class LocalQueueTest {

    @Test
    public void test() throws Exception {
        InputBean inputBean = new InputBean();
        inputBean.setDataFileCapacity(1024);
        inputBean.setStorageDir("/Users/pro/ws/learn/localqueue/src/main/resources");
        LocalQueue localQueue = new DefaultLocalQueue(inputBean);

        try {
            localQueue.init();
            for (int i = 0; i < 2000; i++) {
                System.out.println("id = " + i);
                localQueue.offer(("nimei张付兵-newold" + i).getBytes("utf-8"));
            }

            /*localQueue.offer("nimei张付兵888".getBytes("utf-8"));
            localQueue.offer("nimei张付兵999".getBytes("utf-8"));
            localQueue.offer("nimei张付兵999".getBytes("utf-8"));*/
        } finally {
            localQueue.close();
        }
    }

    @Test
    public void test2() throws Exception {
        InputBean inputBean = new InputBean();
        inputBean.setDataFileCapacity(1024 * 1024);
        inputBean.setStorageDir("/Users/pro/ws/learn/localqueue/src/main/resources");
        LocalQueue localQueue = new DefaultLocalQueue(inputBean);

        try {
            localQueue.init();
            for (int i = 0; i < 200; i++) {
                byte[] data = localQueue.poll();
                if (data != null) {
                    System.out.println(new String(data, "utf-8"));
                } else {
                    System.out.println("end!");
                    break;
                }
            }
            /*System.out.println(new String(localQueue.poll(), "utf-8"));
            System.out.println(new String(localQueue.poll(), "utf-8"));
            System.out.println(new String(localQueue.poll(), "utf-8"));*/
        } finally {
            localQueue.close();
        }
    }
}
