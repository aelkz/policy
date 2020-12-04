package com.redhat.api.policy.ipratelimit.route;

import java.util.logging.Logger;

import com.redhat.api.policy.ipratelimit.exception.RateLimitException;
import com.redhat.api.policy.ipratelimit.processor.RateLimitProcessor;
import com.redhat.api.policy.ipratelimit.processor.RateLimitStorageProcessor;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.infinispan.InfinispanOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Configuração de rota responsável pelo gerenciamento de valores no Red Hat Data Grid
 *
 * @author <a href="mailto:nramalho@redhat.com">Natanael Ramalho</a>
 */
@Component
public class CacheRoute extends RouteBuilder {

    private static final Logger LOGGER = Logger.getLogger(ProxyRoute.class.getName());

    public static final String HIT_COUNT = "hitCount";
    public static final String HIT_TIMESTAMP = "hitTimeStamp";
    public static final String HIT_COUNT_TOTAL = "hitCountTotal";

    @Autowired
    private RateLimitProcessor rateLimitProcessor;

    @Autowired
    private RateLimitStorageProcessor rateLimitStorageProcessor;

    @Override
    public void configure() throws Exception {

        from("direct:getHitCount")
            .routeId("get-hit-count-route")
            .doTry()
                .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.GET))
                .setHeader(InfinispanConstants.KEY, simple("${exchangeProperty." + ProxyRoute.CLIENT_IP + "}"))
                .log(":: request forwarded to datagrid :: PUT KEY :: " + simple("${exchangeProperty." + ProxyRoute.CLIENT_IP + "}"))
                .to("infinispan://{{custom.rhdg.cache.name}}?cacheContainer=#cacheContainer")
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

        from("direct:incrementHitCount")
            .routeId("increment-hit-count-route")
            .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.GET))
            .setHeader(InfinispanConstants.KEY, simple("${exchangeProperty." + ProxyRoute.CLIENT_IP + "}-" + CacheRoute.HIT_TIMESTAMP))
            .log(":: request forwarded to datagrid :: GET KEY " +  simple("${exchangeProperty." + ProxyRoute.CLIENT_IP + "}-" + CacheRoute.HIT_TIMESTAMP))
            .to("infinispan:{{custom.rhdg.cache.name}}?cacheContainer=#cacheContainer")
            .process(rateLimitStorageProcessor)
            // TODO - implementar condicional para evitar atualização sem necessidade
            .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.PUT))
            .setHeader(InfinispanConstants.KEY, simple("${exchangeProperty." + ProxyRoute.CLIENT_IP + "}-" + CacheRoute.HIT_TIMESTAMP))
            .setHeader(InfinispanConstants.VALUE, simple("${header." + CacheRoute.HIT_TIMESTAMP + "}"))
            .log(":: request forwarded to datagrid :: PUT KEY " + simple("${exchangeProperty." + ProxyRoute.CLIENT_IP + "}-" + CacheRoute.HIT_TIMESTAMP))
            .to("infinispan:{{custom.rhdg.cache.name}}?cacheContainer=#cacheContainer")
            .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.PUT))
            .setHeader(InfinispanConstants.KEY, simple("${exchangeProperty." + ProxyRoute.CLIENT_IP + "}"))
            .setHeader(InfinispanConstants.VALUE, simple("${exchangeProperty." + CacheRoute.HIT_COUNT_TOTAL + "}"))
            .log(":: request forwarded to datagrid :: PUT KEY " + simple("${exchangeProperty." + ProxyRoute.CLIENT_IP + "}"))
            .to("infinispan:{{custom.rhdg.cache.name}}?cacheContainer=#cacheContainer")
            .setBody(constant(""));
    }

    private static void sendRateLimitError(final Exchange exchange) {
        LOGGER.info("private static void sendRateLimitError(final Exchange exchange) called");

        LOGGER.info(":: http.status.code=429");
        final Message message = exchange.getIn();
        message.setFault(true);
        message.setHeader(Exchange.HTTP_RESPONSE_CODE, 429);
        message.setBody("");
    }

    private static void serviceUnavailable(final Exchange exchange) {
        LOGGER.info("private static void serviceUnavailable(final Exchange exchange) called");

        LOGGER.info(":: http.status.code=503");
        final Message message = exchange.getIn();
        message.setFault(true);
        message.setHeader(Exchange.HTTP_RESPONSE_CODE, 503);
        message.setBody("");
    }

}
