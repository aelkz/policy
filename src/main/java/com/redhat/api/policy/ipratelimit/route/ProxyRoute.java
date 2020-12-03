package com.redhat.api.policy.ipratelimit.route;

import java.util.ArrayList;
import java.util.logging.Logger;

import com.redhat.api.policy.ipratelimit.exception.RateLimitException;
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

    private static final Logger LOGGER = Logger.getLogger(ProxyRoute.class.getName());

    public static final String CLIENT_IP = "clientIp";
    public static final String EMPTY_XFORWARDEDFOR = "NO_IP";

    @Value("${custom.dev.env}")
    private Boolean env;

    @Override
    public void configure() throws Exception {

        if (!env) {
            configureHttp4();
        } else {
            from("netty4-http:proxy://0.0.0.0:8080/?bridgeEndpoint=true&throwExceptionOnFailure=false")
				.to("direct:internal-redirect");
		}

        from("netty4-http:proxy://0.0.0.0:8443?ssl=true&keyStoreFile=keystore.jks&passphrase=changeit&trustStoreFile=keystore.jks")
			.to("direct:internal-redirect");

        from("direct:internal-redirect")
			.doTry()
				.process(ProxyRoute::saveHostHeader)
				.process(ProxyRoute::addCustomHeader)
				.process(ProxyRoute::clientIpFilter)
				.to("direct:getHitCount")
				.wireTap("direct:incrementHitCount")
				.toD("https4://"
					+ "${headers." + Exchange.HTTP_HOST + "}" + ":"
					+ "${headers." + Exchange.HTTP_PORT + "}"
					+ "?bridgeEndpoint=true&throwExceptionOnFailure=false")
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

    /*
     * Método responsável por recuperar lista de IPs que identificam o cliente
     * @param exchange
     */
    private static void clientIpFilter(final Exchange exchange) {
        ArrayList<String> ipList = (ArrayList<String>) exchange.getIn().getHeader("X-Forwarded-For");
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
        LOGGER.info("private static void addCustomHeader(final Exchange exchange) called");

        final Message message = exchange.getIn();
        final String body = message.getBody(String.class);

        ArrayList<String> ipList = (ArrayList<String>) exchange.getIn().getHeader("X-Forwarded-For");
        LOGGER.info(":: CUSTOM_HEADER.IP_LIST=" + ipList.toString());

        String ips = new String("");
        for (String ip : ipList) {
            LOGGER.info("\t:: " + ip);
            ips = ips.concat(ip).concat(":");
        }

        LOGGER.info(":: " + ips);

        message.setHeader("Fuse-Camel-Proxy", "Request was redirected to Camel netty4 proxy");
        LOGGER.info("" + message.getHeaders());

        message.setBody(body);
        LOGGER.info(body);
    }

    private static void saveHostHeader(final Exchange exchange) {
        LOGGER.info("private static void saveHostHeader(final Exchange exchange) called");

        ArrayList<String> ipList = (ArrayList<String>) exchange.getIn().getHeader("X-Forwarded-For");
        System.out.println(ipList.getClass());

        LOGGER.info(":: " + ipList.toString());
        String ips = new String("");

        for (String ip : ipList) {
            LOGGER.info("\t:: " + ip);
            ips = ips.concat(ip).concat(":");
        }

        LOGGER.info(":: " + ips);
        final Message message = exchange.getIn();
        LOGGER.info(":: " + message.getHeaders());
        String hostHeader = message.getHeader("Host", String.class);
        message.setHeader("Source-Header", hostHeader);
    }

}
