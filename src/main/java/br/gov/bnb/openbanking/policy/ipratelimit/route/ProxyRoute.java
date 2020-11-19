package br.gov.bnb.openbanking.policy.ipratelimit.route;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Logger;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

/**
 * A simple Camel REST DSL route that implement the greetings service.
 * 
 */
@Component
public class ProxyRoute extends RouteBuilder {
	private static final Logger LOGGER = Logger.getLogger(ProxyRoute.class.getName());

    @Override
    public void configure() throws Exception {
		
		from("netty4-http:proxy://0.0.0.0:8080/?bridgeEndpoint=true&throwExceptionOnFailure=false")
			.process((e) -> {
				System.out.println("\n:: proxy received\n");
			})
			// &httpClient.redirectsEnabled=true
			
			.to("direct:internal-redirect")
			.process((e) -> {
				System.out.println("\n:: route processing ended\n");
			});
		
		from("direct:internal-redirect")
			.process((e) -> {
				System.out.println("\n:: internal-rest received\n");
			})
			.process(ProxyRoute::beforeRedirect)
			.toD("https4://"
                + "${headers." + Exchange.HTTP_HOST + "}:"
				+ "${headers." + Exchange.HTTP_PORT + "}"
				+ "?bridgeEndpoint=true&throwExceptionOnFailure=false")
			.process(ProxyRoute::uppercase)
			.process((e) -> {
				System.out.println(":: request forwarded to backend");
		});		
	}
	

	public static void uppercase(final Exchange exchange) {
		final Message message = exchange.getIn();
		final String body = message.getBody(String.class);
		message.setBody(body.toUpperCase(Locale.US));
	}
		
	private static void beforeRedirect(final Exchange exchange) {
		LOGGER.info("BEFORE REDIRECT");
		final Message message = exchange.getIn();
		Iterator<String> iName = message.getHeaders().keySet().iterator();

		LOGGER.info("header values:");
		while(iName.hasNext()) {
			String key = (String) iName.next();
			LOGGER.info("\t[" +key+ "] - {"+message.getHeader(key)+"}");
		}

		// HttpServletRequest req = exchange.getIn().getBody(HttpServletRequest.class);
		InetSocketAddress remoteAddress = (InetSocketAddress)message.getHeader("CamelNettyRemoteAddress");

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

		if(true) {
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
			if (host.indexOf(':') > -1) {
				LOGGER.info("\ttrimming port from host variable: " + host);
				host = 	remoteAddress.getHostName();
				LOGGER.info("\tadjusted to: " + host);
			}
			*/

			LOGGER.info("--------------------------------------------------------------------------------");
			LOGGER.info("PROXY FORWARDING TO "
					+ message.getHeader(Exchange.HTTP_SCHEME)
					+ message.getHeader(Exchange.HTTP_HOST)
					+ ":" + message.getHeader(Exchange.HTTP_PORT)
					+ message.getHeader(Exchange.HTTP_PATH));
			LOGGER.info("--------------------------------------------------------------------------------");
		}

	}

}