package com.redhat.api.policy.route.internal;

import java.util.logging.Logger;

import com.redhat.api.policy.configuration.PolicyConfig;
import com.redhat.api.policy.configuration.SSLProxyConfig;
import com.redhat.api.policy.enumerator.ApplicationEnum;
import com.redhat.api.policy.exception.RateLimitException;
import com.redhat.api.policy.processor.*;

import com.redhat.api.policy.route.external.ProxyRoute;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.infinispan.InfinispanOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("policy-ip-rate-limit")
public class CacheRoute extends RouteBuilder {

    private static final Logger LOGGER = Logger.getLogger(ProxyRoute.class.getName());

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

        from("direct:getHitCount")
            .routeId("get-hit-count-route")

            .doTry()
                .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.GET))
                .setHeader(InfinispanConstants.KEY, simple("${exchangeProperty." + ApplicationEnum.CLIENT_IP + "}"))
                .log(":: request forwarded to datagrid :: PUT :: " + simple("${exchangeProperty." + ApplicationEnum.CLIENT_IP + "}"))
                .to("infinispan://{{infinispan.client.hotrod.cache}}?cacheContainer=#cacheContainer")
            .endDoTry()
            .doCatch(Exception.class)
                .log(":: Exception :: direct:getHitCount :: datagrid service unavailable")
                .process(CacheRoute::serviceUnavailable)
            .end()

            .doTry()
                .process(rateLimitProcessor)
            .endDoTry()
            .doCatch(RateLimitException.class)
                .wireTap("direct:incrementHitCount")
                .process(CacheRoute::sendRateLimitError)
            .end();

        // /--------------------------------------------------\
        // | evaluate and compute remote address hits         |
        // \--------------------------------------------------/

        from("direct:incrementHitCount")
            .routeId("increment-hit-count-route")
            .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.GET))
            .setHeader(InfinispanConstants.KEY, simple("${exchangeProperty." + ApplicationEnum.CLIENT_IP + "}-" + ApplicationEnum.HIT_TIMESTAMP))
            .log(":: request forwarded to datagrid :: GET :: " +  simple("${exchangeProperty." + ApplicationEnum.CLIENT_IP + "}-" + ApplicationEnum.HIT_TIMESTAMP))
                .to("infinispan:{{infinispan.client.hotrod.cache}}?cacheContainer=#cacheContainer")
                .process(rateLimitStorageProcessor)
            .multicast().parallelProcessing()
                .pipeline()
                    // TODO - implementar condicional para evitar atualização sem necessidade
                    .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.PUT))
                    .setHeader(InfinispanConstants.KEY, simple("${exchangeProperty." + ApplicationEnum.CLIENT_IP + "}-" + ApplicationEnum.HIT_TIMESTAMP))
                    .setHeader(InfinispanConstants.VALUE, simple("${header." + ApplicationEnum.HIT_TIMESTAMP + "}"))
                        .log(":: request forwarded to datagrid :: PUT :: " + simple("${exchangeProperty." + ApplicationEnum.CLIENT_IP + "}-" + ApplicationEnum.HIT_TIMESTAMP))
                        .to("infinispan:{{infinispan.client.hotrod.cache}}?cacheContainer=#cacheContainer")
                .pipeline()
                    .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.PUT))
                    .setHeader(InfinispanConstants.KEY, simple("${exchangeProperty." + ApplicationEnum.CLIENT_IP + "}"))
                    .setHeader(InfinispanConstants.VALUE, simple("${exchangeProperty." + ApplicationEnum.HIT_COUNT_TOTAL + "}"))
                        .log(":: request forwarded to datagrid :: PUT :: " + simple("${exchangeProperty." + ApplicationEnum.CLIENT_IP + "}"))
                        .to("infinispan:{{infinispan.client.hotrod.cache}}?cacheContainer=#cacheContainer")

            .setBody(constant(""))
            .choice()
                .when(constant(Boolean.TRUE).isEqualTo(proxyConfig.getDebugHeaders()))
                    .process(new DebugHeaderProcessor("X-RateLimit-Limit", simple(""+policyConfig.getMaxHitCount())))
                    .process(new DebugHeaderRemainingHitsProcessor("X-RateLimit-Remaining", policyConfig.getMaxHitCount(), simple("${exchangeProperty." + ApplicationEnum.HIT_COUNT_TOTAL + "}")))
                    .process(new DebugHeaderProcessor("X-RateLimit-Time", simple(""+System.currentTimeMillis())))
                    .process(new DebugHeaderProcessor("X-RateLimit-Reset", simple("${header." + ApplicationEnum.HIT_BOUNDARY + "}")))
                .endChoice()
            .end();
    }

    private static void sendRateLimitError(final Exchange exchange) {
        LOGGER.info("private static void sendRateLimitError(final Exchange exchange) called");
        LOGGER.info("\t:: http.status.code=429");

        final Message message = exchange.getIn();
        message.setFault(true);
        message.setHeader(Exchange.HTTP_RESPONSE_CODE, 429);
        message.setBody("");
    }

    private static void serviceUnavailable(final Exchange exchange) {
        LOGGER.info("private static void serviceUnavailable(final Exchange exchange) called");
        LOGGER.info("\t:: http.status.code=503");

        final Message message = exchange.getIn();
        message.setFault(true);
        message.setHeader(Exchange.HTTP_RESPONSE_CODE, 503);
        message.setBody("");
    }

}
