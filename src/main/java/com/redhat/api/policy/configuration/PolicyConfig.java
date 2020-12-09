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
    private Long timeWindow;

    @Value("${custom.policy.ip-rate-limit.x-forwarded-for}")
    private String xForwardedFor;

    @Value("${custom.policy.ip-rate-limit.debug}")
    private String debug;

    public Integer getMaxHitCount() {
        return maxHitCount;
    }

    public void setMaxHitCount(Integer maxHitCount) {
        this.maxHitCount = maxHitCount;
    }

    public Long getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(Long timeWindow) {
        this.timeWindow = timeWindow;
    }

    public String getxForwardedFor() {
        return xForwardedFor;
    }

    public void setxForwardedFor(String xForwardedFor) {
        this.xForwardedFor = xForwardedFor;
    }

    public Boolean debug() {
        return Boolean.valueOf(debug);
    }

    public String getDebug() {
        return debug;
    }

    public void setDebug(String debug) {
        this.debug = debug;
    }
}

