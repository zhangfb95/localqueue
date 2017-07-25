package com.zhangfb95.localqueue.logic.core;

import com.zhangfb95.localqueue.logic.bean.InputBean;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author zhangfb
 */
public class DefaultLocalQueue implements LocalQueue {

    private static final String IDX_FILE_NAME = "localqueue_idx.db";
    private IdxFileFacade idxFileFacade;
    private Lock lock = new ReentrantReadWriteLock().writeLock();

    @Getter
    @Setter
    private InputBean inputBean;
    private Context context;

    public void init() {
        context = new Context();
        context.setInputBean(inputBean);
        String idxFilePath = inputBean.getStorageDir() + File.separator + IDX_FILE_NAME;
        idxFileFacade = new IdxFileFacade(idxFilePath);
        context.setIdxBean(new Initializer().loadIdxBean(inputBean, idxFileFacade));
    }

    @Override public boolean offer(byte[] e) {
        return false;
    }

    @Override public byte[] poll() {
        return new byte[0];
    }

    @Override public void close() {

    }
}
