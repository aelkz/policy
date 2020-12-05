package com.redhat.api.policy.route.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.api.policy.configuration.PolicyConfig;
import com.redhat.api.policy.configuration.SSLProxyConfig;
import com.redhat.api.policy.enumerator.ApplicationEnum;
import com.redhat.api.policy.exception.RateLimitException;
import com.redhat.api.policy.processor.*;

import com.redhat.api.policy.route.external.ProxyRoute;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.infinispan.InfinispanOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("policy-ip-rate-limit")
public class CacheRoute extends RouteBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheRoute.class);

    @Autowired
    private RateLimitProcessor rateLimitProcessor;

    @Autowired
    private RateLimitStorageProcessor rateLimitStorageProcessor;

    @Autowired
    private SSLProxyConfig proxyConfig;

    @Autowired
    private PolicyConfig policyConfig;

    @Override
    public void configure() throws Exception {

        // /--------------------------------------------------\
        // | acquire remote address hits                      |
        // \--------------------------------------------------/

        from("direct:policy")
            .routeId("execute-route")
            .doTry()
                .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.GET))
                .setHeader(InfinispanConstants.KEY, simple("${exchangeProperty." + ApplicationEnum.CLIENT_IP.getValue() + "}"))
                .log(":: JBoss Data Grid :: PUT#01 :: " + "${exchangeProperty." + ApplicationEnum.CLIENT_IP.getValue() + "}")
                .to("infinispan://{{infinispan.client.hotrod.cache}}?cacheContainer=#cacheContainer")
            .endDoTry()
            .doCatch(Exception.class)
                .log(":: Exception :: direct:policy :: infinispan service unavailable")
                .process(CacheRoute::serviceUnavailable)
            .end()

            .doTry()
                .process(rateLimitProcessor)
            .endDoTry()
            .doCatch(RateLimitException.class)
                .log(LoggingLevel.ERROR, LOGGER, ":: RateLimitException trowed")
                .process(CacheRoute::sendRateLimitError)
            .end()
            .to("direct:increment-hit-count");

        // /--------------------------------------------------\
        // | evaluate and compute remote address hits         |
        // \--------------------------------------------------/

        from("direct:increment-hit-count")
            .routeId("increment-hit-count-route")
            .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.GET))
            .setHeader(InfinispanConstants.KEY, simple("${exchangeProperty." + ApplicationEnum.CLIENT_IP.getValue() + "}-" + ApplicationEnum.HIT_TIMESTAMP.getValue()))
            .log(":: infinispan :: GET :: " +  "${exchangeProperty." + ApplicationEnum.CLIENT_IP.getValue() + "}-" + ApplicationEnum.HIT_TIMESTAMP.getValue())
                .to("infinispan:{{infinispan.client.hotrod.cache}}?cacheContainer=#cacheContainer")
                .process(rateLimitStorageProcessor)
            .multicast().parallelProcessing()
                .pipeline()
                    // TODO - implementar condicional para evitar atualização sem necessidade
                    .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.PUT))
                    .setHeader(InfinispanConstants.KEY, simple("${exchangeProperty." + ApplicationEnum.CLIENT_IP.getValue() + "}-" + ApplicationEnum.HIT_TIMESTAMP.getValue()))
                    .setHeader(InfinispanConstants.VALUE, simple("${header." + ApplicationEnum.HIT_TIMESTAMP.getValue() + "}"))
                        .log(":: infinispan :: PUT#02 :: " + "${exchangeProperty." + ApplicationEnum.CLIENT_IP.getValue() + "}-" + ApplicationEnum.HIT_TIMESTAMP.getValue())
                        .to("infinispan:{{infinispan.client.hotrod.cache}}?cacheContainer=#cacheContainer")
                .pipeline()
                    .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.PUT))
                    .setHeader(InfinispanConstants.KEY, simple("${exchangeProperty." + ApplicationEnum.CLIENT_IP.getValue() + "}"))
                    .setHeader(InfinispanConstants.VALUE, simple("${exchangeProperty." + ApplicationEnum.HIT_COUNT_TOTAL.getValue() + "}"))
                        .log(":: infinispan :: PUT#03 :: " + "${exchangeProperty." + ApplicationEnum.CLIENT_IP.getValue() + "}")
                        .to("infinispan:{{infinispan.client.hotrod.cache}}?cacheContainer=#cacheContainer")

            .setBody(constant(""))
            .choice()
                .when(constant(Boolean.TRUE).isEqualTo(proxyConfig.getDebugHeaders()))
                    .process(new DebugHeaderProcessor("X-RateLimit-Limit", simple(""+policyConfig.getMaxHitCount())))
                    .process(new DebugHeaderRemainingHitsProcessor("X-RateLimit-Remaining", policyConfig.getMaxHitCount(), simple("${exchangeProperty." + ApplicationEnum.HIT_COUNT_TOTAL.getValue() + "}")))
                    .process(new DebugHeaderProcessor("X-RateLimit-Time", simple(""+System.currentTimeMillis())))
                    .process(new DebugHeaderProcessor("X-RateLimit-Reset", simple("${header." + ApplicationEnum.HIT_BOUNDARY.getValue() + "}")))
                .endChoice()
            .end();
    }

    private static void sendRateLimitError(final Exchange exchange) {
        LOGGER.info(":: method: sendRateLimitError(final Exchange exchange) called");
        LOGGER.info("\t:: http.status.code=429");

        final Message message = exchange.getIn();
        message.setFault(true);
        message.setHeader(Exchange.HTTP_RESPONSE_CODE, 429);
        message.setBody("");
    }

    private static void serviceUnavailable(final Exchange exchange) {
        LOGGER.info(":: method: serviceUnavailable(final Exchange exchange) called");
        LOGGER.info("\t:: http.status.code=503");

        final Message message = exchange.getIn();
        message.setFault(true);
        message.setHeader(Exchange.HTTP_RESPONSE_CODE, 503);
        message.setBody("");
    }

}
