package br.gov.bnb.openbanking.policy.ipratelimit.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.Traceable;
import org.apache.camel.opentracing.ActiveSpanManager;
import org.apache.camel.spi.IdAware;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.opentracing.Span;

public class JeagerTagProcessor implements Processor, Traceable, IdAware {

    static final Logger logger = LoggerFactory.getLogger(JeagerTagProcessor.class);

    private String id;
    private final String tagName;
    private final Expression expression;

    public JeagerTagProcessor( String tagName, Expression expression) {
        this.tagName = tagName;
        this.expression = expression;
        ObjectHelper.notNull(tagName, "tagName");
        ObjectHelper.notNull(expression, "expression");
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        try {
            Span camelSpan = (Span) ActiveSpanManager.getSpan(exchange);
            if (camelSpan != null) {
                String tag = expression.evaluate(exchange, String.class);
                camelSpan.setTag(tagName, tag);
                //camelSpan.setOperationName("proxy-ip-rate-limits");
            } else {
                logger.warn("OpenTracing: could not find managed span for exchange={}", exchange);
            }    
        } catch (Exception e) {
            exchange.setException(e);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getTraceLabel() {
        return "tag[" + tagName + ", " + expression + "]";
    }

    
    
}
