package br.gov.bnb.openbanking.policy.ipratelimit.route;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.infinispan.InfinispanOperation;
import org.springframework.stereotype.Component;

import br.gov.bnb.openbanking.policy.ipratelimit.processor.RateLimitProcessor;

@Component
public class CacheRoute extends RouteBuilder{

  @Override
  public void configure() throws Exception {
    
    from("direct:getHitCount")
      .routeId("get-hit-count-route")
      .setHeader(InfinispanConstants.OPERATION, constant(InfinispanOperation.GET))			
      .setHeader(InfinispanConstants.KEY , constant("10.6.128.23"))
    /*
     * The cache name is defined in the application.properties under the key custom.rhdg.cache.name
     * 
     * O nome do cache está definido na chave key custom.rhdg.cache.name no arquivo application.properties
     */
      .to("infinispan://{{custom.rhdg.cache.name}}?cacheContainer=#remoteCacheManagerExample")
      .process(new RateLimitProcessor());
  }
  
}
