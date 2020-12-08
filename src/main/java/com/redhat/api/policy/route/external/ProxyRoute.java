package com.redhat.api.policy.route.external;

import java.util.ArrayList;
import java.util.Arrays;
import com.redhat.api.policy.configuration.PolicyConfig;
import com.redhat.api.policy.configuration.ProxyConfig;
import com.redhat.api.policy.enumerator.ApplicationEnum;
import com.redhat.api.policy.processor.debug.HeadersDebugProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.util.jsse.KeyManagersParameters;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.camel.util.jsse.TrustManagersParameters;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("proxy")
public class ProxyRoute extends RouteBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRoute.class);

    @Autowired
    private PolicyConfig policyConfig;

    @Autowired
    private ProxyConfig proxyConfig;

    @Autowired
    private HeadersDebugProcessor headersDebug;

    @Override
    public void configure() throws Exception {

        final RouteDefinition from;

        boolean secured =
        (proxyConfig.getKeystoreDest() != null && proxyConfig.getKeystorePass() != null)
                && (!"".equals(proxyConfig.getKeystoreDest()) && !"".equals(proxyConfig.getKeystorePass()));

        if (secured) {
            configureHttp4();

            from = from(proxyConfig.getConsumer()
                +":proxy://0.0.0.0:"
                + proxyConfig.getHttpsPort()
                + "?ssl=true&keyStoreFile="
                + proxyConfig.getKeystoreDest()
                + "&passphrase="
                + proxyConfig.getKeystorePass()
                + "&trustStoreFile="
                + proxyConfig.getKeystoreDest())
                .id("from-"+ proxyConfig.getConsumer()+"-https")
                .log(":: "+ proxyConfig.getConsumer() + " http headers:");
        } else {
            from = from(proxyConfig.getConsumer()
                +":proxy://0.0.0.0:"
                + proxyConfig.getHttpPort())
                .id("from-"+ proxyConfig.getConsumer()+"-http")
                .log(":: "+ proxyConfig.getConsumer() + " http headers:");
        }

        if (proxyConfig.debug()) {
            from.process(headersDebug);
        }

        if (policyConfig.getxForwardedFor() != null && !"".equals(policyConfig.getxForwardedFor().trim())) {
            ArrayList<String> ipList = new ArrayList<String>();
            ipList = new ArrayList<String>(Arrays.asList(policyConfig.getxForwardedFor().split(",")));
            from.log(LoggingLevel.WARN, LOGGER, "x-forwarded-for env set");
            from.log(LoggingLevel.WARN, LOGGER, "\t:: remote addresses: " + ipList);
            from.setHeader("X-Forwarded-For", constant(ipList)).to("direct:internal-redirect");
        }else {
            from.log(LoggingLevel.INFO, LOGGER, "x-forwarded-for env unset");
            from.to("direct:internal-redirect");
        }

        /**
         * known exceptions:
         * 1- RateLimitException
         * 2- Exception (infinispan unavailable)
         * 3- Exception (upstream unavailable)
         */
        from("direct:internal-redirect")
            .choice()
                .when(constant(Boolean.TRUE).isEqualTo(proxyConfig.debug()))
                .process(headersDebug)
            .endChoice()
            .end()
            .process(ProxyRoute::remoteAddressFilter)
            .to("direct:policy")
            .wireTap("direct:increment-hit-count")
            .doTry()
                .toD(proxyConfig.getProducer()+":"
                    + ("http4".equals(proxyConfig.getProducer()) ? "//" : "${header." + Exchange.HTTP_SCHEME + "}://")
                    + "${headers." + Exchange.HTTP_HOST + "}" + ":"
                    + "${headers." + Exchange.HTTP_PORT + "}"
                    + "${header." + Exchange.HTTP_PATH + "}"
                    + proxyConfig.getProducerQuery())
                .log(":: request forwarded to backend")
            .endDoTry()
            .doCatch(Exception.class)
                .log( LoggingLevel.ERROR, LOGGER, ":: Exception :: direct-internal-redirect :: upstream service unavailable")
                .process(ProxyRoute::serviceUnavailable)
            .end();
    }

    private void configureHttp4() {
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource(proxyConfig.getKeystoreDest());
        ksp.setPassword(proxyConfig.getKeystorePass());

        KeyManagersParameters keyManagersParameters = new KeyManagersParameters();
        keyManagersParameters.setKeyStore(ksp);
        keyManagersParameters.setKeyPassword(proxyConfig.getKeystorePass());

        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        trustManagersParameters.setKeyStore(ksp);

        SSLContextParameters scp = new SSLContextParameters();
        scp.setKeyManagers(keyManagersParameters);
        scp.setTrustManagers(trustManagersParameters);

        HttpComponent httpComponent = getContext().getComponent(proxyConfig.getProducer(), HttpComponent.class);
        httpComponent.setSslContextParameters(scp);
        //This is important to make your cert skip CN/Hostname checks
        httpComponent.setX509HostnameVerifier(new NoopHostnameVerifier());
    }

    private static void remoteAddressFilter(final Exchange exchange) {
        LOGGER.info(":: method: clientIpFilter(final Exchange exchange) called");

        Object xForwardedFor = exchange.getIn().getHeader("X-Forwarded-For");
        ArrayList<String> ipList = new ArrayList<String>();

        if (xForwardedFor instanceof String) {
            ipList.add((String) xForwardedFor);
        } else {
            ipList = (ArrayList<String>) xForwardedFor;
        }

        String ips = new String("");
        for (String ip : ipList) {
            ips = ips.concat(ip).concat(":");
        }

        if (ipList.isEmpty()) {
            ips = ApplicationEnum.EMPTY_X_FORWARDED_FOR.getValue();
        } else {
          ips = ips.substring(0, ips.length()-1);
        }

        LOGGER.info("\t:: "+ApplicationEnum.CLIENT_IP.getValue() + " -> " + ips);
        exchange.setProperty(ApplicationEnum.CLIENT_IP.getValue(), ips);
    }

    private static void serviceUnavailable(final Exchange exchange) {
        LOGGER.info(":: method: serviceUnavailable(final Exchange exchange) called");
        LOGGER.error("\t:: http.status.code=503");

        final Message message = exchange.getIn();

        final Throwable ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
        if (ex != null) {
            LOGGER.error(ex.getMessage());
            LOGGER.error(ex.getLocalizedMessage());
        }

        message.setFault(true);
        message.setHeader(Exchange.HTTP_RESPONSE_CODE, 503);
        message.setBody("");
    }

}
