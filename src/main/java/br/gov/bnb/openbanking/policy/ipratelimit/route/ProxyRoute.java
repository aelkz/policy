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

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Logger;

import org.apache.camel.CamelContext;
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
            from = from("netty-http:proxy://0.0.0.0:8080");
        from
        	.doTry()
            	.process(ProxyRoute::beforeRedirect)
            	.toD("netty-http:http://localhost:9081/actuator/health}"
            			+ "${headers." + Exchange.HTTP_SCHEME + "}://"
            			+ "${headers." + Exchange.HTTP_HOST + "}:"
            			+ "${headers." + Exchange.HTTP_PORT + "}"
            			+ "${headers." + Exchange.HTTP_PATH + "}"
            			)
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
    	InetSocketAddress remoteAddress = (InetSocketAddress)message.getHeader("CamelNettyRemoteAddress");
    	LOGGER.info(">>> CLIENT OPETION IP  0 " + remoteAddress.toString());
    	LOGGER.info(">>> CLIENT OPETION IP  1 " + remoteAddress.getAddress().getCanonicalHostName());
    	LOGGER.info(">>> CLIENT OPETION IP  2 " + remoteAddress.getAddress().getHostAddress() );
    	LOGGER.info(">>> CLIENT OPETION IP  3 " + remoteAddress.getAddress().getHostName() );
    	LOGGER.info(">>> CLIENT OPETION IP  4 " + new String(remoteAddress.getAddress().getAddress())) ;
    	//LOGGER.info(">>> CLIENT OPETION IP  3 " + remoteAddress.getAddress().getLocalHost().  );
    	
    	
    	LOGGER.info(">>> CLIENT ADDRESS IP " + remoteAddress.getHostName());
    	boolean isCanAccess = CacheRepository.isCanAccess(remoteAddress.getHostName());
    	if(isCanAccess) {
    		String host = (String)message.getHeader("Host");
        	String uri = (String)message.getHeader("CamelHttpUri");
        	Integer port =  (Integer)message.getHeader("CamelHttpPort");
        	message.setHeader(Exchange.HTTP_HOST, host);
        	message.setHeader(Exchange.HTTP_PORT, port);
        	message.setHeader(Exchange.HTTP_PATH, uri);
        	LOGGER.info(">>>> PROXY REWRITE TO "
        	+ message.getHeader(Exchange.HTTP_HOST)
        	+":"+message.getHeader(Exchange.HTTP_PORT)
        	+ "/"+message.getHeader(Exchange.HTTP_PATH));
        	
        	final String body = message.getBody(String.class);
            message.setBody(body.toUpperCase(Locale.US));
       }else {
    	   LOGGER.info(">>>> RATE LIMIT REACHED FOR IP "+ remoteAddress.getHostName());
   			throw new RateLimitException(">>>> RATE LIMIT REACHED FOR IP "+ remoteAddress.getHostName() );
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
