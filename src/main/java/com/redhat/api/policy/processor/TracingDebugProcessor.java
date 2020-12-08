package com.redhat.api.policy.processor;

import com.redhat.api.policy.configuration.SSLProxyConfig;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
import io.opentracing.tag.Tags;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component
public class TracingDebugProcessor implements Processor {

    private static final Logger LOGGER = Logger.getLogger(TracingDebugProcessor.class.getName());

    @Autowired
    private SSLProxyConfig proxyConfig;

    @Autowired
    private io.opentracing.Tracer tracer;

    public TracingDebugProcessor() { }

    @Override
    public void process(Exchange exchange) {
        if (proxyConfig.debug()) {
            System.out.println("\n");
            exchange.getIn().getHeaders().forEach((k, v) -> {
                if (k.startsWith("X-")) {
                    System.out.println(k + " : " + v);
                }
            });
            System.out.println("\n");
        }

        Tracer.SpanBuilder spanBuilder;

        Map<String,String> strMap = exchange.getIn().getHeaders().entrySet().stream()
                .filter(m -> m.getKey() != null && m.getValue() !=null)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()+"")); // alwyas add as String.class

        try {
            SpanContext parentSpanCtx =
                tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapAdapter(strMap));

            if (parentSpanCtx == null) {
                spanBuilder = tracer.buildSpan("root-span");
            } else {
                spanBuilder = tracer.buildSpan("root-span").asChildOf(parentSpanCtx);
            }
        } catch (IllegalArgumentException e) {
            spanBuilder = tracer.buildSpan("root-span");
        }

        spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).start();

        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
        exchange.getOut().setBody(exchange.getIn().getBody());
    }

}

