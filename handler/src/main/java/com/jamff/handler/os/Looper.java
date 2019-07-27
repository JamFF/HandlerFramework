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
        // 在实例化Looper的时候也创建主线程唯一MessageQueue
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
            // 主线程只能有唯一一个Looper对象，在ActivityThread中进行创建，如果开发者在主线程中调用会抛出异常
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
        // 从Looper中获取主线程唯一MessageQueue对象
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
