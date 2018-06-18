package com.jamff.handler;

import com.jamff.handler.os.Handler;
import com.jamff.handler.os.Looper;
import com.jamff.handler.os.Message;

import java.util.UUID;

/**
 * 描述：测试类
 * 作者：JamFF
 * 创建时间：2018/6/17 18:30
 */
public class ActivityThread {

    private static final String TAG = "JamFF ";
    private static final int HANDLER_WHAT = 1;
    private static final int HANDLER_EMPTY_WHAT = 2;

    public static void main(String[] args) {

        Looper.prepare();

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == HANDLER_WHAT) {
                    System.out.println(TAG + Thread.currentThread() + " msg.obj = " + msg.obj);
                } else if (msg.what == HANDLER_EMPTY_WHAT) {
                    System.out.println(TAG + Thread.currentThread() + " msg.what = " + msg.what);
                }
            }
        };

        for (int i = 0; i < 10; i++) {
            // 开启10个线程
            new Thread(new Runnable() {
                @Override
                public void run() {

                    // post方式
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println(TAG + Thread.currentThread() + " handler.post");
                        }
                    });

                    // sendEmptyMessage方式
                    handler.sendEmptyMessage(HANDLER_EMPTY_WHAT);

                    while (true) {
                        Message msg = new Message();
                        synchronized (UUID.class) {
                            // randomUUID静态方法，加锁保证线程安全
                            msg.obj = UUID.randomUUID().toString();
                        }
                        msg.what = HANDLER_WHAT;
                        // sendMessage方式
                        handler.sendMessage(msg);
                        System.out.println(TAG + Thread.currentThread() + " msg.obj = " + msg.obj);

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }

        // 开启轮询
        Looper.loop();
    }
}
