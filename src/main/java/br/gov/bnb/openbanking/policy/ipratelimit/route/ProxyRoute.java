/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.gov.bnb.openbanking.policy.ipratelimit.route;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.springframework.stereotype.Component;

import br.gov.bnb.openbanking.policy.ipratelimit.exception.RateLimitException;
import br.gov.bnb.openbanking.policy.ipratelimit.repository.CacheRepository;




@Component("ip-rate-limit")
public class ProxyRoute extends RouteBuilder {
	private static final Logger LOGGER = Logger.getLogger(ProxyRoute.class.getName());
	
    @Override
    public void configure() throws Exception {
		
        final RouteDefinition from;
            from = from("jetty:https://0.0.0.0:8080/teste-uri?useXForwardedForHeader=true");
        from
        	.doTry()
            	.process(ProxyRoute::beforeRedirect)
            	
            	.process(ProxyRoute::afterRedirect)
			.endDoTry()
            .doCatch(RateLimitException.class)
					.process(ProxyRoute::sendRateLimitErro)
			.end()
            ;
        
    }

    

    public static void beforeRedirect(final Exchange exchange) throws RateLimitException {
    	LOGGER.info(">>>> BEFORE REDIRECT ");
    	
    	final Message message = exchange.getIn();
    	Iterator<String> iName = message.getHeaders().keySet().iterator();
    	LOGGER.info(">>> [header values]");
    	while(iName.hasNext()) {
    		String key = (String) iName.next();
    		LOGGER.info(">>> [" +key+ "] - {"+message.getHeader(key)+"}");
    	}
		LOGGER.info(">>> <<<");
		HttpServletRequest req = exchange.getIn().getBody(HttpServletRequest.class);

		LOGGER.info(">>> [request header values]");
		Enumeration<String> headerNames = req.getHeaderNames();
		while(headerNames.hasMoreElements()){
			String element = headerNames.nextElement();
    		LOGGER.info(">>> [" +element+ "] - {"+req.getHeader(element)+"}");
		}

			
		LOGGER.info(">>> REQUEST REMOTE Addr <<< >>> " + req.getRemoteAddr() +" <<<");
		LOGGER.info(">>> REQUEST REMOTE HOST <<< >>> " + req.getRemoteHost() +" <<<");
		LOGGER.info(">>> REQUEST REMOTE Request URI <<< >>> " + req.getRequestURI() +" <<<");
		LOGGER.info(">>> REQUEST REMOTE PORT <<< >>> " + req.getRemotePort() +" <<<");
		LOGGER.info(">>> REQUEST REMOTE USER <<< >>> " + req.getRemoteUser() +" <<<");
		LOGGER.info(">>> REQUEST PATH INFO <<< >>> " + req.getPathInfo() +" <<<");
		LOGGER.info(">>> REQUEST PATH Translated <<< >>> " + req.getPathTranslated() +" <<<");
		LOGGER.info(">>> REQUEST Server Name <<< >>> " + req.getServerName() +" <<<");
		LOGGER.info(">>> REQUEST Server Port <<< >>> " + req.getServerPort() +" <<<");
		
    	boolean isCanAccess = CacheRepository.isCanAccess(req.getRemoteAddr());
    	if(isCanAccess) {
        	message.setHeader(Exchange.HTTP_HOST, req.getServerName());
        	message.setHeader(Exchange.HTTP_PORT, req.getServerPort());
        	message.setHeader(Exchange.HTTP_PATH, req.getPathInfo());
        	LOGGER.info(">>>> PROXY REWRITE TO "
        	+ message.getHeader(Exchange.HTTP_HOST)
        	+":"+message.getHeader(Exchange.HTTP_PORT)
        	+ "/"+message.getHeader(Exchange.HTTP_PATH));
        	
        	final String body = message.getBody(String.class);
            message.setBody(body.toUpperCase(Locale.US));
       }else {
    	   LOGGER.info(">>>> RATE LIMIT REACHED FOR IP "+ req.getRemoteAddr());
   			throw new RateLimitException(">>>> RATE LIMIT REACHED FOR IP "+ req.getRemoteAddr() );
       }
    }
    
    public static void afterRedirect(final Exchange exchange) {
    	LOGGER.info(">>>> AFTER REDIRECT ");
    	final Message message = exchange.getIn();
        final String body = message.getBody(String.class);
        message.setBody(body.toUpperCase(Locale.US));
    }
    
    public static void sendRateLimitErro(final Exchange exchange) {
    	LOGGER.info(">>>> SEND COD ERROR 429  ");
    	final Message message = exchange.getIn();
    	message.setHeader(Exchange.HTTP_RESPONSE_CODE,429);
        final String body = message.getBody(String.class);
        message.setBody(body.toUpperCase(Locale.US));
    }
    
    

}
