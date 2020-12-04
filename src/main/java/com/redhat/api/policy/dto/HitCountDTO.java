package com.redhat.api.policy.dto;

import java.io.Serializable;

public class HitCountDTO implements Serializable {

    private static final long serialVersionUID = 5861262466288537282L;
    private String ip;
    private Integer hitCount;
    private Long timeStamp;

    public String getIp() {
        return ip;
    }

    public Integer getHitCount() {
        return hitCount;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public HitCountDTO increase() {
        this.hitCount = hitCount+1;
        return this;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hitCount == null) ? 0 : hitCount.hashCode());
        result = prime * result + ((ip == null) ? 0 : ip.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HitCountDTO other = (HitCountDTO) obj;
        if (hitCount == null) {
            if (other.hitCount != null)
                return false;
        } else if (!hitCount.equals(other.hitCount))
            return false;
        if (ip == null) {
            if (other.ip != null)
                return false;
        } else if (!ip.equals(other.ip))
            return false;
        return true;
    }

}
