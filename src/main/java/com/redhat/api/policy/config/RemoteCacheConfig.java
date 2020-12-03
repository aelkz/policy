package com.redhat.api.policy.config;

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

    @Value("${infinispan.client.hotrod.server_list}")
    private String rhdgHosts;

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
    private Integer socketTimeout;

    @Value("${infinispan.client.hotrod.connect_timeout}")
    private Integer connecionTimeout;

    @Value("${custom.rhdg.cache.name}")
    private String cacheName;

    @Value("${infinispan.client.hotrod.max_retries}")
    private Integer maxRetries;

    private static Logger LOGGER = LoggerFactory.getLogger(RemoteCacheConfig.class);

    /*
     * RemoteCacheManager is used to access remote caches. When started, the RemoteCacheManager instantiates connections to the Hot Rod server (or multiple Hot Rod servers).
     * It then manages the persistent TCP connections while it runs. As a result, RemoteCacheManager is resource-intensive.
     * The recommended approach is to have a single RemoteCacheManager instance for each Java Virtual Machine (JVM).
     */

    @Bean(name = "cacheContainer")
    public RemoteCacheManager remoteCacheManagerExample() {

        LOGGER.info(":: infinispan.hotrod.host: " + host);
        LOGGER.info(":: infinispan.hotrod.port: " + port);
        LOGGER.info(":: custom.rhdg.cache.name: " + cacheName);
        LOGGER.info(":: infinispan.hotrod.username: " + username);
        LOGGER.info(":: infinispan.hotrod.password: " + password);
        LOGGER.info(":: infinispan.hotrod.trustStoreFileName: " + trustStore);

        ConfigurationBuilder builder = new ConfigurationBuilder();

        builder.addServer().host(host).port(port);

        if (null != username && !username.equalsIgnoreCase("")) {
            builder.security()
				// Authentication
				.authentication().enable()
				.username(username)
				.password(password)
				.serverName(host)
				.saslMechanism("DIGEST-MD5")
				.saslQop(SaslQop.AUTH)
				.realm("ApplicationRealm")
				.ssl()
				.trustStorePath(trustStore);
        } else {
            builder.clientIntelligence(ClientIntelligence.BASIC);
        }

        /*
         * List of nodes that make up the cluster to connect.
         * Lista de nodes que formam o cluster
         */
        // builder.addServers("cache-service:11222");

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
         */
        builder.maxRetries(maxRetries);
        return new RemoteCacheManager(builder.build());
    }
}

