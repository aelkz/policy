package com.redhat.api.policy.ipratelimit.dto;

import java.io.Serializable;
import java.util.LinkedList;

public class HitCountStorageDTO implements Serializable {

    private static final long serialVersionUID = 997307483019714284L;
    private String ip;
    private LinkedList<Long> timestamps;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public LinkedList<Long> getTimestamps() {
        return timestamps;
    }

    public void setTimestamps(LinkedList<Long> timestamps) {
        this.timestamps = timestamps;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
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
        HitCountStorageDTO other = (HitCountStorageDTO) obj;
        if (ip == null) {
            if (other.ip != null)
                return false;
        } else if (!ip.equals(other.ip))
            return false;
        return true;
    }

}
