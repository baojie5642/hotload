package com.baojie.hotload.service.manager;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class ReflectionCallback implements ReflectionUtils.FieldCallback {
    private static final Logger log = LoggerFactory.getLogger(ReflectionCallback.class);
    private final SimpleTypeConverter typeConverter = new SimpleTypeConverter();
    private final Object bean;

    private ReflectionCallback(final Object bean) {
        this.bean = bean;
    }

    public static ReflectionCallback create(final Object bean) {
        if (null == bean) {
            throw new IllegalStateException("ReflectionCallback");
        }
        return new ReflectionCallback(bean);
    }

    @Override
    public void doWith(final Field field) {
        if (null == field) {
            log.error("field null");
            return;
        }
        ReflectionUtils.makeAccessible(field);
        Object a = field.getAnnotation(HotReloadable.class);
        if (null == a) {
            return;
        }
        if (!(a instanceof HotReloadable)) {
            return;
        }
        modifyCheck(field);
        HotReloadable h = (HotReloadable) a;
        String k = keyString(h);
        if (null == k) {
            throw new IllegalArgumentException("annotation value null");
        }
        Object pv = ResourceManager.getProperties().get(k);
        validate(k, pv, field);
        if (null == pv) {
            return;
        }
        setPropFromConfig(pv, field);
        BeanPropHolder bh = BeanPropHolder.create(bean, field);
        HotLoadProcessor.putProp4Init(k, bh);
    }

    private void modifyCheck(final Field field) {
        if (Modifier.isFinal(field.getModifiers())) {
            log.error("final field");
            throw new IllegalArgumentException("final field, name=" + field.getClass() + "." + field.getName());
        }
        if (Modifier.isStatic(field.getModifiers())) {
            log.warn("static field");
        }
    }

    private String keyString(final HotReloadable h) {
        final String temp = h.value();
        if (StringUtils.isBlank(temp)) {
            throw new IllegalArgumentException("value blank");
        }
        return temp.replace("${", "").replace("}", "");
    }

    private void validate(final String k, final Object p, final Field field) {
        Object o = null;
        try {
            o = field.get(k);
        } catch (IllegalAccessException e) {
            o = null;
            log.error(e.toString() + ", k=" + k, e);
        } catch (Throwable t) {
            o = null;
            log.error(t.toString(), t);
        }
        // 如果properties配置文件中找不到，并且在代码中还没有默认值，那么会报错，启动异常
        if (null == p && null == o) {
            throw new IllegalArgumentException("properties null and default value null");
        }
    }

    private void setPropFromConfig(final Object pv, final Field field) {
        Object p_real_v = typeConverter.convertIfNecessary(pv, field.getType());
        try {
            field.set(bean, p_real_v);
        } catch (IllegalAccessException e) {
            log.error(e.toString(), e);
        } catch (Throwable t) {
            log.error(t.toString(), t);
        }
    }

}
