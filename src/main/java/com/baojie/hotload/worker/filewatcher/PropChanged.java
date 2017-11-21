package com.baojie.hotload.worker.filewatcher;

import com.baojie.hotload.service.manager.BeanPropHolder;
import com.baojie.hotload.service.manager.HotLoadProcessor;
import com.baojie.hotload.service.manager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class PropChanged implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(PropChanged.class);
    private final SimpleTypeConverter typeConverter = new SimpleTypeConverter();
    private final LinkedBlockingQueue<Resource> resourceQueue;
    private final AtomicBoolean stop;

    public static PropChanged create(final LinkedBlockingQueue<Resource> rq, final AtomicBoolean stop) {
        return new PropChanged(rq, stop);
    }

    private PropChanged(final LinkedBlockingQueue<Resource> rq, final AtomicBoolean stop) {
        this.resourceQueue = rq;
        this.stop = stop;
    }

    @Override
    public void run() {
        try {
            work();
        } finally {

        }
    }

    private void work() {
        Resource r;
        for (; ; ) {
            if (stop.get()) {
                return;
            }
            r = getResource();
            if (null == r) {
                continue;
            }
            reload(r);
        }
    }

    private Resource getResource() {
        try {
            return resourceQueue.take();
        } catch (InterruptedException e) {
            printErr(e);
            return null;
        } catch (Throwable t) {
            printErr(t);
            return null;
        }
    }

    private void printErr(final Throwable t) {
        if (stop.get()) {
            log.error(t.toString());
        } else {
            log.error(t.toString(), t);
        }
    }

    private void reload(Resource r) {
        Properties p = shift2Prop(r);
        if (null == p) {
            p = getFromJDK(r);
        }
        if (null == p) {
            log.error("get properties from spring and JDK null, resource=" + r);
            return;
        }
        Properties sp = getSpringProp();
        if (null == sp) {
            log.error("get spring properties null, resource=" + r);
            return;
        }
        // 以变化的properties为准，不要每次遍历spring properties
        Iterator it = p.stringPropertyNames().iterator();
        while (it.hasNext()) {
            String spPropKey = (String) it.next();
            String oldValue = sp.getProperty(spPropKey);
            String newValue = p.getProperty(spPropKey);
            if (null == oldValue || null == newValue) {
                log.warn("oldValue=" + oldValue + ", newValue" + newValue + ", spPropKey=" + spPropKey);
            }
            if (null == oldValue && null == newValue) {
                log.warn("newValue and oldValue null, spPropKey=" + spPropKey);
                continue;
            }
            // 如果原来的值为oldString=old,当修改properties文件中的值为空时，也就是newString=blank
            // 这时，null == newValue是成立的，所以这时的修改会失效
            if (null == newValue) {
                log.error("null == newValue, oldValue=" + oldValue + ", spPropKey=" + spPropKey + ", resource=" + r);
                continue;
            }
            // 原来的代码中不包含新添加的key，不更新
            if (!sp.contains(spPropKey)) {
                log.error("spring prop not contains key=" + spPropKey + ", old=" + oldValue + ", new=" + newValue);
                continue;
            }
            if (!newValue.equals(oldValue)) {
                changeProperties(spPropKey, p, sp);
                log.debug("value has changed, newValue=" + newValue + ", oldValue=" + oldValue + ", resource=" + r);
            }
        }
    }

    private Properties shift2Prop(Resource r) {
        try {
            return PropertiesLoaderUtils.loadProperties(r);
        } catch (Throwable t) {
            log.error(t.toString() + ", resource=" + r, t);
            return null;
        }
    }

    private File resource2File(Resource r) {
        try {
            return r.getFile();
        } catch (IOException e) {
            log.error(e.toString() + " resource=" + r, e);
        } catch (Throwable t) {
            log.error(t.toString() + " resource=" + r, t);
        }
        return null;
    }

    private Properties getFromJDK(Resource r) {
        File f = resource2File(r);
        if (null == f) {
            log.error("resource2File null, resource=" + r);
            return null;
        }
        String filePath = f.getAbsolutePath();
        String fn = f.getName();
        log.debug("JDK filePath=" + filePath + ", fileName=" + fn);
        InputStream inputStream = getInputStream(fn);
        if (null == inputStream) {
            log.error("inputStream null, filePath=" + filePath + ", fileName=" + fn);
            return null;
        }
        Properties p = loadProperties(inputStream);
        if (null == p) {
            closeStream(inputStream);
            log.error("load properties from inputStream null, filePath=" + filePath + ", fileName=" + fn);
            return null;
        }
        closeStream(inputStream);
        return p;
    }

    // 这里要修改下，获取当前工程路径，没有改
    private InputStream getInputStream(String fn) {
        String path = "." + File.separator + "props" + File.separator + fn;
        try {
            return PropChanged.class.getClassLoader().getResourceAsStream(path);
        } catch (Exception e) {
            log.error(e.toString() + ", fileName=" + fn + ", path=" + path, e);
        } catch (Throwable t) {
            log.error(t.toString() + ", fileName=" + fn + ", path=" + path, t);
        }
        return null;
    }

    private Properties loadProperties(final InputStream inputStream) {
        try {
            Properties p = new Properties();
            p.load(inputStream);
            return p;
        } catch (IOException e) {
            log.error(e.toString(), e);
        }
        return null;
    }

    private void closeStream(InputStream inputStream) {
        if (null == inputStream) {
            return;
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            log.error(e.toString(), e);
        }
    }

    private Properties getSpringProp() {
        return ResourceManager.getProperties();
    }

    private Map<String, BeanPropHolder> getBeanProp() {
        return HotLoadProcessor.getBeanProp();
    }

    private void changeProperties(String spPropKey, Properties p, Properties springInnerProp) {
        Map<String, BeanPropHolder> beanMap = getBeanProp();
        if (null == beanMap) {
            log.error("HotLoadProcessor.getBeanProp null");
            return;
        }
        BeanPropHolder holder = beanMap.get(spPropKey);
        if (null == holder) {
            log.error("get BeanPropHolder null, spPropKey=" + spPropKey);
            return;
        }
        Object newBeanValue = p.get(spPropKey);
        if (null == newBeanValue) {
            log.error("newBeanValue null, spPropKey=" + spPropKey);
        }
        Object newRealValue = convert(newBeanValue, holder.getField());
        if (null == newRealValue) {
            log.error("convert fail, BeanPropHolder=" + holder + ", spPropKey=" + spPropKey);
            return;
        }
        if (doChange(holder, newRealValue)) {
            updateSpringProp(spPropKey, springInnerProp, newRealValue);
        }
    }

    private Object convert(Object newBeanValue, Field fieldToUpdate) {
        try {
            return typeConverter.convertIfNecessary(newBeanValue, fieldToUpdate.getType());
        } catch (TypeMismatchException matchExe) {
            log.error(matchExe.toString(), matchExe);
        } catch (Throwable t) {
            log.error(t.toString(), t);
        }
        return null;
    }

    private boolean doChange(BeanPropHolder holder, Object newRealValue) {
        Object beanToUpdate = holder.getBean();
        Field fieldToUpdate = holder.getField();
        try {
            fieldToUpdate.set(beanToUpdate, newRealValue);
            return true;
        } catch (IllegalAccessException e) {
            log.error(e.toString(), e);
        } catch (Throwable t) {
            log.error(t.toString(), t);
        }
        return false;
    }

    // 更新spring中的properties文件属性值
    private void updateSpringProp(String spPropKey, Properties springInnerProp, Object real) {
        synchronized (springInnerProp) {
            if (springInnerProp.contains(spPropKey)) {
                springInnerProp.replace(spPropKey, real);
                log.debug("update prop, key=" + spPropKey + ", value=" + real + ", size=" + springInnerProp.size());
            } else {
                springInnerProp.put(spPropKey, real);
                log.debug("put prop, key=" + spPropKey + ", value=" + real + ", size=" + springInnerProp.size());
            }
        }
    }

}
