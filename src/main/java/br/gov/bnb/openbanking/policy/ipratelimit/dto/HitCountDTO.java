package br.gov.bnb.openbanking.policy.ipratelimit.dto;

import java.io.Serializable;

public class HitCountDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  private String ip;

  private Integer hitCount;

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
  
}
