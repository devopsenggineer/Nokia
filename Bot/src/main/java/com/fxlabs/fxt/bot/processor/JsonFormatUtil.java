package com.fxlabs.fxt.bot.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class JsonFormatUtil {
    final static Logger logger = LoggerFactory.getLogger(JsonFormatUtil.class);

    public static String format(String value) {

        if (StringUtils.isEmpty(value)) {
            return value;
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            Object jsonObject = mapper.readValue(value, Object.class);
            String result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
            // result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
            return result;
        } catch (IOException e) {
            logger.warn("Unable to read JSON content from [{}] ", value, e.getLocalizedMessage());
        }
        return value;
    }

    public static String clean(String value) {
        return StringUtils.removeAll(value, "\u0000");
    }
}
