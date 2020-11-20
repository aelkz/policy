package br.gov.bnb.openbanking.policy.config;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.SaslQop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Component
public class RemoteCacheConfig {

	/*
	 * get the member list of cluster from application.properties
	 * 
	 * Obtem a lista de membros do cluster do arquivo application.properties.
	 */
 	@Value("${infinispan.client.hotrod.server_list}")
 	private  String rhdgHosts;
 	
 	@Value("${infinispan.hotrod.host}")
 	private String host;

 	@Value("${infinispan.hotrod.port}")
 	private Integer port;
 	
 	@Value("${infinispan.hotrod.username}")
 	private String username;

 	@Value("${infinispan.hotrod.password}")
 	private String password;
 	
 	@Value("${infinispan.hotrod.trustStoreFileName}")
 	private String trustStore;
 	
 	@Value("${infinispan.client.hotrod.socket_timeout}")
 	private  Integer socketTimeout;
 	
 	@Value("${infinispan.client.hotrod.connect_timeout}")
 	private  Integer connecionTimeout;
 	
 	@Value("${custom.rhdg.cache.name}")
 	private  String cacheName;
 	
 	@Value("${infinispan.client.hotrod.max_retries}")
 	private  Integer maxRetries; 	
	
	private static Logger LOGGER = LoggerFactory.getLogger(RemoteCacheConfig.class);
 	
	 /*
	  * English version:
	  * RemoteCacheManager is used to access remote caches. When started, the RemoteCacheManager instantiates connections to the Hot Rod server (or multiple Hot Rod servers). 
	  * It then manages the persistent TCP connections while it runs. As a result, RemoteCacheManager is resource-intensive. 
      * The recommended approach is to have a single RemoteCacheManager instance for each Java Virtual Machine (JVM).
      * 
      * Portuguese version:
      * RemoteCacheManager é usado para acessar caches remotos. Quando iniciado, o RemoteCacheManager instancia as conexões com o servidor Hot Rod (ou vários servidores Hot Rod). 
      * Em seguida, ele gerencia as conexões TCP persistentes enquanto é executado. Como resultado, o RemoteCacheManager consome muitos recursos.
      * A abordagem recomendada é ter uma única instância do RemoteCacheManager para cada Java Virtual Machine (JVM).
      *
	  */
	  @Bean
      public RemoteCacheManager remoteCacheManagerExample(){

      LOGGER.info("###### host: " + host);
      LOGGER.info("###### port: " + port);
      LOGGER.info("###### Cache: " + cacheName);
      LOGGER.info("###### username: '" + username + "'");
      LOGGER.info("###### password: '" + password + "'");
  	//LOGGER.info("###### trustStore: " + trustStore);

      ConfigurationBuilder builder =  new ConfigurationBuilder();

      builder.addServer().host(host).port(port);

      if (null != username && !username.equalsIgnoreCase("")) {
          builder.security()
        // Authentication
        .authentication().enable()
        //.username(username)
        //.password(password)
        .serverName("datagrid-service");
        //.saslMechanism("DIGEST-MD5")		        
        //.saslQop(SaslQop.AUTH)			
        //.ssl()
           //.trustStorePath(trustStore);		 		    
	  } else {
		  builder.clientIntelligence(ClientIntelligence.BASIC);
	  }
	      

	/*
	 *List of nodes that make up the cluster to connect.
	 *Lista de nodes que formam o cluster            
	 * 
	 */
	//   builder.addServers("cache-service:11222");

	/*
	 * This property defines the maximum socket read timeout in milliseconds before giving up waiting for bytes from the server.
	 * Essa propriedade define em milesegundos o timeout do socket  de leitura.
	 */
	builder.socketTimeout(socketTimeout);

	/*
	 * This property defines the maximum socket connect timeout before giving up connecting to the server.
	 * Propriedade que define o tempo maximo de conexão no socket.
	 */
	builder.connectionTimeout(connecionTimeout);

	/*
	 * It sets the maximum number of retries for each request. A valid value should be greater or equals than 0 (zero). 
	 * Zero means no retry will made in case of a network failure. It defaults to 10.
	 * 
	 * Define a quantidadede retentativas para cada request feita. Os valores devem ser superior a 0, onde 0 significa sem tentativas. O valor default é 10.
		 *
	 */
	builder.maxRetries(maxRetries);	    
		return new RemoteCacheManager(builder.build());
	 }
}

