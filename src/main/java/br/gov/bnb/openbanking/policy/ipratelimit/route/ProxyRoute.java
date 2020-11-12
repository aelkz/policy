package br.gov.bnb.openbanking.policy.ipratelimit.route;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.util.jsse.KeyManagersParameters;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import br.gov.bnb.openbanking.policy.ipratelimit.exception.RateLimitException;

@Component("ip-rate-limit")
public class ProxyRoute extends RouteBuilder {
	private static final Logger LOGGER = Logger.getLogger(ProxyRoute.class.getName());

    @Override
    public void configure() throws Exception {

		final RouteDefinition from;
		// from = from("jetty://http://0.0.0.0:8080?useXForwardedForHeader=true&matchOnUriPrefix=true")
		// from = from("jetty://https://0.0.0.0:8443?useXForwardedForHeader=true&matchOnUriPrefix=true");
		from = from("netty4-http:proxy://0.0.0.0:8080");

		from
		.doTry()
			.process((e) -> {
				System.out.println(">>> beforeRedirect method");
			})
            .process(ProxyRoute::beforeRedirect)
				.process((e) -> {
					System.out.println(">>> forwarding request to backend");
				})
			.toD("netty4-http:"
			    + "${header." + Exchange.HTTP_SCHEME + "}://"
				+ "${header." + Exchange.HTTP_HOST + "}:"
				+ "${header." + Exchange.HTTP_PORT + "}"
				+ "${header." + Exchange.HTTP_PATH + "}"
				+ "?ssl=true&sslContextParameters=#sslContextParameters"
			)
			.process((e) -> {
				System.out.println(">>> afterRedirect method");
			})
            .process(ProxyRoute::afterRedirect)
		.endDoTry()
        	.doCatch(RateLimitException.class)
			.process((e) -> {
				System.out.println(">>> afterRedirect method");
			})
		    .process(ProxyRoute::sendRateLimitErro)
		  .end();
    }

    private static void beforeRedirect(final Exchange exchange) throws RateLimitException {
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

			if (host.indexOf(':') > -1) {
				LOGGER.info("\ttrimming port from host variable: " + host);
				host = 	remoteAddress.getHostName();
				LOGGER.info("\tadjusted to: " + host);
			}

			message.setHeader(Exchange.HTTP_HOST, host);
			message.setHeader(Exchange.HTTP_PORT, port);
			message.setHeader(Exchange.HTTP_PATH, path);
			message.setHeader(Exchange.HTTP_SCHEME, scheme);

			LOGGER.info("--------------------------------------------------------------------------------");
			LOGGER.info("PROXY FORWARDING TO "
					+ message.getHeader(Exchange.HTTP_SCHEME)
					+ message.getHeader(Exchange.HTTP_HOST)
					+ ":" + message.getHeader(Exchange.HTTP_PORT)
					+ message.getHeader(Exchange.HTTP_PATH));
			LOGGER.info("--------------------------------------------------------------------------------");

       }else {
			LOGGER.info(">>>> RATE LIMIT REACHED FOR IP "+ remoteAddress.getHostName());
			throw new RateLimitException(">>>> RATE LIMIT REACHED FOR IP "+ remoteAddress.getHostName() );
       }
    }
    
    private static void afterRedirect(final Exchange exchange) {
    	LOGGER.info("AFTER REDIRECT ");
    	// final Message message = exchange.getIn();
        // final String body = message.getBody(String.class);
    }
    
    private static void sendRateLimitErro(final Exchange exchange) {
    	LOGGER.info("SEND COD ERROR 429");
    	final Message message = exchange.getIn();
    	message.setHeader(Exchange.HTTP_RESPONSE_CODE,429);
    }

	@Bean(name = "sslContextParameters")
	public SSLContextParameters sslParameters() throws KeyManagementException, GeneralSecurityException, IOException {

		KeyStoreParameters ksp = new KeyStoreParameters();
		ksp.setResource(this.getClass().getProtectionDomain().getCodeSource().getLocation()+"keystore/truststore-postman.jks");
		ksp.setPassword("password");

		KeyManagersParameters kmp = new KeyManagersParameters();
		kmp.setKeyStore(ksp);
		kmp.setKeyPassword("password");

		SSLContextParameters scp = new SSLContextParameters();
		scp.setKeyManagers(kmp);

		SSLContextBuilder builder = new SSLContextBuilder();
		builder.loadTrustMaterial(new TrustSelfSignedStrategy());
		SSLContext sslcontext = builder.build();
		scp.createSSLContext().setDefault(sslcontext);

		return scp;
	}
}
