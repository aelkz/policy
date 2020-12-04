package com.redhat.api.policy.ipratelimit.route;

import java.util.ArrayList;
import java.util.logging.Logger;

import com.redhat.api.policy.ipratelimit.exception.RateLimitException;
import com.redhat.api.policy.ipratelimit.processor.JeagerTagProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.camel.util.jsse.TrustManagersParameters;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuração de rota que servirá como proxy para a custom policy
 *
 * @author <a href="mailto:nramalho@redhat.com">Natanael Ramalho</a>
 */
@Component
public class ProxyRoute extends RouteBuilder {

    static final String CLIENT_IP = "clientIp";
    private static final Logger LOGGER = Logger.getLogger(ProxyRoute.class.getName());
    private static final String EMPTY_XFORWARDEDFOR = "NO_IP";

    @Value("${custom.dev.env}")
    private Boolean env;

    @Override
    public void configure() throws Exception {

        if (!env) {
            configureHttp4();
            from("netty4-http:proxy://0.0.0.0:8443?ssl=true&keyStoreFile=keystore.jks&passphrase=changeit&trustStoreFile=keystore.jks")
                .id("from-netty-tls")
                //.process(ProxyRoute::saveHostHeader)
                //.process(ProxyRoute::addCustomHeader)
                .to("direct:internal-redirect");
        } else {
            ArrayList<String> ipList = new ArrayList<String>();
            ipList.add("10.6.128.23");
            ipList.add("200.164.107.55");

            from("netty4-http:proxy://0.0.0.0:8088/?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .id("from-netty-no-tls")
                .setHeader("X-Forwarded-For", constant(ipList))
                .to("direct:internal-redirect");
        }

        from("direct:internal-redirect").id("proxy:internal-redirect")
            .doTry()
                .process(ProxyRoute::clientIpFilter).id("proxy:clietIp-discovery")
                .to("direct:getHitCount").id("rhdg:get-hit-count")
                .wireTap("direct:incrementHitCount").id("rhdg:process-hit-count")
                .toD("https4://"
                    + "${headers." + Exchange.HTTP_HOST + "}" + ":"
                    + "${headers." + Exchange.HTTP_PORT + "}"
                    + "?bridgeEndpoint=true&throwExceptionOnFailure=false").id("proxy:endpoint-request")
                .log(":: request forwarded to backend")
                .process(new JeagerTagProcessor("X-Forwarded-For", simple("${header.X-Forwarded-For}"))).id("opentracing:before-endpoint-request")
            .endDoTry()
            .doCatch(RateLimitException.class)
                .wireTap("direct:incrementHitCount")
                .process(ProxyRoute::sendRateLimitError)
            .end();
    }

    private void configureHttp4() {
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource("keystore.jks");
        ksp.setPassword("changeit");

        TrustManagersParameters tmp = new TrustManagersParameters();
        tmp.setKeyStore(ksp);

        SSLContextParameters scp = new SSLContextParameters();
        scp.setTrustManagers(tmp);

        HttpComponent httpComponent = getContext().getComponent("https4", HttpComponent.class);
        httpComponent.setSslContextParameters(scp);
        httpComponent.setX509HostnameVerifier(new NoopHostnameVerifier());
    }

    /**
     * Método responsável por recuperar lista de IPs que identificam o cliente
     * @param exchange
     */
    private static void clientIpFilter(final Exchange exchange) {
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
            ips = ProxyRoute.EMPTY_XFORWARDEDFOR;
        }
        exchange.setProperty(ProxyRoute.CLIENT_IP, ips);
    }

    private static void sendRateLimitError(final Exchange exchange) {
        LOGGER.info("private static void sendRateLimitError(final Exchange exchange) called");

        LOGGER.info(":: http.status.code=429");
        final Message message = exchange.getIn();
        message.setHeader(Exchange.HTTP_RESPONSE_CODE, 429);
        message.setBody("");
    }

    private static void addCustomHeader(final Exchange exchange) {
        final Message message = exchange.getIn();
		final String body = message.getBody(String.class);

		message.setHeader("Fuse-Camel-Proxy", "Request was redirected to Camel netty4 proxy service");
		System.out.println(">>> CUSTOM HEADER: " + message.getHeaders());
        message.setBody(body);
        System.out.println(body);
	}
	
    private static void saveHostHeader(final Exchange exchange) {
		
        final Message message = exchange.getIn();
        System.out.println(">>> SAVE HEADER : " + message.getHeaders());
        String hostHeader = message.getHeader("Host", String.class);
        message.setHeader("Source-Header", hostHeader);
    }

}
