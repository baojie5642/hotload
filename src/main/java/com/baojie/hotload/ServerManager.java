package com.baojie.hotload;

import com.baojie.hotload.service.manager.RoamConfManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class ServerManager {

    private static final Logger log = LoggerFactory.getLogger(ServerManager.class);
    private static final AtomicBoolean INIT = new AtomicBoolean(false);
    private static volatile ServerManager manager;
    private volatile AbstractApplicationContext applicationContext = null;
    private volatile Notification notification;

    public void registerNotification(Notification notification) {
        this.notification = notification;
    }

    private ServerManager() {
    }

    public static ServerManager getInstance() {
        if (null != manager) {
            return manager;
        }
        synchronized (ServerManager.class) {
            if (null == manager) {
                manager = new ServerManager();
            }
            return manager;
        }
    }

    public void startServer() {
        if (INIT.get()) {
            log.warn("Roam Server already started");
            return;
        } else {
            if (INIT.compareAndSet(false, true)) {
                log.info("********* start roam server *********");
                new Thread("hotload") {
                    public void run() {
                        for (; ; ) {
                            log.info("RoamConfManager getBizThreadNum=" + RoamConfManager.getBizThreadNum());
                            log.info("RoamConfManager getBaojie_0_0=" + RoamConfManager.getBaojie_0_0());
                            log.info("RoamConfManager getBaojie_0_1=" + RoamConfManager.getBaojie_0_1());
                            LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(6, TimeUnit.SECONDS));
                        }
                    }
                }.start();
                new Thread("sms-server-0") {
                    public void run() {
                        applicationContext = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
                        log.info("********* roam server starts successfully *********");
                    }
                }.start();
            } else {
                log.warn("other thread running");
            }
        }
    }

    public synchronized void stop() {
        if (this.applicationContext != null) {
            applicationContext.close();
        }
    }

    public boolean isStarted() {
        return applicationContext != null;
    }

    private void addHook() {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private void destory() {
        if (applicationContext == null) {
            return;
        }
        applicationContext.close();
        applicationContext = null;
    }

    private void removeHook() {
        if (this.shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
            } catch (IllegalStateException ex) {
                // ignore - VM is already shutting down
            }
        }
    }

    private Thread shutdownHook = new Thread() {
        public void run() {
            try {
                destory();
                removeHook();
                if (notification != null) {
                    notification.showdown();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public static interface Notification {
        void showdown();
    }
}
