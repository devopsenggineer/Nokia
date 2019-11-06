package com.fxlabs.fxt.bot.validators;

import com.fxlabs.fxt.bot.assertions.Context;
import org.springframework.stereotype.Component;

/**
 * @author Intesar Shannan Mohammed
 */
@Component("greaterValidator")
public class GreaterValidator extends Validator {

    @Override
    public boolean validate(String operand1, String operator, String operand2, Context context, String assertion, StringBuilder assertionLogs) {
        try {
            Double op1 = Double.parseDouble(operand1);
            Double op2 = Double.parseDouble(operand2);

            if (op1 > op2) {
                return validationPass(operand1, operator, operand2, context, assertion, assertionLogs);
            } else {
                return validationFailed(operand1, operator, operand2, context, assertion, assertionLogs);
            }
        } catch (RuntimeException re) {
            logger.warn(re.getLocalizedMessage());
            return validationFailed(operand1, operator, operand2, context, assertion, assertionLogs);
        }
    }
}
