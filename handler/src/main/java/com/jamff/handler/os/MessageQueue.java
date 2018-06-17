package com.jamff.handler.os;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 描述：
 * 作者：JamFF
 * 创建时间：2018/6/10 12:05
 */
public class MessageQueue {

    private static final int COUNT = 50;

    private Message[] items;

    // 消息队列的大小
    private int mCount;

    // 入队的索引
    private int mPutIndex;

    // 出队的索引
    private int mTakeIndex;

    // 互斥锁
    private Lock mLock;

    // 条件变量，队列没有空
    private final Condition notEmpty;
    /**
     * 条件变量，队列没有满
     */
    private final Condition notFull;

    MessageQueue() {
        this.items = new Message[COUNT];   //发消息、取消息都会阻塞
        this.mLock = new ReentrantLock();
        this.notEmpty = mLock.newCondition();
        this.notFull = mLock.newCondition();
    }


    /**
     * 消息入队，在子线程中执行，生产者
     * 需要考虑：生产过快，供大于求，会覆盖
     */
    public boolean enqueueMessage(Message msg) {
        // 生产者队列已满，停止生产，等待子线程消费
        try {
            mLock.lock();
            while (mCount == items.length) {
                // 队列已满，阻塞生产
                notFull.await();//等待，代码不会向下执行
            }
            items[mPutIndex] = msg;
            mPutIndex = (++mPutIndex == items.length) ? 0 : mPutIndex;// 判断到头就从0开始
            mCount++;
            // 队列不再为空，有产品可以消费
            notEmpty.signalAll();// 全部唤醒消费者
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            mLock.unlock();
        }
        return true;
    }

    /**
     * 消息出队，在主线程中执行，消费者
     * 需要考虑：消费过快，供不应求，会取空
     */
    public Message next() {
        // 消费者把队列掏空，等待生产者线程（主线程）生产
        Message msg = null;
        try {
            mLock.lock();
            while (mCount == 0) {
                notEmpty.await();// 等待，代码不会向下执行
            }
            msg = items[mTakeIndex];
            items[mTakeIndex] = null;// 置空，GC回收
            mTakeIndex = (++mTakeIndex == items.length) ? 0 : mTakeIndex;// 判断到头就从0开始
            mCount--;
            // 消费了一个产品，队列不满，通知生产者线程生产
            notFull.signalAll();// 全部唤醒生产者
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mLock.unlock();
        }
        return msg;
    }
}
