package com.redhat.api.policy.configuration;

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
public class InfinispanConfig {
    private static Logger LOGGER = LoggerFactory.getLogger(InfinispanConfig.class);

    @Value("${infinispan.client.hotrod.host}")
    private String host;

    @Value("${infinispan.client.hotrod.port}")
    private Integer port;

    @Value("${infinispan.client.hotrod.username}")
    private String username;

    @Value("${infinispan.client.hotrod.password}")
    private String password;

    @Value("${infinispan.client.hotrod.use-authn}")
    private Boolean authn;

    @Value("${infinispan.client.hotrod.cache}")
    private String cache;

    @Value("${infinispan.client.hotrod.trust-store-path}")
    private String trustStorePath;

    @Value("${infinispan.client.hotrod.socket-timeout}")
    private Integer socketTimeout;

    @Value("${infinispan.client.hotrod.connection-timeout}")
    private Integer connectionTimeout;

    @Value("${infinispan.client.hotrod.max-retries}")
    private Integer maxRetries;

    @Bean(name = "cacheContainer")
    public RemoteCacheManager remoteCacheManagerBean() {

        LOGGER.info(":: infinispan.client.hotrod.host: " + host);
        LOGGER.info(":: infinispan.client.hotrod.port: " + port);
        LOGGER.info(":: infinispan.client.hotrod.cache: " + cache);
        LOGGER.info(":: infinispan.client.hotrod.username: " + username);
        LOGGER.info(":: infinispan.client.hotrod.password: " + password);
        LOGGER.info(":: infinispan.client.hotrod.trust-store-path: " + trustStorePath);

        ConfigurationBuilder builder = new ConfigurationBuilder();

        builder.addServer().host(host).port(port);

        // configure authn (if needed)
        if (null != username && !username.equalsIgnoreCase("")) {
            if (authn) {
                builder.security()
                    .authentication().enable()
                    .username(username)
                    .password(password)
                    .serverName(host)
                    .saslMechanism("DIGEST-MD5")
                    .saslQop(SaslQop.AUTH)
                    .realm("ApplicationRealm")
                    .ssl()
                    .trustStorePath(trustStorePath);
            }
        } else {
            builder.clientIntelligence(ClientIntelligence.BASIC);
        }

        // builder.addServers("cache-service:11222");
        builder.socketTimeout(socketTimeout);
        builder.connectionTimeout(connectionTimeout);
        builder.maxRetries(maxRetries);

        return new RemoteCacheManager(builder.build());
    }

    public static Logger getLOGGER() {
        return LOGGER;
    }

    public static void setLOGGER(Logger LOGGER) {
        InfinispanConfig.LOGGER = LOGGER;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getAuthn() {
        return authn;
    }

    public void setAuthn(Boolean authn) {
        this.authn = authn;
    }

    public String getCache() {
        return cache;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    public Integer getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(Integer socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }
}

