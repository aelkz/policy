package br.gov.bnb.openbanking.policy.ipratelimit.processor;

import java.util.logging.Logger;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.gov.bnb.openbanking.policy.ipratelimit.dto.HitCountDTO;
import br.gov.bnb.openbanking.policy.ipratelimit.exception.RateLimitException;
import br.gov.bnb.openbanking.policy.ipratelimit.route.CacheRoute;

@Component
public class RateLimitProcessor implements Processor {

  private static final Logger LOGGER = Logger.getLogger(RateLimitProcessor.class.getName());

  @Value("${custom.policy.ipratelimit.maxhitcount}")
  private  Integer maxHitCount;
  
  @Override
  public void process(Exchange exchange) throws RateLimitException {
    HitCountDTO hitCountDTO = new HitCountDTO();
    hitCountDTO.setTimeStamp(System.currentTimeMillis());

    LOGGER.info(">>> INICIO DE OPERAÇÕES DE GET NO DATA GRID");
    LOGGER.info("Value of Key " + exchange.getIn().getHeader(InfinispanConstants.KEY) + " is "
        + exchange.getIn().getBody(String.class));
    
    hitCountDTO.setIp(exchange.getIn().getHeader(InfinispanConstants.KEY).toString());
    hitCountDTO.setHitCount(exchange.getIn().getBody(Integer.class));
    
    try{
      if (hitCountDTO.getHitCount()==null) {
        hitCountDTO.setHitCount(1);
    } else if (hitCountDTO.getHitCount() >= maxHitCount){
        throw new RateLimitException("RATE LIMIT REACHED FOR IP "+ hitCountDTO.getIp());
    }
    }finally{
        hitCountDTO.setHitCount(hitCountDTO.getHitCount()+1);
        LOGGER.info(">>>  IP: "+ hitCountDTO.getIp()+ " HIT COUNT :"+ hitCountDTO.getHitCount() + " TIMESTAMP :" +hitCountDTO.getTimeStamp());
        exchange.getIn().setBody("");
        exchange.setProperty(CacheRoute.HIT_COUNT, hitCountDTO);
    }
    
  }

}
