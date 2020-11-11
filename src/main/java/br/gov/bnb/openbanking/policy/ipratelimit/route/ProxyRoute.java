package br.gov.bnb.openbanking.policy.ipratelimit.route;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.springframework.stereotype.Component;
import br.gov.bnb.openbanking.policy.ipratelimit.exception.RateLimitException;

@Component("ip-rate-limit")
public class ProxyRoute extends RouteBuilder {
	private static final Logger LOGGER = Logger.getLogger(ProxyRoute.class.getName());
	
    @Override
    public void configure() throws Exception {
		
        final RouteDefinition from;
        from = from("jetty://http://0.0.0.0:8080?useXForwardedForHeader=true&matchOnUriPrefix=true");

		from
		.doTry()
			.process((e) -> {
				System.out.println(">>> beforeRedirect method");
			})
            .process(ProxyRoute::beforeRedirect)
				.process((e) -> {
					System.out.println(">>> forwarding request to backend");
				})
			.toD("http4://"
				+ "${header." + Exchange.HTTP_HOST + "}:"
				+ "${header." + Exchange.HTTP_PORT + "}"
				+ "${header." + Exchange.HTTP_PATH + "}"
				+ "?connectionClose=false&bridgeEndpoint=true&copyHeaders=true"
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

		HttpServletRequest req = exchange.getIn().getBody(HttpServletRequest.class);

		LOGGER.info("");
		LOGGER.info("request header values:");
		Enumeration<String> headerNames = req.getHeaderNames();

		while(headerNames.hasMoreElements()){
			String element = headerNames.nextElement();
    		LOGGER.info("\t[" +element+ "] - {"+req.getHeader(element)+"}");
		}

		LOGGER.info("");
		LOGGER.info("REQUEST REMOTE Addr: " + req.getRemoteAddr());
		LOGGER.info("REQUEST REMOTE HOST: " + req.getRemoteHost());
		LOGGER.info("REQUEST REMOTE Request URI: " + req.getRequestURI());
		LOGGER.info("REQUEST REMOTE PORT: " + req.getRemotePort());
		LOGGER.info("REQUEST REMOTE USER: " + req.getRemoteUser());
		LOGGER.info("REQUEST PATH INFO: " + req.getPathInfo());
		LOGGER.info("REQUEST PATH Translated: " + req.getPathTranslated());
		LOGGER.info("REQUEST Server Name: " + req.getServerName());
		LOGGER.info("REQUEST Server Port: " + req.getServerPort());

		// DESCOMENTAR PARA HABILITAR O USO DO DATAGRID
    	// boolean isCanAccess = CacheRepository.isCanAccess(req.getRemoteAddr());
    	// if(isCanAccess) {

		if(true) {
			LOGGER.info("");
			LOGGER.info("REDIRECTING TO HTTP_HOST " + req.getServerName());
			LOGGER.info("REDIRECTING TO HTTP_PORT " + req.getServerPort());
			LOGGER.info("REDIRECTING TO HTTP_PATH " + req.getPathInfo());

        	message.setHeader(Exchange.HTTP_HOST, req.getServerName());
        	message.setHeader(Exchange.HTTP_PORT, req.getServerPort());
        	message.setHeader(Exchange.HTTP_PATH, req.getPathInfo());

			LOGGER.info("");
			LOGGER.info("PROXY FORWARDING TO "
        	+ message.getHeader(Exchange.HTTP_HOST)
        	+":"+message.getHeader(Exchange.HTTP_PORT)
        	+ message.getHeader(Exchange.HTTP_PATH));
        	
        	// final String body = message.getBody(String.class);
			// message.setBody(body.toUpperCase(Locale.US));
       }else {
    	   LOGGER.info("RATE LIMIT REACHED FOR IP "+ req.getRemoteAddr());
    	   throw new RateLimitException("RATE LIMIT REACHED FOR IP "+ req.getRemoteAddr() );
       }
    }
    
    private static void afterRedirect(final Exchange exchange) {
    	LOGGER.info("AFTER REDIRECT ");
    	// final Message message = exchange.getIn();
        // final String body = message.getBody(String.class);
        // message.setBody(body.toUpperCase(Locale.US));
    }
    
    private static void sendRateLimitErro(final Exchange exchange) {
    	LOGGER.info("SEND COD ERROR 429");
    	final Message message = exchange.getIn();
    	message.setHeader(Exchange.HTTP_RESPONSE_CODE,429);
        // final String body = message.getBody(String.class);
        // message.setBody(body.toUpperCase(Locale.US));
    }

}
