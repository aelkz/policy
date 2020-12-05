package com.redhat.api.policy.processor;

import com.redhat.api.policy.configuration.PolicyConfig;
import com.redhat.api.policy.configuration.SSLProxyConfig;
import com.redhat.api.policy.enumerator.ApplicationEnum;
import com.redhat.api.policy.exception.RateLimitException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.logging.Logger;

@Component
public class DebugRateLimitProcessor implements Processor {

    private static final Logger LOGGER = Logger.getLogger(DebugRateLimitProcessor.class.getName());

    @Autowired
    private SSLProxyConfig proxyConfig;

    @Autowired
    private PolicyConfig policyConfig;

    public DebugRateLimitProcessor() { }

    @Override
    public void process(Exchange exchange) throws RateLimitException {
        final Message message = exchange.getIn();
        message.setHeader("X-RateLimit-Limit", policyConfig.getMaxHitCount());

        int hits = (Integer) exchange.getProperty(ApplicationEnum.HIT_COUNT_TOTAL.getValue());
        hits = --hits; // decrease as increment value will be persisted into infinispan afterwards
        int maxPolicyHits = policyConfig.getMaxHitCount();
        if (hits > 0) {
            hits = maxPolicyHits - hits;
        } else {
            hits = maxPolicyHits;
        }
        message.setHeader("X-RateLimit-Remaining", hits);

        message.setHeader("X-RateLimit-Time", System.currentTimeMillis());
        message.setHeader("X-RateLimit-Reset", exchange.getIn().getHeader(ApplicationEnum.HIT_BOUNDARY.getValue()));
    }

}
