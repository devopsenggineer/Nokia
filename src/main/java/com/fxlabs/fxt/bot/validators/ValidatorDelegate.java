package com.fxlabs.fxt.bot.validators;

import com.fxlabs.fxt.bot.assertions.AssertionLogger;
import com.fxlabs.fxt.bot.assertions.Context;
import com.fxlabs.fxt.bot.processor.DataEvaluator;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * @author Intesar Shannan Mohammed
 */
@Component
public class ValidatorDelegate {

    final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String OR_OPERATOR = " OR ";
    private static final String AND_OPERATOR = " AND ";

    @Autowired
    @Qualifier("equalsValidator")
    protected Validator equalsValidator;

    @Autowired
    @Qualifier("equalsIgnoreCaseValidator")
    protected Validator equalsIgnoreCaseValidator;

    @Autowired
    @Qualifier("notEqualsValidator")
    protected Validator notEqualsValidator;

    @Autowired
    @Qualifier("greaterAndEqualsValidator")
    protected Validator greaterAndEqualsValidator;

    @Autowired
    @Qualifier("greaterValidator")
    protected Validator greaterValidator;

    @Autowired
    @Qualifier("lesserAndEqualsValidator")
    protected Validator lesserAndEqualsValidator;

    @Autowired
    @Qualifier("lesserValidator")
    protected Validator lesserValidator;

    @Autowired
    @Qualifier("regexValidator")
    protected Validator regexValidator;

    @Autowired
    @Qualifier("notRegexValidator")
    protected Validator notRegexValidator;

    @Autowired
    @Qualifier("startsWithValidator")
    protected Validator startsWithValidator;

    @Autowired
    @Qualifier("startsWithIgnoreCaseValidator")
    protected Validator startsWithIgnoreCaseValidator;

    @Autowired
    @Qualifier("endsWithValidator")
    protected Validator endsWithValidator;

    @Autowired
    @Qualifier("endsWithIgnoreCaseValidator")
    protected Validator endsWithIgnoreCaseValidator;


    @Autowired
    protected DataEvaluator evaluator;

    public void validate(String assertion, Context context, StringBuilder assertionLogs) {
        //logger.info("Validating assertion [{}]", assertion);
        boolean result = false;
        try {

            if (StringUtils.contains(assertion, OR_OPERATOR)) {
                String[] assertions = assertion.split(Pattern.quote(OR_OPERATOR));
                StringJoiner orJoiner = new StringJoiner(ValidatorDelegate.OR_OPERATOR);
                for (String assertion_ : assertions) {

                    String[] tokens = StringUtils.split(assertion_, " ");
                    if (tokens == null || tokens.length != 3) {
                        skipAssertion(context, assertion_, "");
                        continue;
                    }
                    final String operand1 = evaluator.evaluate(tokens[0], context, StringUtils.EMPTY);
                    final String OPERATOR = tokens[1];
                    final String operand2 = evaluator.evaluate(tokens[2], context, StringUtils.EMPTY);

                    result = result || processAssertion(operand1, OPERATOR, operand2, assertion_, context, assertionLogs);

                    StringJoiner resolved = new StringJoiner(" ");
                    resolved.add(operand1).add(OPERATOR).add(operand2);
                    orJoiner.add(resolved.toString());

                }
                AssertionLogger.LogType logType = AssertionLogger.LogType.INFO;
                if (BooleanUtils.isFalse(result)) {
                    logType = AssertionLogger.LogType.ERROR;
                }
                String msg = String.format("Assertion [%s] resolved-to [%s] result [%s]", assertion, orJoiner.toString(), (result ? "Passed" : "Failed"));
                context.getLogs().append(logType, context.getSuitename(), msg);
                assertionLogs.append(msg);

            } else if (StringUtils.contains(assertion, AND_OPERATOR)) {
                String[] assertions = assertion.split(Pattern.quote(AND_OPERATOR));
                StringJoiner andJoiner = new StringJoiner(ValidatorDelegate.AND_OPERATOR);
                result = true; // we need this for AND operator
                for (String assertion_ : assertions) {

                    String[] tokens = StringUtils.split(assertion_, " ");
                    if (tokens == null || tokens.length != 3) {
                        skipAssertion(context, assertion_, "");
                        continue;
                    }
                    final String operand1 = evaluator.evaluate(tokens[0], context, StringUtils.EMPTY);
                    final String OPERATOR = tokens[1];
                    final String operand2 = evaluator.evaluate(tokens[2], context, StringUtils.EMPTY);

                    result = result && processAssertion(operand1, OPERATOR, operand2, assertion_, context, assertionLogs);

                    StringJoiner resolved = new StringJoiner(" ");
                    resolved.add(operand1).add(OPERATOR).add(operand2);
                    andJoiner.add(resolved.toString());

                }

                AssertionLogger.LogType logType = AssertionLogger.LogType.INFO;
                if (BooleanUtils.isFalse(result)) {
                    logType = AssertionLogger.LogType.ERROR;
                }
                String msg = String.format("Assertion [%s] resolved-to [%s] result [%s]", assertion, andJoiner.toString(), (result ? "Passed" : "Failed"));
                context.getLogs().append(logType, context.getSuitename(), msg);
                assertionLogs.append(msg);

            } else {

                String[] tokens = StringUtils.split(assertion, " ");
                if (tokens == null || tokens.length != 3) {
                    skipAssertion(context, assertion, "");
                    return;
                }
                final String operand1 = evaluator.evaluate(tokens[0], context, StringUtils.EMPTY);
                final String OPERATOR = tokens[1];
                final String operand2 = evaluator.evaluate(tokens[2], context, StringUtils.EMPTY);


                result = processAssertion(operand1, OPERATOR, operand2, assertion, context, assertionLogs);
                AssertionLogger.LogType logType = AssertionLogger.LogType.INFO;
                if (BooleanUtils.isFalse(result)) {
                    logType = AssertionLogger.LogType.ERROR;
                }

                String msg = String.format("Assertion [%s] resolved-to [%s %s %s] result [%s]", assertion, operand1, OPERATOR, operand2, (result ? "Passed" : "Failed"));
                context.getLogs().append(logType, context.getSuitename(), msg);
                assertionLogs.append(msg);
            }
        } catch (Exception e) {
            logger.warn(e.getLocalizedMessage(), e);
        }

        context.setResult(result ? "pass" : "fail");

    }

