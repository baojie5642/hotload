package com.baojie.hotload.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UnitedThreadGroup {

	private static final Logger log = LoggerFactory.getLogger(UnitedThreadGroup.class);

	private UnitedThreadGroup() {

	}

	public static final ThreadGroup getGroup() {
		ThreadGroup threadGroup = null;
		final SecurityManager sm = System.getSecurityManager();
		if (null != sm) {
			threadGroup = sm.getThreadGroup();
		} else {
			log.warn("SecurityManager can be null when get ThreadGroup, ignore this");
			threadGroup = Thread.currentThread().getThreadGroup();
		}
		if (null == threadGroup) {
			log.error("ThreadGroup get from Main(JVM) must not be null");
			throw new NullPointerException("ThreadGroup get from Main(JVM) must not be null");
		}
		return threadGroup;
	}

}
