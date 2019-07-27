package com.jamff.handlerframework;

import org.junit.Test;

public class ThreadLocalTest {

    @Test
    public void test() {
        // 创建本地线程（主线程）
        final ThreadLocal<String> threadLocal = new ThreadLocal<String>() {
            @Override
            protected String initialValue() {
                // 重写初始化方法，默认返回null，如果ThreadLocalMap拿不到值再调用初始化方法
                return "JamFF";
            }
        };

        // 从ThreadLocalMap中获取String值，key是主线程
        System.out.println("主线程threadLocal：" + threadLocal.get());

        //--------------------------thread-0
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 从ThreadLocalMap中获取key：thread-0的值？没有，拿不到值再调用初始化方法
                System.out.println("thread-0：" + threadLocal.get()); // JamFF

                // ThreadLocalMap存入：key:thread-0  value:"熊老师"
                threadLocal.set("FF");
                System.out.println("thread-0  set  >>> " + threadLocal.get()); // FF

                // 使用完成建议remove()，避免大量无意义的内存占用
                threadLocal.remove();
            }
        }).start();

        //--------------------------thread-1
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 从ThreadLocalMap中获取key：thread-1的值？没有，拿不到值再调用初始化方法
                System.out.println("thread-1：" + threadLocal.get()); // JamFF

                // ThreadLocalMap存入：key:thread-1  value:"Jam"
                threadLocal.set("Jam");
                System.out.println("thread-1  set  >>> " + threadLocal.get()); // Jam

                // 使用完成建议remove()，避免大量无意义的内存占用
                threadLocal.remove();
            }
        }).start();
    }
}
