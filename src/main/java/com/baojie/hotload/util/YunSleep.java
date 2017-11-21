package com.baojie.hotload.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class YunSleep {
    private static final Logger log = LoggerFactory.getLogger(YunSleep.class);
    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    public static final int FAST = (NCPU > 1) ? 1 << 6 : 0;
    public static final int MIDDLE = (NCPU > 1) ? 1 << 10 : 0;
    public static final int SLOW = (NCPU > 1) ? 1 << 16 : 0;

    static {
        log.info("System NCPU=" + NCPU);
        log.info("System FAST=" + FAST);
        log.info("System MIDDLE=" + MIDDLE);
        log.info("System SLOW=" + SLOW);
    }

    private YunSleep() {
        throw new IllegalAccessError("init");
    }

    // 因为是sleep形式调用，会擦除掉中断状态
    public static void threadSleep(final TimeUnit timeUnit, final long sleep) {
        Thread.yield();
        try {
            timeUnit.sleep(sleep);
        } catch (InterruptedException e) {
            log.error("threadSleep occur error=" + e.toString() + ", ignore this, interrupt status has been cleaned");
        }
    }

    // 因为使用的locksupport，这种方式会忽略掉线程中断状态，并且立即返回，所以这个方法添加了一些安全性日志打印
    // 由于是先判断是否被中断，所以没有对状态进行擦除，locksupport也不会擦除
    // 如果在休眠期间检测到中断，能够快速响应
    public static void locksptSleep(final TimeUnit timeUnit, final long sleep) {
        Thread.yield();
        final Thread thread = Thread.currentThread();
        interrupted(thread);
        LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(sleep, timeUnit));
    }

    public static void locksptCleanSleep(final TimeUnit timeUnit, final long sleep) {
        Thread.yield();
        final Thread thread = Thread.currentThread();
        if (interrupted(thread)) {
            Thread.interrupted();
        } else {
            LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(sleep, timeUnit));
        }
    }

    private static boolean interrupted(final Thread thread) {
        final boolean isInterrupted = thread.isInterrupted();
        if (isInterrupted) {
            final String threadName = thread.getName();
            final long threadId = thread.getId();
            log.error("thread interrupted before sleep , threadName=" + threadName + ", threadId=" + threadId);
        }
        return isInterrupted;
    }

}
