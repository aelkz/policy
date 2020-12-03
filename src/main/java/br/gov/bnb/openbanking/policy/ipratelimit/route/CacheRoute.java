package br.gov.bnb.openbanking.policy.ipratelimit.route;

import java.util.logging.Logger;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.infinispan.InfinispanOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.gov.bnb.openbanking.policy.ipratelimit.processor.JeagerTagProcessor;
import br.gov.bnb.openbanking.policy.ipratelimit.processor.RateLimitProcessor;
import br.gov.bnb.openbanking.policy.ipratelimit.processor.RateLimitStorageProcessor;

/**
 * Configuração de rota responsável pelo gerenciamento de valores no Red Hat Data Grid
 * @author <a href="mailto:nramalho@redhat.com">Natanael Ramalho</a>
 */
@Component
public class CacheRoute extends RouteBuilder{
  
  private static final Logger LOGGER = Logger.getLogger(ProxyRoute.class.getName());

  public static final String HIT_COUNT = "hitCount";
  public static final String HIT_TIMESTAMP = "hitTimeStamp";
  public static final String HIT_COUNT_TOTAL = "hitCountTotal";

  @Autowired
  private RateLimitProcessor rateLimitProcessor;

  @Autowired
  private RateLimitStorageProcessor rateLimitStorageProcessor;

  @Override
  public void configure() throws Exception {

    
    
    from("direct:getHitCount")
      .routeId("get-hit-count-route")
      .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.GET))			
      .setHeader(InfinispanConstants.KEY , simple("${exchangeProperty."+ProxyRoute.CLIENT_IP+"}"))
      .to("infinispan://{{custom.rhdg.cache.name}}?cacheContainer=#cacheContainer")
      .process(rateLimitProcessor);
    
    
    from("direct:incrementHitCount")
      .routeId("increment-hit-count-route")
      .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.GET))		
      .setHeader(InfinispanConstants.KEY , simple("${exchangeProperty."+ProxyRoute.CLIENT_IP+"}-"+CacheRoute.HIT_TIMESTAMP))
      .to("infinispan:{{custom.rhdg.cache.name}}?cacheContainer=#cacheContainer")
      .process(rateLimitStorageProcessor)
      //Fazer condicional para evitar atualização sem necessidade
      .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.PUT))		
      .setHeader(InfinispanConstants.KEY , simple("${exchangeProperty."+ProxyRoute.CLIENT_IP+"}-"+CacheRoute.HIT_TIMESTAMP))
      .setHeader(InfinispanConstants.VALUE, simple("${header."+CacheRoute.HIT_TIMESTAMP+"}"))
      .to("infinispan:{{custom.rhdg.cache.name}}?cacheContainer=#cacheContainer")
      .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.PUT))			
      .setHeader(InfinispanConstants.KEY , simple("${exchangeProperty."+ProxyRoute.CLIENT_IP+"}"))
      .setHeader(InfinispanConstants.VALUE , simple("${exchangeProperty."+CacheRoute.HIT_COUNT_TOTAL+"}"))
      .to("infinispan:{{custom.rhdg.cache.name}}?cacheContainer=#cacheContainer")
      .setBody(constant(""));
  }
  
}
