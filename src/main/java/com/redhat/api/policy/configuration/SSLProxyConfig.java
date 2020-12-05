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
    @Value("${custom.proxy.http-port}")
    private String httpPort;
    @Value("${custom.proxy.https-port}")
    private String httpsPort;
    @Value("${custom.proxy.debug-headers}")
    private String debugHeaders;

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

    public String getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(String httpPort) {
        this.httpPort = httpPort;
    }

    public String getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(String httpsPort) {
        this.httpsPort = httpsPort;
    }

    public String getProducerQuery() {
        return producerQuery;
    }

    public void setProducerQuery(String producerQuery) {
        this.producerQuery = producerQuery;
    }

    public String getDebugHeaders() {
        return debugHeaders;
    }

    public void setDebugHeaders(String debugHeaders) {
        this.debugHeaders = debugHeaders;
    }
}
