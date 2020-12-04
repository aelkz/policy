package com.redhat.api.policy.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PolicyConfig {
    private static Logger LOGGER = LoggerFactory.getLogger(PolicyConfig.class);

    @Value("${custom.policy.ip-rate-limit.max-hit-count}")
    private Integer maxHitCount;

    @Value("${custom.policy.ip-rate-limit.time-window}")
    private Integer timeWindow;

    @Value("${custom.policy.ip-rate-limit.ip-address-whitelist}")
    private String ipWhitelist;

    public Integer getMaxHitCount() {
        return maxHitCount;
    }

    public void setMaxHitCount(Integer maxHitCount) {
        this.maxHitCount = maxHitCount;
    }

    public Integer getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(Integer timeWindow) {
        this.timeWindow = timeWindow;
    }

    public String getIpWhitelist() {
        return ipWhitelist;
    }

    public void setIpWhitelist(String ipWhitelist) {
        this.ipWhitelist = ipWhitelist;
    }
}

