package com.baojie.hotload.worker.filewatcher;

import com.baojie.hotload.service.manager.PropertiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HotDog implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(HotDog.class);
    private final AtomicBoolean stop;
    private final WatchService service;
    private final Path path;
    private final List<Resource> resources;

    public static HotDog create(final Path path, final List<Resource> resources, final AtomicBoolean stop) {
        if (null == path) {
            throw new IllegalStateException("path");
        }
        if (null == resources) {
            throw new IllegalStateException("resources");
        }
        return new HotDog(path, resources, stop);
    }

    private HotDog(final Path path, final List<Resource> resources, final AtomicBoolean stop) {
        this.path = path;
        this.resources = resources;
        this.stop = stop;
        this.service = getWatchService();
    }

    private WatchService getWatchService() {
        try {
            return FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            log.error(e.toString() + ", path=" + path, e);
            return null;
        }
    }

    @Override
    public void run() {
        if (null == service) {
            log.error("WatchService null, path=" + path);
            return;
        }
        boolean r = register();
        if (r == false) {
            closeService();
            log.error("hot watcher register fail, path=" + path);
            return;
        }
        try {
            hotWatcher();
        } catch (Throwable t) {
            closeService();
            log.error("hot load exit, err=" + t.toString() + ", path=" + path, t);
        } finally {
            closeService();
            log.info("hot load properties exit, path=" + path);
        }
    }

    private boolean register() {
        // 可以注册其他监视类型
        try {
            //path.register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
            //       StandardWatchEventKinds.ENTRY_DELETE);
            path.register(service, StandardWatchEventKinds.ENTRY_MODIFY);
            return true;
        } catch (IOException e) {
            log.error(e.toString(), e);
            return false;
        } catch (Throwable t) {
            log.error(t.toString(), t);
            return false;
        }
    }

    private void hotWatcher() {
        WatchKey key = null;
        for (; ; ) {
            if (stop.get()) {
                log.info("hot load properties exit, path=" + path);
                return;
            }
            key = getKey();
            if (null == key) {
                if (isStop()) {
                    return;
                }
                continue;
            }
            if (!key.isValid()) {
                closeService();
                log.error("WatchKey isValid==false, path=" + path);
                return;
            }
            dealRightKey(key);
            if (!key.reset()) {
                closeService();
                log.error("key.reset==false, stop hot load, path=" + path);
                return;
            }
        }
    }

    private WatchKey getKey() {
        try {
            return service.poll(60, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            printErr(e);
            return null;
        } catch (Throwable t) {
            printErr(t);
            return null;
        }
    }

    private void printErr(final Throwable t) {
        if (null == t) {
            return;
        }
        if (stop.get()) {
            log.error(t.toString());
        } else {
            log.error(t.toString(), t);
        }
    }

    private boolean isStop() {
        if (stop.get()) {
            closeService();
            log.info("hot load properties exit, path=" + path);
            return true;
        }
        return false;
    }

    private void dealRightKey(final WatchKey key) {
        List<WatchEvent<?>> listKey = key.pollEvents();
        if (null == listKey) {
            log.error("key.pollEvents null, path=" + path);
            return;
        }
        if (listKey.isEmpty()) {
            return;
        }
        for (WatchEvent<?> watchEvent : listKey) {
            Kind<?> kind = watchEvent.kind();
            if (!getKind(kind)) {
                continue;
            }
            if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                log.info("kind=" + StandardWatchEventKinds.ENTRY_MODIFY + ", path=" + path);
                reload(watchEvent);
            }
        }
    }

    private boolean getKind(final Kind<?> kind) {
        if (null == kind) {
            log.error("kind null, path=" + path);
            return false;
        }
        if (kind == StandardWatchEventKinds.OVERFLOW) {
            log.debug("kind=" + StandardWatchEventKinds.OVERFLOW + ", path=" + path);
            return false;
        }
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            log.debug("kind=" + StandardWatchEventKinds.ENTRY_CREATE + ", path=" + path);
            return false;
        }
        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            log.debug("kind=" + StandardWatchEventKinds.OVERFLOW + ", path=" + path);
            return false;
        }
        return true;
    }

    private void reload(final WatchEvent<?> watchEvent) {
        final WatchEvent<Path> watchEventPath = (WatchEvent<Path>) watchEvent;
        final Path p = watchEventPath.context();
        URI uri = p.toUri();
        File f = shift2File(uri);
        if (null == f) {
            log.error("uri maybe enable, uri=" + uri + ", get from watchEvent path=" + p + ", spring path=" + path);
            sendAllResource();
            return;
        }
        String fn = f.getName();
        if (null == fn) {
            sendAllResource();
            return;
        }
        log.debug("fileName=" + fn + ", watchEvent path=" + p + ", spring path=" + path);
        if (fn.contains(".goutputstream")) {
            // 在ubuntu和centos虚拟机中名称是异常的，并且，在这些系统中需要点击两次保存才能使变量的替换生效
            // 但是在服务器上是可以的，都比较正常,这个是平台原因导致的，或者是虚拟机的原因，毕竟是java的nioPath
            log.warn("maybe Ubuntu or VisualCentOS, watchEvent path=" + p + ", spring path=" + path);
            sendAllResource();
            return;
        }
        if (!fn.contains(".properties")) {
            sendAllResource();
            return;
        }
        sendSpecialResource(fn);
    }

    private File shift2File(URI uri) {
        try {
            return new File(uri);
        } catch (Throwable t) {
            log.error(t.toString(), t);
            return null;
        }
    }

    private void sendAllResource() {
        if (null == resources) {
            return;
        }
        for (Resource r : resources) {
            if (null != r) {
                log.debug("resource file name=" + r.getFilename());
                PropertiesManager.putReloadResource(r);
            }
        }
    }

    private void sendSpecialResource(String fn) {
        if (null == fn) {
            return;
        }
        if (null == resources) {
            return;
        }
        for (Resource r : resources) {
            if (null == r) {
                continue;
            }
            String n = r.getFilename();
            if (null == n) {
                continue;
            }
            if (!n.equals(fn)) {
                continue;
            }
            PropertiesManager.putReloadResource(r);
            log.debug("get special resource=" + r);
            break;
        }
    }

    private void closeService() {
        if (null != service) {
            try {
                service.close();
            } catch (IOException e) {
                printErr(e);
            } catch (Throwable t) {
                printErr(t);
            }
        }
    }

}
