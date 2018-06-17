# HandlerFramework
Handler源码分析及手写实现

#### 源码分析

###### Handler源码

1. 所有的sendMessage最终都会调用到sendMessageAtTime

    ```java
    public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
        MessageQueue queue = mQueue;
        if (queue == null) {
            RuntimeException e = new RuntimeException(
                    this + " sendMessageAtTime() called with no mQueue");
            Log.w("Looper", e.getMessage(), e);
            return false;
        }
        return enqueueMessage(queue, msg, uptimeMillis);
    }
    ```

2. 最后一行关键代码，使用到了queue

    ```java
    private boolean enqueueMessage(MessageQueue queue, Message msg, long uptimeMillis) {
        // 注意，将handler赋值为target，让msg中持有Handler，方便在Looper.loop()中通过msg调用Handler.dispatchMessage分发的消息
        msg.target = this;
        if (mAsynchronous) {
            msg.setAsynchronous(true);
        }
        return queue.enqueueMessage(msg, uptimeMillis);
    }
    ```

3. 追根溯源，queue是在Handler构造方法中拿到Looper中的mQueue，并且发现了一个mLooper实例

    ```java
    public Handler(Callback callback, boolean async) {
        if (FIND_POTENTIAL_LEAKS) {
            final Class<? extends Handler> klass = getClass();
            if ((klass.isAnonymousClass() || klass.isMemberClass() || klass.isLocalClass()) &&
                    (klass.getModifiers() & Modifier.STATIC) == 0) {
                Log.w(TAG, "The following Handler class should be static or leaks might occur: " +
                    klass.getCanonicalName());
            }
        }
        // 得到Looper实例
        mLooper = Looper.myLooper();
        if (mLooper == null) {
            throw new RuntimeException(
                "Can't create handler inside thread that has not called Looper.prepare()");
        }
        // 得到MessageQueue实例
        mQueue = mLooper.mQueue;
        mCallback = callback;
        mAsynchronous = async;
    }
    ```

###### Looper源码

1. 在Looper构造方法中创建了mQueue

    ```java
    private Looper(boolean quitAllowed) {
        mQueue = new MessageQueue(quitAllowed);
        mThread = Thread.currentThread();
    }
    ```

2. 通过myLooper()得到Looper实例

    ```java
    /**
     * Return the Looper object associated with the current thread.  Returns
     * null if the calling thread is not associated with a Looper.
     */
    public static @Nullable Looper myLooper() {
        return sThreadLocal.get();
    }
    ```

3. 为了保证在使用Looper时线程安全，用*ThreadLocal*包装一下，操作的是Looper的副本，所以上面的myLooper()返回的是一个Looper的副本

    ```java
    // sThreadLocal.get() will return null unless you've called prepare().
    static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<Looper>();
    ```
    
4. *ThreadLocal*内部有一个Map (内部类ThreadLocalMap)，key是线程ID，value是传入的对象

5. 找到sThreadLocal赋值的地方

    ```java
    private static void prepare(boolean quitAllowed) {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper(quitAllowed));
    }
    ```

###### ActivityThread源码

prepare是在ActivityThread中main中调用的
```java
public static void main(String[] args) {
    Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "ActivityThreadMain");
    SamplingProfilerIntegration.start();

    // CloseGuard defaults to true and can be quite spammy.  We
    // disable it here, but selectively enable it later (via
    // StrictMode) on debug builds, but using DropBox, not logs.
    CloseGuard.setEnabled(false);

    Environment.initForCurrentUser();

    // Set the reporter for event logging in libcore
    EventLogger.setReporter(new EventLoggingReporter());

    // Make sure TrustedCertificateStore looks in the right place for CA certificates
    final File configDir = Environment.getUserConfigDirectory(UserHandle.myUserId());
    TrustedCertificateStore.setDefaultUserDirectory(configDir);

    Process.setArgV0("<pre-initialized>");
    
    // 为主线程创建Looper
    Looper.prepareMainLooper();

    // 实例化ActivityThread
    ActivityThread thread = new ActivityThread();
    thread.attach(false);

    if (sMainThreadHandler == null) {
        // 保存了主线程的Handler
        sMainThreadHandler = thread.getHandler();
    }

    if (false) {
        Looper.myLooper().setMessageLogging(new
                LogPrinter(Log.DEBUG, "ActivityThread"));
    }

    // End of event ActivityThreadMain.
    Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
    // 开启轮询
    Looper.loop();

    throw new RuntimeException("Main thread loop unexpectedly exited");
}
```

###### Looper源码

1. ActivityThread调用了Looper.prepareMainLooper();

    ```java
    /**
     * Initialize the current thread as a looper, marking it as an
     * application's main looper. The main looper for your application
     * is created by the Android environment, so you should never need
     * to call this function yourself.  See also: {@link #prepare()}
     */
    public static void prepareMainLooper() {
        // 这里面实例化了Looper
        prepare(false);
        synchronized (Looper.class) {
            if (sMainLooper != null) {
                throw new IllegalStateException("The main Looper has already been prepared.");
            }
            sMainLooper = myLooper();
        }
    }
    ```

