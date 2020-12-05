package com.redhat.api.policy.dto;

import java.io.Serializable;

public class HitCountDTO implements Serializable {

    private static final long serialVersionUID = 5861262466288537282L;
    private String ip;
    private Integer hitCount;
    private Long timeStamp;

    public HitCountDTO() { }

    public Integer getHitCount() {
        if (this.hitCount == null) {
            this.hitCount = 0;
        }
        return hitCount;
    }

    public String getIp() {
        return ip;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void increase() {
        this.hitCount = hitCount+1;
    }

    public HitCountDTO withHitCount(Integer hitCount) {
        this.hitCount = hitCount;
        return this;
    }

    public HitCountDTO withIp(String ip) {
        this.ip = ip;
        return this;
    }

    public HitCountDTO withTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
        return this;
    }

    public Boolean isEmpty() {
        return getHitCount() == null || getHitCount() == 0;
    }

}
