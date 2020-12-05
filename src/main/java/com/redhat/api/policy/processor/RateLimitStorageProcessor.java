package com.redhat.api.policy.processor;

import java.util.LinkedList;
import java.util.logging.Logger;

import com.redhat.api.policy.configuration.PolicyConfig;
import com.redhat.api.policy.enumerator.ApplicationEnum;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.redhat.api.policy.dto.HitCountDTO;
import com.redhat.api.policy.dto.HitCountStorageDTO;

@Component
public class RateLimitStorageProcessor implements Processor {

    private static final Logger LOGGER = Logger.getLogger(RateLimitStorageProcessor.class.getName());

    @Autowired
    private PolicyConfig policyConfig;

    @Override
    public void process(Exchange exchange) throws Exception {
        LOGGER.info(":: method: process(Exchange exchange) called");
        LOGGER.info("\t:: key.value = [" + exchange.getIn().getHeader(InfinispanConstants.KEY) + "," + exchange.getIn().getBody(String.class) + "]");

        LinkedList<Long> response = (LinkedList<Long>) exchange.getIn().getBody();

        HitCountDTO hitCountDTO = exchange.getProperty(ApplicationEnum.HIT_COUNT.getValue(), HitCountDTO.class);
        HitCountStorageDTO entry = new HitCountStorageDTO()
                .withIp(exchange.getIn().getHeader(InfinispanConstants.KEY).toString());

        if (response == null || response.isEmpty()) {
            entry.withTimestampRecords(new LinkedList<Long>());
        } else {
            entry.withTimestampRecords((LinkedList<Long>) response.clone());
        }

        long boundary = hitCountDTO.getTimeStamp() - policyConfig.getTimeWindow();

        synchronized (entry.getTimestampRecords()) {
            if (hitCountDTO.getHitCount() >= policyConfig.getMaxHitCount()) {
                while (!entry.getTimestampRecords().isEmpty() && entry.getTimestampRecords().element() <= boundary) {
                    entry.getTimestampRecords().poll();
                }
            }
            entry.getTimestampRecords().add(hitCountDTO.getTimeStamp());
        }

        LOGGER.info("\t:: ip [" + entry.getIp() + "] with [" + entry.getTimestampRecords().size() + "] hits in current time-frame");
        hitCountDTO.withHitCount(entry.getTimestampRecords().size());

        exchange.setProperty(ApplicationEnum.HIT_COUNT_TOTAL.getValue(), hitCountDTO.getHitCount());
        exchange.getIn().setBody("");
        exchange.getIn().setHeader(ApplicationEnum.HIT_TIMESTAMP.getValue(), entry.getTimestampRecords());
        exchange.getIn().setHeader(ApplicationEnum.HIT_BOUNDARY.getValue(), boundary);
    }

}