2. ActivityThread调用了Looper.loop();

    ```java
    /**
     * Run the message queue in this thread. Be sure to call
     * {@link #quit()} to end the loop.
     */
    public static void loop() {
        final Looper me = myLooper();
        if (me == null) {
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
        }
        final MessageQueue queue = me.mQueue;
    
        // Make sure the identity of this thread is that of the local process,
        // and keep track of what that identity token actually is.
        Binder.clearCallingIdentity();
        final long ident = Binder.clearCallingIdentity();
    
        for (;;) {
            // 出队
            Message msg = queue.next(); // might block
            if (msg == null) {
                // No message indicates that the message queue is quitting.
                return;
            }
    
            // This must be in a local variable, in case a UI event sets the logger
            final Printer logging = me.mLogging;
            if (logging != null) {
                logging.println(">>>>> Dispatching to " + msg.target + " " +
                        msg.callback + ": " + msg.what);
            }
    
            final long slowDispatchThresholdMs = me.mSlowDispatchThresholdMs;
    
            final long traceTag = me.mTraceTag;
            if (traceTag != 0 && Trace.isTagEnabled(traceTag)) {
                Trace.traceBegin(traceTag, msg.target.getTraceName(msg));
            }
            final long start = (slowDispatchThresholdMs == 0) ? 0 : SystemClock.uptimeMillis();
            final long end;
            try {
                // 分发消息，msg.target就是handler
                msg.target.dispatchMessage(msg);
                end = (slowDispatchThresholdMs == 0) ? 0 : SystemClock.uptimeMillis();
            } finally {
                if (traceTag != 0) {
                    Trace.traceEnd(traceTag);
                }
            }
            if (slowDispatchThresholdMs > 0) {
                final long time = end - start;
                if (time > slowDispatchThresholdMs) {
                    Slog.w(TAG, "Dispatch took " + time + "ms on "
                            + Thread.currentThread().getName() + ", h=" +
                            msg.target + " cb=" + msg.callback + " msg=" + msg.what);
                }
            }
    
            if (logging != null) {
                logging.println("<<<<< Finished to " + msg.target + " " + msg.callback);
            }
    
            // Make sure that during the course of dispatching the
            // identity of the thread wasn't corrupted.
            final long newIdent = Binder.clearCallingIdentity();
            if (ident != newIdent) {
                Log.wtf(TAG, "Thread identity changed from 0x"
                        + Long.toHexString(ident) + " to 0x"
                        + Long.toHexString(newIdent) + " while dispatching to "
                        + msg.target.getClass().getName() + " "
                        + msg.callback + " what=" + msg.what);
            }
    
            msg.recycleUnchecked();
        }
    }
    ```

3. 上面关键的两行代码

    ```java
    // 出队
    Message msg = queue.next(); // might block
    ```
    
    ```java
    // 分发消息，msg.target就是handler
    msg.target.dispatchMessage(msg);
    ```

###### Handler源码

1. 分发消息，两种方式，一种是post传入的Runnable，一种是sendMessage

    ```java
    /**
     * Handle system messages here.
     */
    public void dispatchMessage(Message msg) {
        if (msg.callback != null) {
            handleCallback(msg);
        } else {
            if (mCallback != null) {
                if (mCallback.handleMessage(msg)) {
                    return;
                }
            }
            handleMessage(msg);
        }
    }
    ```

2. sendMessage方式，这就是new Handler时重写的方法

    ```java
    /**
     * Subclasses must implement this to receive messages.
     */
    public void handleMessage(Message msg) {
        // 空实现，交给创建者重写该方法
    }
    ```

###### MessageQueue源码

1. 清楚了Looper，再回到queue.enqueueMessage(msg, uptimeMillis)，入队

    ```java
    boolean enqueueMessage(Message msg, long when) {
        if (msg.target == null) {
            throw new IllegalArgumentException("Message must have a target.");
        }
        if (msg.isInUse()) {
            throw new IllegalStateException(msg + " This message is already in use.");
        }
    
        synchronized (this) {
            if (mQuitting) {
                IllegalStateException e = new IllegalStateException(
                        msg.target + " sending message to a Handler on a dead thread");
                Log.w(TAG, e.getMessage(), e);
                msg.recycle();
                return false;
            }
            
            // 把消息编辑为正在使用
            msg.markInUse();
            // 记录执行的时间
            msg.when = when;
            // 将当前mMessages赋值给p
            Message p = mMessages;
            boolean needWake;
            if (p == null || when == 0 || when < p.when) {
                // New head, wake up the event queue if blocked.
                // 当前p为null，或者msg.when为0，或者msg要早于p执行时
                // 将p放到下一个执行
                msg.next = p;// 看到msg.next，我们知道Messages是链表结构，搜索不到pre说明是单向链表，使用链表可以处理好顺序问题
                // 将msg赋值给当前mMessages
                mMessages = msg;
                needWake = mBlocked;
            } else {
                // Inserted within the middle of the queue. Usually we don't have to wake
                // up the event queue unless there is a barrier at the head of the queue
                // and the message is the earliest asynchronous message in the queue.
                needWake = mBlocked && p.target == null && msg.isAsynchronous();
                Message prev;
                // 循环找到msg应该插入的位置
                for (;;) {
                    prev = p;
                    // 取下一个Messages
                    p = p.next;
                    if (p == null || when < p.when) {
                        // 如果没有下一个，或者msg要早于该Messages下一个执行时，跳出循环
                        break;
                    }
                    if (needWake && p.isAsynchronous()) {
                        needWake = false;
                    }
                }
                // 将在msg后面执行的Messages挂在msg后面
                msg.next = p; // invariant: p == prev.next
                // 将在msg挂在需要在其执行的Messages后面
                prev.next = msg;
            }
    
            // We can assume mPtr != 0 because mQuitting is false.
            if (needWake) {
                // 判断是否需要唤醒，通过native方法唤醒
                nativeWake(mPtr);
            }
        }
        return true;
    }
    ```
    
