package com.zhangfb95.localqueue;

import com.zhangfb95.localqueue.exception.MappedByteBufferCreateException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author zhangfb
 */
@Slf4j
public class LocalQueue {

    private Lock lock = new ReentrantReadWriteLock().writeLock();
    private RandomAccessFile accessFile = null;
    private static final long position = 0;
    private static final long count = 1024 * 1024 * 200;
    private MappedByteBuffer mappedByteBuffer;
    private int writeIndex = 0;
    private int readIndex = 0;

    public LocalQueue(RandomAccessFile accessFile) {
        this.accessFile = accessFile;
        try {
            mappedByteBuffer = accessFile.getChannel().map(FileChannel.MapMode.READ_WRITE, position, count);
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
            throw new MappedByteBufferCreateException(e.getMessage(), e);
        }
    }

    public boolean offer(byte[] e) {
        lock.lock();
        try {
            mappedByteBuffer.position(writeIndex);
            mappedByteBuffer.putInt(e.length);
            mappedByteBuffer.put(e);
            writeIndex += 4 + e.length;
            return true;
        } finally {
            lock.unlock();
        }
    }

    public byte[] poll() {
        lock.lock();
        try {
            int length = mappedByteBuffer.getInt(readIndex);
            byte[] data = new byte[length];
            for (int i = 0; i < length; i++) {
                data[i] = mappedByteBuffer.get(readIndex + 4 + i);
            }
            readIndex += length + 4;
            return data;
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws Exception {
        ConfigBean configBean = new ConfigBean();
        configBean.setFileName("/Users/pro/ws/learn/localqueue/src/main/resources/abcd.txt");
        RandomAccessFile file =
                new LocalQueueStartup(configBean)
                        .readFile("/Users/pro/ws/learn/localqueue/src/main/resources/abcd.txt");
        LocalQueue localQueue = new LocalQueue(file);
        localQueue.offer("abc".getBytes("utf-8"));
        localQueue.offer("张付兵".getBytes("utf-8"));
        localQueue.offer("黄丽娟".getBytes("utf-8"));

        System.out.println(new String(localQueue.poll(), "utf-8"));
        System.out.println(new String(localQueue.poll(), "utf-8"));
        System.out.println(new String(localQueue.poll(), "utf-8"));
        System.out.println(new String(localQueue.poll(), "utf-8"));
    }
}
