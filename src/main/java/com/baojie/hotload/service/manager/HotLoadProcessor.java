package com.baojie.hotload.service.manager;

import com.baojie.hotload.service.manager.resource.HotLoadResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HotLoadProcessor extends InstantiationAwareBeanPostProcessorAdapter {
    private static final Logger log = LoggerFactory.getLogger(HotLoadProcessor.class);
    private static final Map<String, BeanPropHolder> BEAN_PROP = new ConcurrentHashMap<>(256);
    @Autowired
    private volatile HotLoadResource hotLoadResource;

    @PostConstruct
    private void init() {
        if (null == hotLoadResource) {
            throw new IllegalStateException("hotLoadResource");
        }
    }

    @PreDestroy
    private void destory() {
        if (null != BEAN_PROP) {
            BEAN_PROP.clear();
        }
    }

    @Override
    public boolean postProcessAfterInstantiation(final Object bean, final String beanName) throws BeansException {
        log.info("beanName=" + beanName);
        if (log.isDebugEnabled()) {
            log.debug("post beanName=" + beanName);
        }
        ReflectionUtils.doWithFields(bean.getClass(), ReflectionCallback.create(bean));
        return true;
    }

    protected static void putProp4Init(final String k, final BeanPropHolder b) {
        if (null == k || null == b) {
            return;
        }
        BEAN_PROP.put(k, b);
    }

    public static Map<String, BeanPropHolder> getBeanProp() {
        return BEAN_PROP;
    }

}
