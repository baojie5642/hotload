package com.baojie.hotload.service.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class RoamConfManager {
    private static final Logger log = LoggerFactory.getLogger(RoamConfManager.class);
    @HotReloadable("${roam.bizThreadNum}")
    private static volatile int bizThreadNum = 4;
    @HotReloadable("${roam.disThreadNum}")
    private static volatile int disThreadNum = 4;
    @HotReloadable("${roam.roamThreadNum}")
    private static volatile int roamThreadNum = 4;
    @HotReloadable("${roam.smsThreadNum}")
    private static volatile int smsThreadNum = 4;
    @HotReloadable("${roam.queryTaskThreadNum}")
    private static volatile int queryTaskThreadNum = 4;
    @HotReloadable("${roam.queryMax}")
    private static volatile int queryMax = 40;
    @HotReloadable("${roam.errMaxDuration}")
    private static volatile int errMaxDuration = 1800;
    @HotReloadable("${roam.schedDuration}")
    private static volatile int schedDuration = 180;


    @HotReloadable("${baojie_0_0}")
    private static volatile String baojie_0_0;
    @HotReloadable("${baojie_0_1}")
    private static volatile String baojie_0_1;

    public static String getBaojie_0_0() {
        return baojie_0_0;
    }

    public static String getBaojie_0_1() {
        return baojie_0_1;
    }

    @PostConstruct
    private void init() {
        log.debug("bizThreadNum=" + bizThreadNum);
        log.debug("disThreadNum=" + disThreadNum);
        log.debug("roamThreadNum=" + roamThreadNum);
        log.debug("smsThreadNum=" + smsThreadNum);
        log.debug("errMaxDuration=" + errMaxDuration);
        log.debug("queryTaskThreadNum=" + queryTaskThreadNum);
        log.debug("queryMax=" + queryMax);
        log.debug("schedDuration=" + schedDuration);
    }

    public static int getBizThreadNum() {
        return bizThreadNum;
    }

    public static int getDisThreadNum() {
        return disThreadNum;
    }

    public static int getRoamThreadNum() {
        return roamThreadNum;
    }

    public static int getSmsThreadNum() {
        return smsThreadNum;
    }

    public static int getErrMaxDuration() {
        return errMaxDuration;
    }

    public static int getQueryTaskThreadNum() {
        return queryTaskThreadNum;
    }

    public static int getQueryMax() {
        return queryMax;
    }

    public static int getSchedDuration() {
        return schedDuration;
    }
}
