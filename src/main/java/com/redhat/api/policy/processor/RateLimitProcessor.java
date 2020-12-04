package com.redhat.api.policy.processor;

import java.util.logging.Logger;

import com.redhat.api.policy.configuration.PolicyConfig;
import com.redhat.api.policy.enumerator.ApplicationEnum;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.redhat.api.policy.dto.HitCountDTO;
import com.redhat.api.policy.exception.RateLimitException;

@Component
public class RateLimitProcessor implements Processor {

    private static final Logger LOGGER = Logger.getLogger(RateLimitProcessor.class.getName());

    @Autowired
    private PolicyConfig policyConfig;

    @Override
    public void process(Exchange exchange) throws RateLimitException {
        LOGGER.info(":: RateLimitProcessor.process(Exchange exchange) throws RateLimitException called");
        LOGGER.info("\t:: key.value = [" + exchange.getIn().getHeader(InfinispanConstants.KEY) + "," + exchange.getIn().getBody(String.class) + "]");

        HitCountDTO hitCountDTO = new HitCountDTO()
            .withTimeStamp(System.currentTimeMillis())
            .withIp(exchange.getIn().getHeader(InfinispanConstants.KEY).toString())
            .withHitCount(exchange.getIn().getBody(Integer.class));

        try {
            if (hitCountDTO.isEmpty()) {
                hitCountDTO.withHitCount(1);
            } else if (hitCountDTO.getHitCount() >= policyConfig.getMaxHitCount()) {
                throw new RateLimitException(hitCountDTO.getIp());
            }
        } catch (Exception ex){
            LOGGER.severe(ApplicationEnum.GENERAL_PROXY_ERROR_MESSAGE.getValueWithMessage(ex.getMessage()));
        } finally {
            hitCountDTO.increase();
            exchange.getIn().setBody("");
            exchange.setProperty(ApplicationEnum.HIT_COUNT.getValue(), hitCountDTO);
            LOGGER.info("\t:: ip [" + hitCountDTO.getIp() + "] hits [" + hitCountDTO.getHitCount() + "] at " + hitCountDTO.getTimeStamp());
        }

    }

}
