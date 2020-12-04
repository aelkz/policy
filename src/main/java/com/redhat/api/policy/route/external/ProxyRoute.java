package com.redhat.api.policy.route.external;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import com.redhat.api.policy.configuration.PolicyConfig;
import com.redhat.api.policy.configuration.SSLProxyConfig;
import com.redhat.api.policy.enumerator.ApplicationEnum;
import com.redhat.api.policy.processor.JeagerTagProcessor;
import org.apache.camel.Exchange;
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

@Component("proxy")
public class ProxyRoute extends RouteBuilder {

    private static final Logger LOGGER = Logger.getLogger(ProxyRoute.class.getName());

    @Autowired
    private PolicyConfig policyConfig;

    @Autowired
    private SSLProxyConfig proxyConfig;

    @Override
    public void configure() throws Exception {

        final RouteDefinition from;

        if (proxyConfig.isSecured()) {
            configureHttp4();

            from = from(proxyConfig.getConsumer()
                +":proxy://0.0.0.0:"
                + proxyConfig.getPort()
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
                + proxyConfig.getPort())
                .id("from-"+ proxyConfig.getConsumer()+"-http")
                .log(":: "+ proxyConfig.getConsumer() + " http headers:");
        }

        if (policyConfig.getIpWhitelist() != null && !"".equals(policyConfig.getIpWhitelist())) {
            ArrayList<String> ipList = new ArrayList<String>();
            ipList = new ArrayList<String>(Arrays.asList(policyConfig.getIpWhitelist().split(",")));
            from.setHeader("X-Forwarded-For", constant(ipList)).to("direct:internal-redirect");
        }else {
            from.to("direct:internal-redirect");
        }

        from("direct:internal-redirect").id("proxy-internal-redirect")
            .process(ProxyRoute::remoteAddressFilter).id("proxy-client-ip-discovery")
            .to("direct:getHitCount").id("datagrid-get-hit-count")
            .wireTap("direct:incrementHitCount").id("datagrid-evaluate-hit-count")
            .doTry()
                .toD(proxyConfig.getProducer()+":"
                    + ("http4".equals(proxyConfig.getProducer()) ? "//" : "${header." + Exchange.HTTP_SCHEME + "}://")
                    + "${headers." + Exchange.HTTP_HOST + "}" + ":"
                    + "${headers." + Exchange.HTTP_PORT + "}"
                    + "${header." + Exchange.HTTP_PATH + "}"
                    + proxyConfig.getProducerQuery())
                    .id("proxy-endpoint-request")
                .log(":: request forwarded to backend")
                .process(new JeagerTagProcessor("X-Forwarded-For", simple("${header.X-Forwarded-For}"))).id("opentracing:before-endpoint-request")
            .endDoTry()
            .doCatch(Exception.class)
                .log(":: Exception :: direct-internal-redirect :: upstream service unavailable")
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
        LOGGER.info("private static void clientIpFilter(final Exchange exchange) called");

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

        if (ipList == null) {
            ips = ApplicationEnum.EMPTY_X_FORWARDED_FOR.getValue();
        }

        exchange.setProperty(ApplicationEnum.CLIENT_IP.getValue(), ips);
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