    protected Boolean processAssertion(String operand1, String OPERATOR, String operand2, String assertion, Context context, StringBuilder assertionLogs) {

        Boolean result = false;

        try {

            switch (OPERATOR) {
                case "==":
                    result = equalsValidator.validate(operand1, OPERATOR, operand2, context, assertion, assertionLogs);
                    break;
                case "==IgnoreCase":
                    result = equalsIgnoreCaseValidator.validate(operand1, OPERATOR, operand2, context, assertion, assertionLogs);
                    break;
                case "!=":
                    result = notEqualsValidator.validate(operand1, OPERATOR, operand2, context, assertion, assertionLogs);
                    break;
                case ">":
                    result = greaterValidator.validate(operand1, OPERATOR, operand2, context, assertion, assertionLogs);
                    break;
                case ">=":
                    result = greaterAndEqualsValidator.validate(operand1, OPERATOR, operand2, context, assertion, assertionLogs);
                    break;
                case "<":
                    result = lesserValidator.validate(operand1, OPERATOR, operand2, context, assertion, assertionLogs);
                    break;
                case "<=":
                    result = lesserAndEqualsValidator.validate(operand1, OPERATOR, operand2, context, assertion, assertionLogs);
                    break;
                case "=~":
                    result = regexValidator.validate(operand1, OPERATOR, operand2, context, assertion, assertionLogs);
                    break;
                case "!=~":
                    result = notRegexValidator.validate(operand1, OPERATOR, operand2, context, assertion, assertionLogs);
                    break;
                case "startsWith":
                    result = startsWithValidator.validate(operand1, OPERATOR, operand2, context, assertion, assertionLogs);
                    break;
                case "startsWithIgnoreCase":
                    result = startsWithIgnoreCaseValidator.validate(operand1, OPERATOR, operand2, context, assertion, assertionLogs);
                    break;
                case "endsWith":
                    result = endsWithValidator.validate(operand1, OPERATOR, operand2, context, assertion, assertionLogs);
                    break;
                case "endsWithIgnoreCase":
                    result = endsWithIgnoreCaseValidator.validate(operand1, OPERATOR, operand2, context, assertion, assertionLogs);
                    break;
                default:
                    result = skipAssertion(context, assertion, "");
            }

        } catch (RuntimeException re) {
            logger.warn(re.getLocalizedMessage());
            skipAssertion(context, assertion, re.getLocalizedMessage());
        }
        return result;
    }

    protected boolean skipAssertion(Context context, String assertion, String errors) {
        //context.setResult("fail");
        logger.debug("Invalid assertion [{}] errors [{}]", assertion, errors);
        AssertionLogger logger = null;
        if (context.getLogs() != null) {
            context.getLogs().append(AssertionLogger.LogType.ERROR, context.getSuitename(),
                    String.format("Invalid assertion [%s] errors [{}]. e.g. valid [<Operand1> <Operation> <Operand2>] i.e. @Request.name == @Response.name", assertion, errors));
        }
        return false;
    }
}
