package com.redhat.api.policy.processor;

import com.redhat.api.policy.exception.RateLimitException;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.util.ObjectHelper;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class DebugHeaderRemainingHitsProcessor implements Processor {

    private static final Logger LOGGER = Logger.getLogger(DebugHeaderRemainingHitsProcessor.class.getName());

    private String header;
    private Integer maxPolicyHits;
    private Expression expression;

    public  DebugHeaderRemainingHitsProcessor() { }

    public DebugHeaderRemainingHitsProcessor(String header, Integer maxPolicyHits, Expression expression) {
        this.header = header;
        this.maxPolicyHits = maxPolicyHits;
        this.expression = expression;
        ObjectHelper.notNull(header, "header");
        ObjectHelper.notNull(maxPolicyHits, "maxPolicyHits");
        ObjectHelper.notNull(expression, "expression");
    }

    @Override
    public void process(Exchange exchange) throws RateLimitException {
        Integer value = expression.evaluate(exchange, Integer.class); // will throw RuntimeException if not Integer.class
        if (value != null && value > 0) {
            value = maxPolicyHits - value;
        } else {
            value = maxPolicyHits;
        }
        exchange.getOut().setHeader(getHeader(), value);
    }

    public String getHeader() {
        return header;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public Integer getMaxPolicyHits() {
        return maxPolicyHits;
    }

    public void setMaxPolicyHits(Integer maxPolicyHits) {
        this.maxPolicyHits = maxPolicyHits;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }
}
