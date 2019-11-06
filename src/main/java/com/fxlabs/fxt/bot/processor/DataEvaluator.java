package com.fxlabs.fxt.bot.processor;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fxlabs.fxt.bot.assertions.AssertionLogger;
import com.fxlabs.fxt.bot.assertions.Context;
import com.fxlabs.fxt.dto.project.MarketplaceDataTask;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Intesar Shannan Mohammed
 */
@Component
public class DataEvaluator {

    final Logger logger = LoggerFactory.getLogger(getClass());

    private final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~@#%*()-_=+:,.?";

    String DATE_FORMAT = "yyyy-MM-dd";
    String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    String TIME_FORMAT = "HH:mm";

    static String SPECIAL_CHARACTERS = "!@#$%^&*()_=+";

    @Autowired
    private CommandService commandService;

    @Autowired
    private DataResolverNested dataResolverNested;

    private DataReader reader;

    public DataEvaluator(DataReader reader) {
        this.reader = reader;
    }

    public String evaluate(String key, Context context, String suite) {
        String val = "";
        try {
            String[] tokens = StringUtils.split(key, ".", 2);
            String KEY = tokens[0];
            String PATH = null;//"$..*";
            String PIPE = "";

            // handle Cmd
            if (StringUtils.startsWithAny(key, "@Cmd", "@CmdCache")) {
                tokens = new String[]{};
                String[] tokens1 = StringUtils.split(key, "|", 2);
                KEY = StringUtils.trim(tokens1[0]);
                PATH = StringUtils.trim(tokens1[1]);

            }

            if (tokens.length == 2 && (StringUtils.containsIgnoreCase(KEY, "Request") || StringUtils.containsIgnoreCase(KEY, "Response"))
                    && StringUtils.startsWith(tokens[1], "$..")) {
                PATH = tokens[1];
            } else if (tokens.length == 2 && (StringUtils.containsIgnoreCase(KEY, "Request") || StringUtils.containsIgnoreCase(KEY, "Response"))) {
                PATH = "$." + tokens[1];
            } else if (tokens.length == 2) {
                PATH = tokens[1];
            } else if (tokens.length == 1 && StringUtils.contains(tokens[0], "|")) {
                String[] pipeTokens = StringUtils.split(tokens[0], "|");
                KEY = StringUtils.trim(pipeTokens[0]);
                if (pipeTokens.length == 2) {
                    PATH = StringUtils.trim(pipeTokens[1]);
                }
            }

            if (StringUtils.contains(PATH, "|") && !StringUtils.startsWithAny(key, "@Cmd", "@CmdCache")) {
                String[] pipeTokens = StringUtils.split(PATH, "|");
                PATH = StringUtils.trim(pipeTokens[0]);
                if (pipeTokens.length == 2) {
                    PIPE = StringUtils.trim(pipeTokens[1]);
                }
            }

            // Locate suite
            String suiteName = "";
            if (StringUtils.contains(KEY, "_") && !StringUtils.contains(KEY, "/")) {
                suiteName = StringUtils.substringBeforeLast(KEY, "_");
                suiteName = StringUtils.substringAfter(suiteName, "@");
                KEY = "@" + StringUtils.substringAfterLast(KEY, "_");
            }
            if (StringUtils.isEmpty(suiteName)) {
                suiteName = suite;
            }

            int count = 8;

            switch (KEY) {
                case "@NULL":
                case "@EMPTY":
                case "NULL":
                case "null":
                case "EMPTY":
                case "empty":
                    val = "";
                    break;

                case "@StatusCode":
                    val = context.getStatusCode();
                    break;

                // TODO
                case "@Suite_Headers":
                case "@ResponseHeaders":
                case "@Headers":
                    val = context.getHeaders(suiteName).get(PATH).get(0);
                    logger.info("Header [{}]", val);
                    break;

                case "@Suite_Request":
                case "@Request":
                    if (StringUtils.isEmpty(PATH)) {
                        val = context.getRequest(suiteName);
                    } else {
                        try {
                            Object objRequest = JsonPath.read(context.getRequest(suiteName), PATH);
                            val = objRequest.toString();
                        } catch (Exception e) {
                            logger.warn("Not able to evaluate key [{}] for path [{}] due to exception [{}]", KEY , PATH, e.getMessage());
                        }
                    }
                    break;

                case "@Suite_Response":
                case "@Response":
                    if (StringUtils.isEmpty(PATH)) {
                        val = context.getResponse(suiteName);
                    } else {
                        try {
                            String json = context.getResponse(suiteName);
                            Object objResponse = JsonPath.read(json, PATH);
                            val = objResponse.toString();
                            if (objResponse instanceof Map) {
                                //JsonNode node = JsonPath.parse(context.getResponse(suiteName)).read(PATH, JsonNode.class);
                                DocumentContext doc = JsonPath.parse(json);
                                try {
                                    ObjectMapper mapper = new ObjectMapper();
                                    JsonNode jsonNode = mapper.readTree(doc.jsonString());
                                    // strip $.
                                    // iterate items
                                    PATH = StringUtils.removeStart(PATH, "$.");
                                    for (String column : StringUtils.split(PATH, ".")) {
                                        jsonNode = jsonNode.get(column);
                                        val = jsonNode.toString();
                                    }

                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Not able to evaluate key [{}] for path [{}] due to exception [{}] ", KEY, PATH, e.getMessage());
                        }
                    }
                    break;

                case "@ResponseTime":
                    val = String.valueOf(context.getResponseTime());
                    break;

                case "@ResponseSize":
                    val = String.valueOf(context.getResponseSize());
                    break;

                case "@Random":
                    if (StringUtils.isNotEmpty(PATH)) {
                        count = Integer.parseInt(PATH);
                    }
                    val = RandomStringUtils.randomAlphanumeric(count);
                    break;
                case "@Password":
                    if (StringUtils.isNotEmpty(PATH)) {
                        count = Integer.parseInt(PATH);
                    }
                    val = RandomStringUtils.random(count, PASSWORD_CHARS);
                    break;
                case "@RandomAscii":
                    if (StringUtils.isNotEmpty(PATH)) {
                        count = Integer.parseInt(PATH);
                    }
                    val = RandomStringUtils.randomAscii(count);
                    break;
                case "@RandomAlphabetic":
                    if (StringUtils.isNotEmpty(PATH)) {
                        count = Integer.parseInt(PATH);
                    }
                    val = RandomStringUtils.randomAlphabetic(count);
                    break;
                case "@PassAlphaNumSpl":
                    if (StringUtils.isNotEmpty(PATH)) {
                        count = Integer.parseInt(PATH);
                    }
                    char specialCharacter = SPECIAL_CHARACTERS.charAt(getRandomNumber(SPECIAL_CHARACTERS.length()));
                    val = RandomStringUtils.randomAlphanumeric(7);
                    val = val + RandomStringUtils.randomAlphabetic(4).toUpperCase();
                    val = val + specialCharacter;
                    break;
                case "@PassAlphaNum":
                    if (StringUtils.isNotEmpty(PATH)) {
                        count = Integer.parseInt(PATH);
                    }
                    val = RandomStringUtils.randomAlphanumeric(12);
                    break;
                case "@RandomAlphanumeric":
                    if (StringUtils.isNotEmpty(PATH)) {
                        count = Integer.parseInt(PATH);
                    }
                    val = RandomStringUtils.randomAlphanumeric(count);
                    break;
                case "@RandomNumeric":
                    if (StringUtils.isNotEmpty(PATH)) {
                        count = Integer.parseInt(PATH);
                    }
                    val = RandomStringUtils.randomNumeric(count);
                    break;
                case "@RandomDouble":
                case "@RandomDecimal":
                    if (StringUtils.isNotEmpty(PATH)) {
                        count = Integer.parseInt(PATH);
                    }
                    val = RandomStringUtils.randomNumeric(count);
                    break;
                case "@RandomInteger":
                    val = String.valueOf(RandomUtils.nextInt());
                    break;
                case "@RandomLong":
                    val = String.valueOf(RandomUtils.nextLong());
                    break;
                // Supports - CurrentDate, RandomDate, RandomDOB, PastDate, StartDate, FutureDate, EndDate
                case "@Date":
                    if (StringUtils.isNotEmpty(PATH)) {
                        SimpleDateFormat df = new SimpleDateFormat(PATH);
                        val = df.format(new Date());
                    } else {
                        val = String.valueOf(new Date().getTime());
                    }
                    break;
                case "@CurrentDate":
                    if (StringUtils.isNotEmpty(PATH)) {
                        SimpleDateFormat df = new SimpleDateFormat(PATH);
                        val = df.format(new Date());
                    } else {
                        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
                        val = df.format(new Date());
                    }
                    break;
                case "@RandomDate":
                    if (StringUtils.isNotEmpty(PATH)) {
                        SimpleDateFormat df = new SimpleDateFormat(PATH);
                        val = df.format(RandomDate.createRandomDate(1950, 2024));
                    } else {
                        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
                        val = df.format(RandomDate.createRandomDate(1950, 2024));
                    }
                    break;
                case "@RandomDOB":
                    if (StringUtils.isNotEmpty(PATH)) {
                        SimpleDateFormat df = new SimpleDateFormat(PATH);
                        val = df.format(RandomDate.createRandomDate(1950, 2000));
                    } else {
                        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
                        val = df.format(RandomDate.createRandomDate(1950, 2000));
                    }
                    break;

                case "@PastDate":
                case "@StartDate":
                    if (StringUtils.isNotEmpty(PATH)) {
                        SimpleDateFormat df = new SimpleDateFormat(PATH);
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.YEAR, -1);
                        val = df.format(calendar.getTime());
                    } else {
                        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.YEAR, -1);
                        val = df.format(calendar.getTime());
                    }
                    break;
                case "@FutureDate":
                case "@EndDate":
                    if (StringUtils.isNotEmpty(PATH)) {
                        SimpleDateFormat df = new SimpleDateFormat(PATH);
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.YEAR, 1);
                        val = df.format(calendar.getTime());
                    } else {
                        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.YEAR, 1);
                        val = df.format(calendar.getTime());
                    }
                    break;

                case "@DateTime":
                    if (StringUtils.isNotEmpty(PATH)) {
                        SimpleDateFormat df = new SimpleDateFormat(PATH);
                        val = df.format(new Date());
                    } else {
                        SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT);
                        val = df.format(new Date());
                    }
                    break;
                case "@PastDateTime":
                case "@PStartDateTime":
                    if (StringUtils.isNotEmpty(PATH)) {
                        SimpleDateFormat df = new SimpleDateFormat(PATH);
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.YEAR, -1);
                        val = df.format(calendar.getTime());
                    } else {
                        SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT);
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.YEAR, -1);
                        val = df.format(calendar.getTime());
                    }
                    break;
                case "@FutureDateTime":
                case "@EndDateTime":
                    if (StringUtils.isNotEmpty(PATH)) {
                        SimpleDateFormat df = new SimpleDateFormat(PATH);
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.YEAR, 1);
                        val = df.format(calendar.getTime());
                    } else {
                        SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT);
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.YEAR, 1);
                        val = df.format(calendar.getTime());
                    }
                    break;
                case "@RandomUUID":
                    val = UUID.randomUUID().toString();
                    break;

                case "@Vault":
                    MarketplaceDataTask response_ = this.reader.get(context.getProjectId(), context.getEnvironmentId(), "{{" + KEY + "." + PATH + "}}");
                    if (StringUtils.isNotEmpty(response_.getErrors())) {
                        context.getLogs().append(AssertionLogger.LogType.ERROR, context.getSuitename(), response_.getErrors());
                    } else {
                        val = response_.getEval();
                    }
                    break;

                case "@Var":
                    MarketplaceDataTask varResponse = this.reader.get(context.getProjectId(), context.getEnvironmentId(), KEY + "." + PATH);
                    if(varResponse!=null) {
                        if (StringUtils.isNotEmpty(varResponse.getErrors())) {
                            context.getLogs().append(AssertionLogger.LogType.ERROR, context.getSuitename(), varResponse.getErrors());
                        } else {
                            val = varResponse.getEval();
                        }
                    }else{
                        logger.warn("Not able to evaluate key [{}] for path [{}] - Did not receive MarketplaceDataTask", KEY, PATH);
                    }
                    break;

                case "@Faker":
                    val = FakerProcessor.process(PATH);
                    break;
                case "@Cmd":
                    if (StringUtils.isNotEmpty(PATH)) {
                        String eval = dataResolverNested.resolve(PATH, context, suite);
                        val = commandService.exec(eval);
                    }
                    break;
                case "@CmdCache":
                    if (StringUtils.isNotEmpty(PATH)) {
                        String eval = dataResolverNested.resolve(PATH, context, suite);
                        val = commandService.execAndCache(eval);
                    }
                    break;
                default:
                    if (StringUtils.endsWithIgnoreCase(KEY, "_Request") || StringUtils.endsWithIgnoreCase(KEY, "_Response")) {
                        Object initData = JsonPath.read(context.get(KEY), PATH);
                        if (initData != null) {
                            val = initData.toString();
                        }
                    } else if (StringUtils.startsWith(KEY, "@")) {
                        // Handle Marketplace request
                        MarketplaceDataTask response = this.reader.get(context.getProjectId(), context.getEnvironmentId(), KEY, context);
                        if (response != null && StringUtils.isNotEmpty(response.getErrors())) {
                            context.getLogs().append(AssertionLogger.LogType.ERROR, context.getSuitename(), response.getErrors());
                        } else if (StringUtils.isNotEmpty(PATH)) {
                            if (isJsonObject(response.getEval())) {
                                try {
                                    val = JsonPath.read(response.getEval(), PATH);
                                } catch (Exception ex) {
                                    val = response.getEval();
                                }
                            } else {
                                val = response.getEval();
                            }
                        } else {
                            val = response.getEval();
                        }

                    } else {
                        val = key;
                    }
            }

            if (StringUtils.isNotEmpty(PIPE)) {
                val = evaluatePipe(PIPE, val);
            }
