package com.fxlabs.fxt.bot.validators;

import com.fxlabs.fxt.bot.assertions.Context;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Intesar Shannan Mohammed
 */
@Component("notEqualsValidator")
public class NotEqualsValidator extends Validator {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public boolean validate(String operand1, String operator, String operand2, Context context, String assertion, StringBuilder assertionLogs) {
        if (StringUtils.equalsIgnoreCase(operand2, "EMPTY") && StringUtils.isNotEmpty(operand1)) {
            return validationPass(operand1, operator, operand2, context, assertion, assertionLogs);
        } else if (!StringUtils.equals(operand1, operand2)) {
            return validationPass(operand1, operator, operand2, context, assertion, assertionLogs);
        } else {
            return validationFailed(operand1, operator, operand2, context, assertion, assertionLogs);
        }
    }

    /*protected boolean validationFailed(String operand1, String operator, String operand2, Context context, String assertion, StringBuilder assertionLogs) {
        //context.setResult("fail");
        String msg = String.format("Assertion [%s] failed, not expecting [%s] but found [%s]", assertion, operand2, operand1);
        //logger.debug(msg);
        context.getLogs().append(AssertionLogger.LogType.INFO, context.getSuitename(), msg);
        assertionLogs.append(msg);
        return false;
    }

    protected boolean validationPass(String operand1, String operator, String operand2, Context context, String assertion, StringBuilder assertionLogs) {
        //context.setResult("pass");
        String msg = String.format("Assertion [%s] passed, not expecting [%s] and found [%s]", assertion, operand2, operand1);
        //logger.debug(msg);
        context.getLogs().append(AssertionLogger.LogType.INFO, context.getSuitename(), msg);
        assertionLogs.append(msg);
        return true;
    }*/
}
