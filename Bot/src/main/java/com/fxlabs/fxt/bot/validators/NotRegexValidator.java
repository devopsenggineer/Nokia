package com.fxlabs.fxt.bot.validators;

import com.fxlabs.fxt.bot.assertions.Context;
import org.springframework.stereotype.Component;

/**
 * @author Intesar Shannan Mohammed
 */
@Component("notRegexValidator")
public class NotRegexValidator extends Validator {

    @Override
    public boolean validate(String operand1, String operator, String operand2, Context context, String assertion, StringBuilder assertionLogs) {
        if (!operand1.matches(operand2)) {
            return validationPass(operand1, operator, operand2, context, assertion, assertionLogs);
        } else {
            return validationFailed(operand1, operator, operand2, context, assertion, assertionLogs);
        }
    }

    /*protected boolean validationFailed(String operand1, String operand2, Context context, String assertion, StringBuilder assertionLogs) {
        //context.setResult("pass");
        String msg = String.format("Assertion [%s] failed, not expecting [%s] but found [%s]", assertion, operand2, operand1);
        //logger.debug(msg);
        context.getLogs().append(AssertionLogger.LogType.INFO, context.getSuitename(), msg);
        assertionLogs.append(msg);
        return false;
    }

    protected boolean validationPass(String operand1, String operand2, Context context, String assertion, StringBuilder assertionLogs) {
        //context.setResult("pass");
        String msg = String.format("Assertion [%s] passed, not expecting [%s] and found [%s]", assertion, operand2, operand1);
        //logger.debug(msg);
        context.getLogs().append(AssertionLogger.LogType.INFO, context.getSuitename(), msg);
        assertionLogs.append(msg);
        return true;
    }*/
}
