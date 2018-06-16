# HandlerFramework
Handler源码分析及手写实现

所有的sendMessage最终都会调用到

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

找到关键的最后一行代码
```java
private boolean enqueueMessage(MessageQueue queue, Message msg, long uptimeMillis) {
    // 将handler赋值为target
    msg.target = this;
    if (mAsynchronous) {
        msg.setAsynchronous(true);
    }
    return queue.enqueueMessage(msg, uptimeMillis);
}

```

这里使用到了queue，追根溯源，是在Handler构造方法中赋值

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

    mLooper = Looper.myLooper();
    if (mLooper == null) {
        throw new RuntimeException(
            "Can't create handler inside thread that has not called Looper.prepare()");
    }
    mQueue = mLooper.mQueue;
    mCallback = callback;
    mAsynchronous = async;
}
```

一个新的对象*Looper*


```java
/**
 * Return the Looper object associated with the current thread.  Returns
 * null if the calling thread is not associated with a Looper.
 */
public static @Nullable Looper myLooper() {
    return sThreadLocal.get();
}
```

查看下*ThreadLocal*
为了保证在使用Looper时线程安全，用ThreadLocal包装一下，操作的是Looper的副本
所以上面的myLooper返回的是一个Looper的副本

```java
// sThreadLocal.get() will return null unless you've called prepare().
static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<Looper>();
```

找到sThreadLocal赋值的地方

```java
private static void prepare(boolean quitAllowed) {
    if (sThreadLocal.get() != null) {
        throw new RuntimeException("Only one Looper may be created per thread");
    }
    sThreadLocal.set(new Looper(quitAllowed));
}
```

prepare是在ActivityThread中调用的
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

    Looper.prepareMainLooper();

    ActivityThread thread = new ActivityThread();
    thread.attach(false);

    if (sMainThreadHandler == null) {
        sMainThreadHandler = thread.getHandler();
    }

    if (false) {
        Looper.myLooper().setMessageLogging(new
                LogPrinter(Log.DEBUG, "ActivityThread"));
    }

    // End of event ActivityThreadMain.
    Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
    Looper.loop();

    throw new RuntimeException("Main thread loop unexpectedly exited");
}
```

调用了Looper的prepareMainLooper方法，在这里调用的prepare(boolean quitAllowed)方法
```java
/**
 * Initialize the current thread as a looper, marking it as an
 * application's main looper. The main looper for your application
 * is created by the Android environment, so you should never need
 * to call this function yourself.  See also: {@link #prepare()}
 */
public static void prepareMainLooper() {
    prepare(false);
    synchronized (Looper.class) {
        if (sMainLooper != null) {
            throw new IllegalStateException("The main Looper has already been prepared.");
        }
        sMainLooper = myLooper();
    }
}
```

在main函数的最后还调用了Looper.loop();

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

上面的分发代码
```java
msg.target.dispatchMessage(msg);
```

msg.target就是handler

```java
/**
 * Handle system messages here.
 */
public void dispatchMessage(Message msg) {
    if (msg.callback != null) {
        // 使用Handler的post方法时传入的Runnable
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

```java
/**
 * Subclasses must implement this to receive messages.
 */
public void handleMessage(Message msg) {
    // 空实现，交给创建者重写该方法
}
```


清楚了Looper，再回到mQueue赋值的地方
```java
private Looper(boolean quitAllowed) {
    mQueue = new MessageQueue(quitAllowed);
    mThread = Thread.currentThread();
}
```

进入*MessageQueue*

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

整理下逻辑
1. 首先是在程序启动时使用hook调用了ActivityThread的main函数
2. main函数通过Looper.prepareMainLooper()，调用prepare(boolean quitAllowed)创建了Looper
3. 在Looper的构造方法中创建了MessageQueue的实例mQueue
4. prepare(boolean quitAllowed)，保存Looper副本在sThreadLocal中
5. 在Looper.myLooper()中返回了sThreadLocal的Looper副本
6. 创建Handler时，取到了Looper副本mLooper，以及mQueue
7. 调用Handler.sendMessage时，调用了MessageQueue的enqueueMessage，将Message加入队列

