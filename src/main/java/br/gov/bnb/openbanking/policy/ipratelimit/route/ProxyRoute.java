package br.gov.bnb.openbanking.policy.ipratelimit.route;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.camel.util.jsse.TrustManagersParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.gov.bnb.openbanking.policy.ipratelimit.exception.RateLimitException;

/**
 * Configuração de rota que servirá como proxy para a custom policy
 * @author <a href="mailto:nramalho@redhat.com">Natanael Ramalho</a>
 */
@Component
public class ProxyRoute extends RouteBuilder {
	
	private static final Logger LOGGER = Logger.getLogger(ProxyRoute.class.getName());
	
	public static final String CLIENT_IP = "clientIp";
 
	@Value("${custom.dev.env}")
	private  Boolean env;
	

	@Override
	public void configure() throws Exception {
		ArrayList<String> ipList = new ArrayList<String>();
		if(!env){
			configureHttp4();
		}else{
			ipList.add("10.6.128.23");
			ipList.add("200.164.107.55");
		}
		
		from("netty4-http:proxy://0.0.0.0:9090/?bridgeEndpoint=true&throwExceptionOnFailure=false")
			.to("direct:internal-redirect");

		from("direct:internal-redirect")
			.doTry()
				// habilitar essa linha para teste em ambiente de desenvolvimento
				//.setHeader("X-Forwarded-For",constant(ipList))
				.process(ProxyRoute::beforeRedirect)
				.process(ProxyRoute::clientIpFilter)
				.to("direct:getHitCount")
				.wireTap("direct:incrementHitCount")
				.toD("https4://" 
					+  "${headers." + Exchange.HTTP_HOST + "}" + ":" 
					+ "${headers." + Exchange.HTTP_PORT + "}"
					+ "?bridgeEndpoint=true&throwExceptionOnFailure=false")
				.process(ProxyRoute::uppercase).process((e) -> {
					LOGGER.info(">>> request forwarded to backend");
				})
				
			.endDoTry()
			.doCatch(RateLimitException.class)
				.wireTap("direct:incrementHitCount")
				.process(ProxyRoute::sendRateLimitErro)
				.process((e) -> {
					LOGGER.info(">>> After Exception Method");
				})
		  	.end();
	}

	private void configureHttp4() {
		KeyStoreParameters ksp = new KeyStoreParameters();
		ksp.setResource(this.getClass().getProtectionDomain().getCodeSource().getLocation() + "keystore/bnb.jks");
		ksp.setPassword("changeit");
		TrustManagersParameters tmp = new TrustManagersParameters();
		tmp.setKeyStore(ksp);
		SSLContextParameters scp = new SSLContextParameters();
		scp.setTrustManagers(tmp);
		HttpComponent httpComponent = getContext().getComponent("https4", HttpComponent.class);
		httpComponent.setSslContextParameters(scp);
	}

	public static void uppercase(final Exchange exchange) {
		final Message message = exchange.getIn();
		final String body = message.getBody(String.class);
		message.setBody(body.toUpperCase(Locale.US));
	}

	private static void sendRateLimitErro(final Exchange exchange) {
    	LOGGER.info("SEND COD ERROR 429");
    	final Message message = exchange.getIn();
		message.setHeader(Exchange.HTTP_RESPONSE_CODE,429);
		message.setBody("");
	}

	private static void clientIpFilter(final Exchange exchange){
		ArrayList<String> ipList = (ArrayList<String>) exchange.getIn().getHeader("X-Forwarded-For");
		if (ipList != null) {
			exchange.setProperty(ProxyRoute.CLIENT_IP, ipList.get(0));
		}else{
			//TODO - Tratamentro de erro para o caso de não existir o parâmetro X-Forwarded-For no header
		}
	}

	private static void beforeRedirect(final Exchange exchange) {
		LOGGER.info("BEFORE REDIRECT");
		final Message message = exchange.getIn();
		Iterator<String> iName = message.getHeaders().keySet().iterator();

		exchange.setProperty("ipList", exchange.getIn().getHeader("X-Forwarded-For"));
		LOGGER.info(exchange.getProperty("ipList", List.class).get(0).toString());

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