package com.fxlabs.fxt.bot.validators;

import com.fxlabs.fxt.bot.assertions.Context;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Intesar Shannan Mohammed
 */
@Component("endsWithIgnoreCaseValidator")
public class EndsWithIgnoreCaseValidator extends Validator {

    @Override
    public boolean validate(String operand1, String operator, String operand2, Context context, String assertion, StringBuilder assertionLogs) {
        if (StringUtils.endsWithIgnoreCase(operand1, operand2)) {
            return validationPass(operand1, operator, operand2, context, assertion, assertionLogs);
        } else {
            return validationFailed(operand1, operator, operand2, context, assertion, assertionLogs);
        }
    }
}
