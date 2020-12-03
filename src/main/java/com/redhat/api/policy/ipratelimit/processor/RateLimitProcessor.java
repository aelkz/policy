package com.redhat.api.policy.ipratelimit.processor;

import java.util.logging.Logger;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.redhat.api.policy.ipratelimit.dto.HitCountDTO;
import com.redhat.api.policy.ipratelimit.exception.RateLimitException;
import com.redhat.api.policy.ipratelimit.route.CacheRoute;

/**
 * Componente responsável por recuperar informações consolidadas de acessos por IP
 *
 * @author <a href="mailto:nramalho@redhat.com">Natanael Ramalho</a>
 */
@Component
public class RateLimitProcessor implements Processor {

    private static final Logger LOGGER = Logger.getLogger(RateLimitProcessor.class.getName());

    @Value("${custom.policy.ipratelimit.maxhitcount}")
    private Integer maxHitCount;

    @Override
    public void process(Exchange exchange) throws RateLimitException {
        HitCountDTO hitCountDTO = new HitCountDTO();
        hitCountDTO.setTimeStamp(System.currentTimeMillis());

        LOGGER.info(":: Red Hat DataGrid process started");
        LOGGER.info("\t:: key.value = [" + exchange.getIn().getHeader(InfinispanConstants.KEY) + "," + exchange.getIn().getBody(String.class) + "]");

        hitCountDTO.setIp(exchange.getIn().getHeader(InfinispanConstants.KEY).toString());
        hitCountDTO.setHitCount(exchange.getIn().getBody(Integer.class));

        try {
            if (hitCountDTO.getHitCount() == null) {
                hitCountDTO.setHitCount(1);
            } else if (hitCountDTO.getHitCount() >= maxHitCount) {
                throw new RateLimitException(RateLimitException.RATE_LIMIT_REACHED_MESSAGE + hitCountDTO.getIp());
            }
        } finally {
            hitCountDTO.setHitCount(hitCountDTO.getHitCount() + 1);
            LOGGER.info("\t:: IP= " + hitCountDTO.getIp() + " HIT_COUNT= " + hitCountDTO.getHitCount() + " TIMESTAMP= " + hitCountDTO.getTimeStamp());
            exchange.getIn().setBody("");
            exchange.setProperty(CacheRoute.HIT_COUNT, hitCountDTO);
        }

    }

}