2. 出队

    ```java
    Message next() {
        // Return here if the message loop has already quit and been disposed.
        // This can happen if the application tries to restart a looper after quit
        // which is not supported.
        final long ptr = mPtr;
        if (ptr == 0) {
            return null;
        }
    
        int pendingIdleHandlerCount = -1; // -1 only during first iteration
        int nextPollTimeoutMillis = 0;
        for (;;) {
            if (nextPollTimeoutMillis != 0) {
                Binder.flushPendingCommands();
            }
    
            nativePollOnce(ptr, nextPollTimeoutMillis);
    
            synchronized (this) {
                // Try to retrieve the next message.  Return if found.
                final long now = SystemClock.uptimeMillis();
                Message prevMsg = null;
                Message msg = mMessages;
                if (msg != null && msg.target == null) {
                    // Stalled by a barrier.  Find the next asynchronous message in the queue.
                    do {
                        prevMsg = msg;
                        msg = msg.next;
                    } while (msg != null && !msg.isAsynchronous());
                }
                if (msg != null) {
                    if (now < msg.when) {
                        // Next message is not ready.  Set a timeout to wake up when it is ready.
                        nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);
                    } else {
                        // Got a message.
                        mBlocked = false;
                        if (prevMsg != null) {
                            prevMsg.next = msg.next;
                        } else {
                            mMessages = msg.next;
                        }
                        msg.next = null;
                        if (DEBUG) Log.v(TAG, "Returning message: " + msg);
                        msg.markInUse();
                        return msg;
                    }
                } else {
                    // No more messages.
                    nextPollTimeoutMillis = -1;
                }
    
                // Process the quit message now that all pending messages have been handled.
                if (mQuitting) {
                    dispose();
                    return null;
                }
    
                // If first time idle, then get the number of idlers to run.
                // Idle handles only run if the queue is empty or if the first message
                // in the queue (possibly a barrier) is due to be handled in the future.
                if (pendingIdleHandlerCount < 0
                        && (mMessages == null || now < mMessages.when)) {
                    pendingIdleHandlerCount = mIdleHandlers.size();
                }
                if (pendingIdleHandlerCount <= 0) {
                    // No idle handlers to run.  Loop and wait some more.
                    mBlocked = true;
                    continue;
                }
    
                if (mPendingIdleHandlers == null) {
                    mPendingIdleHandlers = new IdleHandler[Math.max(pendingIdleHandlerCount, 4)];
                }
                mPendingIdleHandlers = mIdleHandlers.toArray(mPendingIdleHandlers);
            }
    
            // Run the idle handlers.
            // We only ever reach this code block during the first iteration.
            for (int i = 0; i < pendingIdleHandlerCount; i++) {
                final IdleHandler idler = mPendingIdleHandlers[i];
                mPendingIdleHandlers[i] = null; // release the reference to the handler
    
                boolean keep = false;
                try {
                    keep = idler.queueIdle();
                } catch (Throwable t) {
                    Log.wtf(TAG, "IdleHandler threw exception", t);
                }
    
                if (!keep) {
                    synchronized (this) {
                        mIdleHandlers.remove(idler);
                    }
                }
            }
    
            // Reset the idle handler count to 0 so we do not run them again.
            pendingIdleHandlerCount = 0;
    
            // While calling an idle handler, a new message could have been delivered
            // so go back and look again for a pending message without waiting.
            nextPollTimeoutMillis = 0;
        }
    }
    ```

#### 整理思路

![alt text](https://github.com/JamFF/HandlerFramework/blob/master/art/handler.png)

1. 首先是在程序启动时调用了ActivityThread的main方法
2. main方法通过Looper.prepareMainLooper()，调用prepare(boolean quitAllowed)创建了Looper实例
3. 在Looper的构造方法中创建了MessageQueue的实例mQueue
4. prepare(boolean quitAllowed)，保存Looper副本在sThreadLocal中
5. 在ActivityThread的main方法的最后开启轮询Looper.loop()
6. 在Looper.myLooper()中返回了sThreadLocal的Looper副本
7. 创建Handler时，取到了Looper副本mLooper，以及mQueue
8. 调用Handler.sendMessage时，调用了MessageQueue的enqueueMessage，将Message加入队列
9. Looper.loop()中不断轮询，通过Handler的dispatchMessage分发消息