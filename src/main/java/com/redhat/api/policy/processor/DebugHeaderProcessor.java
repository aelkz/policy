package com.redhat.api.policy.processor;

import com.redhat.api.policy.exception.RateLimitException;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.util.ObjectHelper;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class DebugHeaderProcessor implements Processor {

    private static final Logger LOGGER = Logger.getLogger(DebugHeaderProcessor.class.getName());

    private final String header;
    private final Expression expression;

    public DebugHeaderProcessor(String header, Expression expression) {
        this.header = header;
        this.expression = expression;
        ObjectHelper.notNull(header, "header");
        ObjectHelper.notNull(expression, "expression");
    }

    @Override
    public void process(Exchange exchange) throws RateLimitException {
        String value = expression.evaluate(exchange, String.class); // will throw RuntimeException if not String.class
        exchange.getOut().setHeader(getHeader(), value);
    }

    public String getHeader() {
        return header;
    }

    public Expression getExpression() {
        return expression;
    }
}
