package com.fxlabs.fxt.bot.validators;

import com.fxlabs.fxt.bot.assertions.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Intesar Shannan Mohammed
 */
public abstract class Validator {

    final Logger logger = LoggerFactory.getLogger(getClass());

    abstract boolean validate(String operand1, String operator, String operand2, Context context, String assertion, StringBuilder assertionLogs);

    protected boolean validationPass(String operand1, String operator, String operand2, Context context, String assertion, StringBuilder assertionLogs) {
        return true;
    }

    protected boolean validationFailed(String operand1, String operator, String operand2, Context context, String assertion, StringBuilder assertionLogs) {
        return false;
    }
}
