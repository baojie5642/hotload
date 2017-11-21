package com.baojie.hotload.service.manager;

import com.baojie.hotload.service.manager.resource.HotLoadResource;
import com.baojie.hotload.service.manager.resource.ResourceExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Properties;

// 继承这个类是为了解决静态调用出错的问题，这个与spring的bean的初始化顺序有关
@Service
public class ResourceManager extends InstantiationAwareBeanPostProcessorAdapter {
    private static volatile ResourceExecutor executor;
    @Autowired
    private volatile HotLoadResource hotLoadResource;

    @PostConstruct
    private void init() {
        if (null == hotLoadResource) {
            throw new IllegalStateException("hotLoadResource");
        }
        if (null != executor) {
            throw new IllegalStateException("executor");
        }
        executor = ResourceExecutor.create(hotLoadResource);
    }

    public static Properties getProperties() {
        return executor.getProperties();
    }

    public static Resource[] getLocations() {
        return executor.getLocations();
    }

}
