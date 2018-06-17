package com.jamff.handler.os;

/**
 * 描述：
 * 作者：JamFF
 * 创建时间：2018/6/10 12:05
 */
public class Looper {

    /**
     * 利用ThreadLocal使用Looper副本，解决多线程并发问题
     */
    private static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<>();

    final MessageQueue mQueue;

    private Looper() {
        mQueue = new MessageQueue();
    }

    public static Looper myLooper() {
        return sThreadLocal.get();
    }

    /**
     * 实例化Looper
     */
    public static void prepare() {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper());
    }

    /**
     * 轮询器，不断的查询MessageQueue中的消息，并分发
     */
    public static void loop() {
        final Looper me = myLooper();
        if (me == null) {
            // loop调用之前需要调用prepare创建Looper
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
        }
        final MessageQueue queue = me.mQueue;

        for (; ; ) {
            Message msg = queue.next(); // might block
            if (msg == null) {
                // No message indicates that the message queue is quitting.
                continue;
            }
            // 分发消息
            msg.target.dispatchMessage(msg);
        }
    }
}
