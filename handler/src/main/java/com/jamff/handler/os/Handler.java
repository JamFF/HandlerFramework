package com.jamff.handler.os;

/**
 * 描述：
 * 作者：JamFF
 * 创建时间：2018/6/10 12:04
 */
public class Handler {

    private static final String TAG = "JamFF ";

    private final Looper mLooper;
    private final MessageQueue mQueue;

    public Handler() {

        // 得到Looper实例
        mLooper = Looper.myLooper();
        if (mLooper == null) {
            throw new RuntimeException(
                    "Can't create handler inside thread that has not called Looper.prepare()");
        }
        // 得到MessageQueue实例
        mQueue = mLooper.mQueue;
    }

    public final boolean sendMessage(Message msg) {
        return sendMessageDelayed(msg);
    }

    public final boolean sendEmptyMessage(int what) {
        return sendEmptyMessageDelayed(what);
    }

    public final boolean sendEmptyMessageDelayed(int what) {
        Message msg = new Message();
        msg.what = what;
        return sendMessageDelayed(msg);
    }

    public final boolean post(Runnable r) {
        return sendMessageDelayed(getPostMessage(r));
    }

    public final boolean sendMessageDelayed(Message msg) {
        return sendMessageAtTime(msg);
    }

    public boolean sendMessageAtTime(Message msg) {
        MessageQueue queue = mQueue;
        if (queue == null) {
            RuntimeException e = new RuntimeException(
                    this + " sendMessageAtTime() called with no mQueue");

            System.out.println(TAG + e.getMessage());
            return false;
        }
        return enqueueMessage(queue, msg);
    }

    /**
     * 消息入队
     */
    private boolean enqueueMessage(MessageQueue queue, Message msg) {
        // 注意，将handler赋值为target，让msg中持有Handler
        // 目的是在Looper.loop()中通过msg调用Handler.dispatchMessage分发的消息
        msg.target = this;
        return queue.enqueueMessage(msg);
    }

    /**
     * 调用者重写该方法
     */
    public void handleMessage(Message msg) {

    }

    /**
     * 分发消息
     */
    public void dispatchMessage(Message msg) {
        if (msg.callback != null) {
            // post方式
            handleCallback(msg);
        } else {
            // sendMessage方式
            handleMessage(msg);
        }
    }

    private static Message getPostMessage(Runnable r) {
        Message m = new Message();
        m.callback = r;
        return m;
    }

    private static void handleCallback(Message message) {
        message.callback.run();
    }
}
