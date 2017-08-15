package com.zhangfb95.localqueue.core;

import com.zhangfb95.localqueue.core.logic.feature.DefaultLocalQueue;
import com.zhangfb95.localqueue.core.logic.feature.LocalQueue;
import com.zhangfb95.localqueue.core.logic.bean.Config;
import org.junit.Test;

/**
 * @author zhangfb
 */
public class LocalQueueTest {

    @Test
    public void test() throws Exception {
        Config config = new Config();
        config.setDataFileCapacity(1024);
        config.setStorageDir("/Users/pro/tmp/localqueue");
        LocalQueue localQueue = new DefaultLocalQueue(config);

        try {
            localQueue.init();
            for (int i = 0; i < 2000; i++) {
                String value = "nimei张付兵-newold" + i;
                System.out.println(value);
                localQueue.offer((value).getBytes("utf-8"));
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
        Config config = new Config();
        config.setDataFileCapacity(1024 * 1024);
        config.setStorageDir("/Users/pro/tmp/localqueue");
        LocalQueue localQueue = new DefaultLocalQueue(config);

        try {
            localQueue.init();
            for (int i = 0; i < 2100; i++) {
                byte[] data = localQueue.poll();
                if (data != null) {
                    System.out.println(i + ":" + new String(data, "utf-8"));
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
