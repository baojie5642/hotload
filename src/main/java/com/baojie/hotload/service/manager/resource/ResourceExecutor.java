package com.baojie.hotload.service.manager.resource;

import org.springframework.core.io.Resource;

import java.util.Properties;

public class ResourceExecutor {
    private final HotLoadResource hotLoadResource;

    private ResourceExecutor(final HotLoadResource hotLoadResource) {
        this.hotLoadResource = hotLoadResource;
    }

    public static ResourceExecutor create(final HotLoadResource hotLoadResource) {
        if (null == hotLoadResource) {
            throw new IllegalStateException("hotLoadResource");
        }
        return new ResourceExecutor(hotLoadResource);
    }

    public Properties getProperties() {
        return hotLoadResource.getProperties();
    }

    public Resource[] getLocations() {
        return hotLoadResource.getLocations();
    }

}