//            val = URLEncoder.encode(val, "UTF-8");
        } catch (Exception e) {
            logger.warn("Not able to evaluate key [{}] for suite [{}] due to exception [{}]", key, suite, e.getMessage());
        }

        return val;
    }

    private boolean isJsonObject(String str) {

        try {
            JsonParser parser = new ObjectMapper().getFactory().createParser(str);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /*
     * TODO - Nested Pipes
     *  http://localhost:8090/example/v1/hotels/14
     *  {{@Headers.Location | substringAfterLast /}}
     */
    private String evaluatePipe(String pipe, String text) {
        String val = text;
        if (StringUtils.isEmpty(pipe)) {
            return val;
        }

        String[] seed = StringUtils.split(pipe);

        String KEY = StringUtils.trim(seed[0]);


        switch (KEY) {

            case "trim":
                val = StringUtils.trim(text);
                break;
            case "trimToNull":
                val = StringUtils.trimToNull(text);
                break;
            case "trimToEmpty":
                val = StringUtils.trimToEmpty(text);
                break;
            case "truncate":
                val = StringUtils.truncate(text, Integer.parseInt(seed[1]));
                break;
            case "strip":
                val = StringUtils.strip(text, seed[1]);
                break;
            case "indexOf":
                val = StringUtils.indexOf(text, seed[1]) + "";
                break;
            case "indexOfIgnoreCase":
                val = StringUtils.indexOfIgnoreCase(text, seed[1]) + "";
                break;
            case "lastIndexOf":
                val = StringUtils.lastIndexOf(text, seed[1]) + "";
                break;
            case "left":
                val = StringUtils.left(text, Integer.parseInt(seed[1]));
                break;
            case "right":
                val = StringUtils.right(text, Integer.parseInt(seed[1]));
                break;
            case "substringBefore":
                val = StringUtils.substringBefore(text, seed[1]);
                break;
            case "substringAfter":
                val = StringUtils.substringAfter(text, StringUtils.trim(seed[1]));
                break;
            case "substringBeforeLast":
                val = StringUtils.substringBeforeLast(text, StringUtils.trim(seed[1]));
                break;
            case "substringAfterLast":
                val = StringUtils.substringAfterLast(text, StringUtils.trim(seed[1]));
                break;
            case "substringBetween":
                val = StringUtils.substringBetween(text, StringUtils.trim(seed[1]));
                break;
            case "removeStart":
                val = StringUtils.removeStart(text, StringUtils.trim(seed[1]));
                break;
            case "removeStartIgnoreCase":
                val = StringUtils.removeStartIgnoreCase(text, StringUtils.trim(seed[1]));
                break;
            case "removeEnd":
                val = StringUtils.removeEnd(text, StringUtils.trim(seed[1]));
                break;
            case "removeEndIgnoreCase":
                val = StringUtils.removeEndIgnoreCase(text, StringUtils.trim(seed[1]));
                break;
            case "remove":
                val = StringUtils.remove(text, StringUtils.trim(seed[1]));
                break;
            case "removeIgnoreCase":
                val = StringUtils.removeIgnoreCase(text, StringUtils.trim(seed[1]));
                break;
            case "removeAll":
                val = StringUtils.removeAll(text, StringUtils.trim(seed[1]));
                break;
            case "removeFirst":
                val = StringUtils.removeFirst(text, StringUtils.trim(seed[1]));
                break;
            case "removePattern":
                val = StringUtils.removePattern(text, StringUtils.trim(seed[1]));
                break;
            case "chomp":
                val = StringUtils.chomp(text);
                break;
            case "chop":
                val = StringUtils.chop(text);
                break;
            case "repeat":
                val = StringUtils.repeat(text, Integer.parseInt(seed[1]));
                break;
            case "rightPad":
                val = StringUtils.rightPad(text, Integer.parseInt(seed[1]));
                break;
            case "leftPad":
                val = StringUtils.leftPad(text, Integer.parseInt(seed[1]));
                break;
            case "upperCase":
                val = StringUtils.upperCase(text);
                break;
            case "lowerCase":
                val = StringUtils.lowerCase(text);
                break;
            case "capitalize":
                val = StringUtils.capitalize(text);
                break;
            case "uncapitalize":
                val = StringUtils.uncapitalize(text);
                break;
            case "reverse":
                val = StringUtils.reverse(text);
                break;

        }

        return val;
    }


    private int getRandomNumber(int index) {
        int randomInt = 0;
        Random randomGenerator = new Random();
        randomInt = randomGenerator.nextInt(index - 1);
        return randomInt;
    }
}
