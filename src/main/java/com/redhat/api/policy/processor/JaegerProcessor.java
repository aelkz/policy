package com.redhat.api.policy.processor;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.logging.Logger;

@Component
public class JaegerProcessor implements Processor {

    private static final Logger LOGGER = Logger.getLogger(JaegerProcessor.class.getName());

    @Autowired
    private io.opentracing.Tracer tracer;

    private String tag;
    private String value;

    public JaegerProcessor() { }

    public JaegerProcessor withTag(String tag, String value) {
        this.tag = tag;
        this.value = value;

        return this;
    }

    @Override
    public void process(Exchange exchange) {
        String tag = getTag();
        String value = getValue();

        // will create a new tag
        Span span1 = tracer.buildSpan(tag+".1").start();
        try (Scope scope = tracer.scopeManager().activate(span1)) {
            LOGGER.info(tag+".1");
            span1.setTag(tag+".1", value);
        } catch(Exception ex) {
            Tags.ERROR.set(span1, true);
            span1.log(tag+".2");
        } finally {
            span1.finish();
        }

        // evaluating to false (tag is not inserted)
        /*
        Scope scope1 = tracer.scopeManager().active();
        if (scope1 != null) {
            Span span2 = scope1.span();
            if (span2 != null) {
                span2.log(tag+".2");
                span2.setTag(tag+".2", value);
                span2.finish();
            }else {
                LOGGER.info("FAIL span2."+tag+".2");
            }
        } else {
            LOGGER.info("FAIL span2."+tag+".2 SCOPE");
        }
        */

        //
        Span span3 = tracer.activeSpan();
        if (span3 != null) {
            span3.log(tag+".3");
            span3.setTag(tag+".3", value);
            span3.finish();
        }else {
            LOGGER.info("FAIL span3."+tag+".3");
        }

        Span span4 = tracer.buildSpan(tag+".4").start();
        if (span4 != null) {
            span4.log(tag+".4");
            span4.setTag(tag+".4", value);
            span4.finish();
        }else {
            LOGGER.info("FAIL span4."+tag+".4");
        }

        if (tracer.activeSpan() != null) {
            //Span span1 = tracer.buildSpan(SPAN_NAME).start();
            //tracer.scopeManager().activate(span1, true);
            Span span5 = tracer.activeSpan();
            span5.log(tag+".5");
            span5.setTag(tag+".5", value);
        }else {
            LOGGER.info("FAIL span5."+tag+".5");
        }

        Tracer tracer1 = GlobalTracer.get();
        if (tracer1.activeSpan() != null) {
            Span span6 = tracer1.buildSpan(tag+".6").start();
            span6.log(tag+".6");
            span6.setTag(tag+".6", value);
            // tracer1.scopeManager().activate(span6, false);
            tracer1.scopeManager().activate(span6);
        }else {
            LOGGER.info("FAIL span6."+tag+".6");
        }

        /*
        Tracer tracer2 = GlobalTracer.get();
        Scope scope2 = tracer2.scopeManager().active();
        if (scope2 != null) {
            if (scope2.span() != null) {
                scope2.span().setTag(tag+".7", value);
                scope2.span().log(tag+".7");
            } else {
                LOGGER.info("FAIL span7."+tag+".7");
            }
        } else {
            LOGGER.info("FAIL span7."+tag+".7 SCOPE");
        }
        */

        Span span8 = tracer.scopeManager().activeSpan();
        if (span8 != null) {
            span8.setTag(tag+".8", value);
            span8.log(tag+".8");
        }else {
            LOGGER.info("FAIL span8."+tag+".8");
        }

        Span span9 = tracer.buildSpan(tag+".9").start();
        if (span9 != null) {
            span9.setTag(tag+".9", value);
            span9.log(tag+".9");
            span9.finish();
        }else {
            LOGGER.info("FAIL span9."+tag+".9");
        }

        Span span10 = tracer.buildSpan(tag+".10").asChildOf(tracer.scopeManager().activeSpan()).start();
        span10.setTag(tag+".10", value);
        span10.finish();

        System.out.println("\n\n\n");
        exchange.getIn().getHeaders().forEach((k,v) -> {
            if (k.startsWith("X-")) {
                System.out.println(k + " : " + v);
            }
        });
        System.out.println("\n\n\n");

        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
        exchange.getOut().setBody(exchange.getIn().getBody());



    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
