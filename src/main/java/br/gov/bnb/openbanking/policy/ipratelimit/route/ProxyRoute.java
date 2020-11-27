package br.gov.bnb.openbanking.policy.ipratelimit.route;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuração de rota que servirá como proxy para a custom policy
 * @author <a href="mailto:nramalho@redhat.com">Natanael Ramalho</a>
 */
@Component
public class ProxyRoute extends RouteBuilder {
	
	private static final Logger LOGGER = Logger.getLogger(ProxyRoute.class.getName());
	
	public static final String CLIENT_IP = "clientIp";

	public static final String EMPTY_XFORWARDEDFOR = "NO_IP";
 
	@Value("${custom.dev.env}")
	private  Boolean env;
	
	
	@Override
    public void configure() throws Exception {
        final RouteDefinition route =
                from("netty4-http:proxy://0.0.0.0:8088");
        createRoute(route);

        final RouteDefinition routeTLS =
                from("netty4-http:proxy://0.0.0.0:8443?ssl=true&keyStoreFile=keystore.jks&passphrase=changeit&trustStoreFile=keystore.jks");
        createRoute(routeTLS);
    }

    private void createRoute(RouteDefinition route) {
        route.process(ProxyRoute::saveHostHeader)
                .process(ProxyRoute::addCustomHeader)
                .toD("netty4-http:"
                        + "${headers." + Exchange.HTTP_SCHEME + "}://"
                        + "${headers." + Exchange.HTTP_HOST + "}:"
                        + "${headers." + Exchange.HTTP_PORT + "}"
                        + "${headers." + Exchange.HTTP_PATH + "}")
                .process(ProxyRoute::addCustomHeader);
    }

    private static void addCustomHeader(final Exchange exchange) {
        final Message message = exchange.getIn();
        final String body = message.getBody(String.class);
        System.out.println("HEADERS: " + message.getHeaders());
        message.setHeader("Fuse-Camel-Proxy", "Request was redirected to Camel netty4 proxy service");
        message.setBody(body);
		System.out.println(body);
		
		LOGGER.info("--------------------------------------------------------------------------------");
		LOGGER.info("PROXY FORWARDING TO " + message.getHeader(Exchange.HTTP_SCHEME) + "://"
				+ message.getHeader(Exchange.HTTP_HOST) + ":" + message.getHeader(Exchange.HTTP_PORT) + "/"
				+ message.getHeader(Exchange.HTTP_PATH));
		LOGGER.info("--------------------------------------------------------------------------------");
    }

    private static void saveHostHeader(final Exchange exchange) {
        final Message message = exchange.getIn();
        System.out.println("HEADERS: " + message.getHeaders());
        String hostHeader = message.getHeader("Host", String.class);
        message.setHeader("Source-Header", hostHeader);
    }

	@Deprecated
	private static void beforeRedirect(final Exchange exchange) {
		LOGGER.info("BEFORE REDIRECT");
		final Message message = exchange.getIn();
		Iterator<String> iName = message.getHeaders().keySet().iterator();

		LOGGER.info("header values:");
		while (iName.hasNext()) {
			String key = (String) iName.next();
			LOGGER.info("\t[" + key + "] - {" + message.getHeader(key) + "}");
		}

		// HttpServletRequest req = exchange.getIn().getBody(HttpServletRequest.class);
		InetSocketAddress remoteAddress = (InetSocketAddress) message.getHeader("CamelNettyRemoteAddress");

		LOGGER.info("");
		LOGGER.info("REQUEST REMOTE ADDRESS: " + remoteAddress.toString());
		LOGGER.info("REQUEST CANONICAL HOST NAME: " + remoteAddress.getAddress().getCanonicalHostName());
		LOGGER.info("REQUEST HOST ADDRESS: " + remoteAddress.getAddress().getHostAddress());
		LOGGER.info("REQUEST HOST NAME: " + remoteAddress.getAddress().getHostName());
		LOGGER.info("REQUEST ADDRESS: " + new String(remoteAddress.getAddress().getAddress(), StandardCharsets.UTF_8));
		LOGGER.info("REQUEST HOST NAME: " + remoteAddress.getHostName());

		// DESCOMENTAR PARA HABILITAR O USO DO DATAGRID
		// boolean isCanAccess = CacheRepository.isCanAccess(req.getRemoteAddr());
		// if(isCanAccess) {

		if (true) {
			LOGGER.info("");

			String host = (String) message.getHeader("CamelHttpHost");
			String path = (String) message.getHeader("CamelHttpPath");
			Integer port = (Integer) message.getHeader("CamelHttpPort");
			String scheme = (String) message.getHeader("CamelHttpScheme");

			LOGGER.info("REDIRECTING TO HTTP_HOST: " + host);
			LOGGER.info("REDIRECTING TO HTTP_PORT: " + port);
			LOGGER.info("REDIRECTING TO HTTP_PATH: " + path);
			LOGGER.info("REDIRECTING TO HTTP_SCHEME: " + scheme);

			/*
			 * if (host.indexOf(':') > -1) {
			 * LOGGER.info("\ttrimming port from host variable: " + host); host =
			 * remoteAddress.getHostName(); LOGGER.info("\tadjusted to: " + host); }
			 */

			LOGGER.info("--------------------------------------------------------------------------------");
			LOGGER.info("PROXY FORWARDING TO " + message.getHeader(Exchange.HTTP_SCHEME) + "://"
					+ message.getHeader(Exchange.HTTP_HOST) + ":" + message.getHeader(Exchange.HTTP_PORT) + "/"
					+ message.getHeader(Exchange.HTTP_PATH));
			LOGGER.info("--------------------------------------------------------------------------------");
		}

	}

}