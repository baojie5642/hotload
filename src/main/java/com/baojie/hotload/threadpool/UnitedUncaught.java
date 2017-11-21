package com.baojie.hotload.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Thread.UncaughtExceptionHandler;

public class UnitedUncaught implements UncaughtExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(UnitedUncaught.class);
	private static volatile UnitedUncaught Instance;

	public UnitedUncaught() {

	}

	public static UnitedUncaught getInstance() {
		if (null != Instance) {
			return Instance;
		} else {
			synchronized (UnitedUncaught.class) {
				if (null == Instance) {
					Instance = new UnitedUncaught();
				}
				return Instance;
			}
		}
	}

	@Override
	public void uncaughtException(final Thread t, final Throwable e) {
		if (null == t) {
			log.error("thread must not be null");
			return;
		}
		final String threadName = Thread.currentThread().getName();
		firstInterrupt(t);
		if (null == e) {
			log.error("throwable must not be null, threadName=" + threadName);
			return;
		}
		log.error("threadName=" + threadName + ", uncaughtException=" + e.toString());
		log.error(e.toString(),e);
	}

	private void firstInterrupt(final Thread t) {
		try {
			t.interrupt();
		} finally {
			alwaysInterrupt(t);
		}
	}

	private void alwaysInterrupt(final Thread t) {
		if (!t.isInterrupted()) {
			t.interrupt();
		}
		if (t.isAlive()) {
			t.interrupt();
		}
	}
}
