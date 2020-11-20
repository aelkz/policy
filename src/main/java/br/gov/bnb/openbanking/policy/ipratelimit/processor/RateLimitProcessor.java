package br.gov.bnb.openbanking.policy.ipratelimit.processor;

import java.util.logging.Logger;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.infinispan.InfinispanConstants;

import br.gov.bnb.openbanking.policy.ipratelimit.dto.HitCountDTO;
import br.gov.bnb.openbanking.policy.ipratelimit.exception.RateLimitException;

public class RateLimitProcessor implements Processor {

  private static final Logger LOGGER = Logger.getLogger(RateLimitProcessor.class.getName());

  @Override
  public void process(Exchange exchange) throws RateLimitException {
    LOGGER.info(">>> INICIO DE OPERAÇÕES COM DATA GRID");
    LOGGER.info("Value of Key " + exchange.getIn().getHeader(InfinispanConstants.KEY) + " is "
        + exchange.getIn().getBody(String.class));

    HitCountDTO hitCountDTO = new HitCountDTO();
    hitCountDTO.setIp(exchange.getIn().getHeader(InfinispanConstants.KEY).toString());
    hitCountDTO.setHitCount(exchange.getIn().getBody(Integer.class));

    if (hitCountDTO.getHitCount()==null) {
        hitCountDTO.setHitCount(1);
    } else{
      hitCountDTO.setHitCount(hitCountDTO.getHitCount()+1);
    }
    LOGGER.info(">>>  IP: "+ hitCountDTO.getIp()+ "HIT COUNT :"+ hitCountDTO.getHitCount());
    exchange.setProperty("HIT_COUNT",hitCountDTO);
  }

}
