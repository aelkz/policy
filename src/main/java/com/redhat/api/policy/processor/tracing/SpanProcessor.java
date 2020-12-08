package com.redhat.api.policy.processor.tracing;

import com.redhat.api.policy.configuration.ProxyConfig;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component
public class SpanProcessor implements Processor {

    private static final Logger LOGGER = Logger.getLogger(SpanProcessor.class.getName());

    @Autowired
    private ProxyConfig proxyConfig;

    @Autowired
    private io.opentracing.Tracer tracer;

    private String tag;
    private String value;
    private String operation; // TODO - change String.class to Expression.class

    public SpanProcessor() { }

    public SpanProcessor withTag(String tag, String value) {
        this.tag = tag;
        this.value = value;
        this.operation = tag;

        return this;
    }

    public SpanProcessor withTag(String tag, String value, String operation) {
        this.tag = tag;
        this.value = value;
        this.operation = operation;

        return this;
    }

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
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()+"")); // alwyas add as String.class

        SpanContext parentSpan =
            tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapAdapter(strMap));

        Span childSpan = tracer.buildSpan(getTag()).asChildOf(parentSpan).start();
        // valid: x.forwarded.for
        // invalid: X-Forwarded-For
        childSpan.setTag(getTag(), getValue());
        childSpan.setOperationName(getOperation());
        childSpan.finish();

        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
        exchange.getOut().setBody(exchange.getIn().getBody());
    }

    private String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    private String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    private String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}
