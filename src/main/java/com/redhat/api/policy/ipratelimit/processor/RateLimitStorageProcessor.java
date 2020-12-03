package com.redhat.api.policy.ipratelimit.processor;

import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.redhat.api.policy.ipratelimit.dto.HitCountDTO;
import com.redhat.api.policy.ipratelimit.dto.HitCountStorageDTO;
import com.redhat.api.policy.ipratelimit.route.CacheRoute;

/**
 * Componente responável por gerenciar a quantidade de requisições dentro da janela tempo definida
 *
 * @author <a href="mailto:nramalho@redhat.com">Natanael Ramalho</a>
 */
@Component
public class RateLimitStorageProcessor implements Processor {

    private static final Logger LOGGER = Logger.getLogger(RateLimitStorageProcessor.class.getName());

    @Value("${custom.policy.ipratelimit.maxhitcount}")
    private Integer maxHitCount;

    @Value("${custom.policy.ipratelimit.timeWindow}")
    private Integer timeWindow;

    @Override
    public void process(Exchange exchange) throws Exception {


        LOGGER.info(":: Red Hat DataGrid process started");
        LOGGER.info("\t:: key.value = [" + exchange.getIn().getHeader(InfinispanConstants.KEY) + "," + exchange.getIn().getBody(String.class) + "]");

        LinkedList<Long> response = (LinkedList<Long>) exchange.getIn().getBody();

        HitCountDTO hitCountDTO = exchange.getProperty(CacheRoute.HIT_COUNT, HitCountDTO.class);
        HitCountStorageDTO chave = new HitCountStorageDTO();
        chave.setIp(exchange.getIn().getHeader(InfinispanConstants.KEY).toString());

        if (response == null || response.isEmpty()) {
            chave.setTimestamps(new LinkedList<Long>());
        } else {
            chave.setTimestamps((LinkedList<Long>) response.clone());
        }

        long curTime = hitCountDTO.getTimeStamp();
        long boundary = curTime - timeWindow;

        synchronized (chave.getTimestamps()) {
            if (hitCountDTO.getHitCount() >= maxHitCount) {
                while (!chave.getTimestamps().isEmpty() && chave.getTimestamps().element() <= boundary) {
                    chave.getTimestamps().poll();
                }
            }
            chave.getTimestamps().add(curTime);
        }

        LOGGER.info("\t:: KEY= " + chave.getIp() + " HITS_IN_CURRENT_TIME_FRAME= " + chave.getTimestamps().size());
        hitCountDTO.setHitCount(chave.getTimestamps().size());
        exchange.setProperty(CacheRoute.HIT_COUNT_TOTAL, hitCountDTO.getHitCount());
        exchange.getIn().setBody("");
        exchange.getIn().setHeader(CacheRoute.HIT_TIMESTAMP, chave.getTimestamps());
    }

}
