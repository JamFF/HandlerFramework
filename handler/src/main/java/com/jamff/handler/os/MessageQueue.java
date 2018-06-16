package com.jamff.handler.os;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * 描述：
 * 作者：JamFF
 * 创建时间：2018/6/10 12:05
 */
public class MessageQueue {

    private static final int COUNT = 50;

    private ArrayBlockingQueue<Message> mQueue;

    public MessageQueue() {
        mQueue = new ArrayBlockingQueue<>(COUNT);
    }

    /**
     * 往消息队列添加消息
     * 仓库满的时候，不能再生产，block
     * 当生产了一个消息，通知消费者，可以继续消费
     *
     * @param msg {@link Message}
     */
    public void enqueueMessage(Message msg) {
        try {
            mQueue.put(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
