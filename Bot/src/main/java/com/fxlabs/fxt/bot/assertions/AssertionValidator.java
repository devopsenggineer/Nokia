package com.fxlabs.fxt.bot.assertions;

import com.fxlabs.fxt.bot.validators.ValidatorDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author Intesar Shannan Mohammed
 */
@Component
public class AssertionValidator {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected ValidatorDelegate delegate;

    public void validate(List<String> assertions, Context context, StringBuilder assertionLogs) {
        if (CollectionUtils.isEmpty(assertions)) {
            return;
        }
        assertions.parallelStream().forEach(assertion -> {
            delegate.validate(assertion, context, assertionLogs);
        });
    }
}
