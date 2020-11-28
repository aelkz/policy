package br.gov.bnb.openbanking.policy.ipratelimit.route;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Logger;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.stereotype.Component;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.util.jsse.KeyManagersParameters;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.TrustManagersParameters;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;

import br.gov.bnb.openbanking.policy.ipratelimit.exception.RateLimitException;

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
		setupSSLConext();
		if(!env){
			//setupSSLConext();
		}else {
			from("netty4-http:proxy://0.0.0.0:8080/?bridgeEndpoint=true&throwExceptionOnFailure=false")
				.to("direct:internal-redirect");
		}

		from("netty4-http:proxy://0.0.0.0:8443?ssl=true&keyStoreFile=keystore.jks&passphrase=changeit&trustStoreFile=keystore.jks")
			.to("direct:internal-redirect");
		

		from("direct:internal-redirect")
			.doTry()
				//.process(ProxyRoute::beforeRedirect)
				.process(ProxyRoute::saveHostHeader)
            	.process(ProxyRoute::addCustomHeader)
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
		  	.end();
	}

	private void setupSSLConext() throws Exception {
		LOGGER.info(">>> CÓDIGO NOVO DO RAPHAEL <<<");
        KeyStoreParameters keyStoreParameters = new KeyStoreParameters();
        // Change this path to point to your truststore/keystore as jks files
        keyStoreParameters.setResource("keystore.jks");
        keyStoreParameters.setPassword("changeit");

        KeyManagersParameters keyManagersParameters = new KeyManagersParameters();
        keyManagersParameters.setKeyStore(keyStoreParameters);
        keyManagersParameters.setKeyPassword("changeit");

        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        trustManagersParameters.setKeyStore(keyStoreParameters);

        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setKeyManagers(keyManagersParameters);
        sslContextParameters.setTrustManagers(trustManagersParameters);

        HttpComponent httpComponent = getContext().getComponent("https4", HttpComponent.class);
        httpComponent.setSslContextParameters(sslContextParameters);
        //This is important to make your cert skip CN/Hostname checks
        httpComponent.setX509HostnameVerifier(new AllowAllHostnameVerifier());

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
	}

	/**
	 * Método responsável por recuperar lista de IPs que identificam o cliente
	 * @param exchange
	 */
	private static void clientIpFilter(final Exchange exchange){
		ArrayList<String> ipList = (ArrayList<String>) exchange.getIn().getHeader("X-Forwarded-For");
		String ips = new String("");
		for(String ip : ipList){
			ips =  ips.concat(ip).concat(":");
		}
		if (ipList == null) {
			ips = ProxyRoute.EMPTY_XFORWARDEDFOR;
		}
		exchange.setProperty(ProxyRoute.CLIENT_IP, ips);
		
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

	private static void addCustomHeader(final Exchange exchange) {
        final Message message = exchange.getIn();
		final String body = message.getBody(String.class);
		ArrayList<String> ipList = (ArrayList<String>) exchange.getIn().getHeader("X-Forwarded-For");
		System.out.println(">>> CUSTOM HEADER >>> IPLIST: " + ipList.toString());
		String ips = new String("");
		for(String ip : ipList){
			System.out.println(">>> CUSTOM HEADER >>> IP: " + ip);
			ips =  ips.concat(ip).concat(":");
		}
		System.out.println(">>> CUSTOM HEADER >>> IP: " + ips);
		message.setHeader("Fuse-Camel-Proxy", "Request was redirected to Camel netty4 proxy service");
		System.out.println(">>> CUSTOM HEADER: " + message.getHeaders());
        message.setBody(body);
        System.out.println(body);
	}
	
    private static void saveHostHeader(final Exchange exchange) {
		ArrayList<String> ipList = (ArrayList<String>) exchange.getIn().getHeader("X-Forwarded-For");
		System.out.println(">>> SAVE HEADER >>> IPLKIST: " + ipList.toString());
		String ips = new String("");
		for(String ip : ipList){
			System.out.println(">>> SAVE HEADER>>> IP: " + ip);
			ips =  ips.concat(ip).concat(":");
		}
		System.out.println(">>> SAVE HEADER >>> IP: " + ips);
        final Message message = exchange.getIn();
        System.out.println(">>> SAVE HEADER : " + message.getHeaders());
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