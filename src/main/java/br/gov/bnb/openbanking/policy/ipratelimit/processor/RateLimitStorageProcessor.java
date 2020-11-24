package br.gov.bnb.openbanking.policy.ipratelimit.processor;

import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.infinispan.InfinispanOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.gov.bnb.openbanking.policy.ipratelimit.dto.HitCountDTO;
import br.gov.bnb.openbanking.policy.ipratelimit.dto.HitCountStorageDTO;
import br.gov.bnb.openbanking.policy.ipratelimit.route.CacheRoute;

@Component
public class RateLimitStorageProcessor implements Processor {

    private static final Logger LOGGER = Logger.getLogger(RateLimitStorageProcessor.class.getName());

    @Value("${custom.policy.ipratelimit.maxhitcount}")
    private  Integer maxHitCount;

    @Value("${custom.policy.ipratelimit.timeWindow}")
    private  Integer timeWindow;

    @Override
    public void process(Exchange exchange) throws Exception {


        LOGGER.info(">>> INICIO DE OPERAÇÕES DE GET STORAGE NO DATA GRID");
        LOGGER.info("Value of Key " + exchange.getIn().getHeader(InfinispanConstants.KEY) + " is "
                + exchange.getIn().getBody(String.class));

        LinkedList<Long> response = (LinkedList<Long>) exchange.getIn().getBody();
        
        HitCountDTO hitCountDTO = exchange.getProperty(CacheRoute.HIT_COUNT, HitCountDTO.class);
        HitCountStorageDTO chave = new HitCountStorageDTO();
        chave.setIp(exchange.getIn().getHeader(InfinispanConstants.KEY).toString());

        if (response == null  || response.isEmpty()) {
            chave.setTimestamps(new LinkedList<Long>());
        } else {
            chave.setTimestamps((LinkedList<Long>)response.clone());
        }

        long curTime = hitCountDTO.getTimeStamp();
        long boundary = curTime - timeWindow;
        synchronized (chave.getTimestamps()) {
            if(hitCountDTO.getHitCount()>=maxHitCount){
                while (!chave.getTimestamps().isEmpty() && chave.getTimestamps().element() <= boundary) {
                    chave.getTimestamps().poll();
                }
            }
            chave.getTimestamps().add(curTime);
        }
        LOGGER.info(">>> KEY :"+chave.getIp() + " HIT VALUES IN WINDOW BOUNDRY :" + chave.getTimestamps().size()); 
        hitCountDTO.setHitCount(chave.getTimestamps().size());
        exchange.setProperty(CacheRoute.HIT_COUNT, hitCountDTO);
        exchange.getIn().setBody("");
        exchange.getIn().setHeader(CacheRoute.HIT_TIMESTAMP, chave.getTimestamps());
    }

}
