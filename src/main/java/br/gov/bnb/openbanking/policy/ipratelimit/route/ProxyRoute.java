package br.gov.bnb.openbanking.policy.ipratelimit.route;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Logger;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.opentracing.ActiveSpanManager;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.camel.util.jsse.TrustManagersParameters;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.gov.bnb.openbanking.policy.ipratelimit.exception.RateLimitException;
import br.gov.bnb.openbanking.policy.ipratelimit.processor.JeagerTagProcessor;

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

		ArrayList<String> ipList = new ArrayList<String>();
	
		if(!env){
			configureHttp4();
			from("netty4-http:proxy://0.0.0.0:8443?ssl=true&keyStoreFile=keystore.jks&passphrase=changeit&trustStoreFile=keystore.jks")
				.id("from-netty-tls")
				.to("direct:internal-redirect");
		}else {
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
				.process(new JeagerTagProcessor("ip", simple("${header.X-Forwarded-For}"))).id("opentracing:before-endpoint-request")
				.toD("https4://" 
					+  "${headers." + Exchange.HTTP_HOST + "}" + ":" 
					+ "${headers." + Exchange.HTTP_PORT + "}"
					+ "?bridgeEndpoint=true&throwExceptionOnFailure=false").id("proxy:endpoint-request")
				.process(ProxyRoute::uppercase).process((e) -> {
					LOGGER.info(">>> request forwarded to backend");
				}).id("proxy:after-endpoint-request")
				.process(new JeagerTagProcessor("body", simple("${body}"))).id("opentracing:after-endpoint-request")
				
			.endDoTry()
			.doCatch(RateLimitException.class)
				.wireTap("direct:incrementHitCount")
				.process(ProxyRoute::sendRateLimitErro)
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
	private static void clientIpFilter(final Exchange exchange){
		Object xForwardedFor = exchange.getIn().getHeader("X-Forwarded-For");
		ArrayList<String> ipList = new ArrayList<String>();
		if (xForwardedFor instanceof String){
			ipList.add((String)xForwardedFor);
		}else{
			ipList = (ArrayList<String>) xForwardedFor;
		}
		
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
        Object xForwardedFor = exchange.getIn().getHeader("X-Forwarded-For");
		ArrayList<String> ipList = new ArrayList<String>();
		if (xForwardedFor instanceof String){
			ipList.add((String)xForwardedFor);
		}else{
			ipList = (ArrayList<String>) xForwardedFor;
		}

        final Message message = exchange.getIn();
		final String body = message.getBody(String.class);
		
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
		Object xForwardedFor = exchange.getIn().getHeader("X-Forwarded-For");
		ArrayList<String> ipList = new ArrayList<String>();
		if (xForwardedFor instanceof String){
			ipList.add((String)xForwardedFor);
		}else{
			ipList = (ArrayList<String>)xForwardedFor;
		}

		System.out.println(">>> SAVE HEADER >>> IPLIST: " + ipList.toString());
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


}