package com.fxlabs.fxt.bot.validators;

import com.fxlabs.fxt.bot.assertions.Context;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Intesar Shannan Mohammed
 */
@Component("equalsValidator")
public class EqualsValidator extends Validator {

    @Override
    public boolean validate(String operand1, String operator, String operand2, Context context, String assertion, StringBuilder assertionLogs) {
        if (StringUtils.equalsIgnoreCase(operand2, "EMPTY") && StringUtils.isEmpty(operand1)) {
            return validationPass(operand1, operator, operand2, context, assertion, assertionLogs);
        } else if (StringUtils.equals(operand1, operand2)) {
            return validationPass(operand1, operator, operand2, context, assertion, assertionLogs);
        } else {
            return validationFailed(operand1, operator, operand2, context, assertion, assertionLogs);
        }
    }
}
