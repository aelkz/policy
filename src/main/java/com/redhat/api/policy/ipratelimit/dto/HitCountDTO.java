package com.redhat.api.policy.ipratelimit.dto;

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

    public void setHitCount(Integer hitCount) {
        this.hitCount = hitCount;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
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
