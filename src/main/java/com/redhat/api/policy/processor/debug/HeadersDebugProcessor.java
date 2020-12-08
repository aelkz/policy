package com.redhat.api.policy.processor.debug;

import com.redhat.api.policy.configuration.ProxyConfig;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component
public class HeadersDebugProcessor implements Processor {

    private static final Logger LOGGER = Logger.getLogger(HeadersDebugProcessor.class.getName());

    @Autowired
    private ProxyConfig proxyConfig;

    public HeadersDebugProcessor() { }

    @Override
    public void process(Exchange exchange) {
        if (proxyConfig.debug()) {
            System.out.println("\n");
            exchange.getIn().getHeaders().forEach((k, v) -> {
                System.out.println(k + " : " + v);
            });
            System.out.println("\n");
        }

        Map<String,String> strMap = exchange.getIn().getHeaders().entrySet().stream()
            .filter(m -> m.getKey() != null && m.getValue() !=null)
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()+"")); // always add as String.class

        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
        exchange.getOut().setBody(exchange.getIn().getBody());
    }

}

