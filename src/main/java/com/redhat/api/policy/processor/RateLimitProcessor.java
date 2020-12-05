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
        LOGGER.info(":: method: process(Exchange exchange) called");
        LOGGER.info("\t:: key.value = [" + exchange.getIn().getHeader(InfinispanConstants.KEY) + "," + exchange.getIn().getBody(String.class) + "]");

        HitCountDTO record = new HitCountDTO(); // must initialize it first

        record.withTimeStamp(System.currentTimeMillis())
        .withIp(exchange.getIn().getHeader(InfinispanConstants.KEY) != null ? exchange.getIn().getHeader(InfinispanConstants.KEY).toString() : "")
        .withHitCount(exchange.getIn().getBody(Integer.class));

        try {
            if (record.isEmpty()) {
                record.withHitCount(1);
            } else if (record.getHitCount() >= policyConfig.getMaxHitCount()) {
                throw new RateLimitException(record.getIp());
            }
        // catch (Excption e) {} -> block not allowed here!
        } finally {
            record.increase();
            long boundary = record.getTimeStamp() - policyConfig.getTimeWindow();

            exchange.getIn().setBody("");
            exchange.setProperty(ApplicationEnum.HIT_COUNT.getValue(), record);
            exchange.setProperty(ApplicationEnum.HIT_COUNT_TOTAL.getValue(), record.getHitCount());
            exchange.getIn().setHeader(ApplicationEnum.HIT_BOUNDARY.getValue(), boundary);

            LOGGER.info("\t:: ip [" + record.getIp() + "] hits [" + record.getHitCount() + "] at " + record.getTimeStamp());
        }

    }

}
