package com.redhat.api.policy.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SSLProxyConfig {

    @Value("${custom.proxy.keystore.dest}")
    private String keystoreDest;
    @Value("${custom.proxy.keystore.pass}")
    private String keystorePass;
    @Value("${custom.proxy.consumer}")
    private String consumer;
    @Value("${custom.proxy.producer}")
    private String producer;
    @Value("${custom.proxy.producer-query}")
    private String producerQuery;
    @Value("${custom.proxy.port}")
    private String port;
    @Value("${custom.proxy.debug-headers}")
    private Boolean debugHeaders;

    public Boolean isSecured() {
        return (getKeystoreDest() != null && getKeystorePass() != null) && (!"".equals(getKeystoreDest()) && !"".equals(getKeystorePass()));
    }

    public String getKeystoreDest() {
        return keystoreDest;
    }

    public void setKeystoreDest(String keystoreDest) {
        this.keystoreDest = keystoreDest;
    }

    public String getKeystorePass() {
        return keystorePass;
    }

    public void setKeystorePass(String keystorePass) {
        this.keystorePass = keystorePass;
    }

    public String getConsumer() {
        return consumer;
    }

    public void setConsumer(String consumer) {
        this.consumer = consumer;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getProducerQuery() {
        return producerQuery;
    }
    public Boolean getDebugHeaders() {
        return debugHeaders;
    }
    public void setDebugHeaders(Boolean debugHeaders) {
        this.debugHeaders = debugHeaders;
    }

    public void setProducerQuery(String producerQuery) {
        this.producerQuery = producerQuery;
    }

}
