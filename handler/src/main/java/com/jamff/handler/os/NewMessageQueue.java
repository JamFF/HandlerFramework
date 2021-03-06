package com.jamff.handler.os;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * 描述：使用Java自带API，完成生产消费模式
 * 作者：JamFF
 * 创建时间：2018/6/17 19:07
 */
public class NewMessageQueue {

    private static final int COUNT = 50;

    // 阻塞式的队列
    private ArrayBlockingQueue<Message> mQueue;

    NewMessageQueue() {
        mQueue = new ArrayBlockingQueue<>(COUNT);
    }

    /**
     * 往消息队列添加消息
     * 仓库满的时候，不能再生产，block
     * 当生产了一个消息，通知消费者，可以继续消费
     *
     * @param msg {@link Message}
     */
    public boolean enqueueMessage(Message msg) {
        try {
            mQueue.put(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 从消息队列取消息
     * 当仓库空的时候，不能再消费，block
     * 当消费了一个消息，通知生产者，可以继续生产
     *
     * @return {@link Message}
     */
    public Message next() {
        try {
            return mQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
