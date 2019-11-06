package com.fxlabs.fxt.bot.processor;

import com.github.javafaker.Faker;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * @author Mohammed Luqman Shareef
 * @since 6/19/2018
 */
public class FakerProcessor {

    final static Logger logger = LoggerFactory.getLogger(FakerProcessor.class);

    private static Method[] methodArray = Faker.class.getDeclaredMethods();

    public static String process(String str){

        logger.debug("Processing Faker String "+str);
        if (StringUtils.isBlank(str) ){
            return null;
        }

        String[] tokens = StringUtils.split(str,".");
        if (tokens == null || tokens.length != 2 ) return null;
        String type = tokens[0];
        String property = tokens[1];

        String value = null;
        Faker faker = new Faker();

        try {
            Method typeMethod = null;
            for (Method m : methodArray) {
                if (m.getName().equalsIgnoreCase(type)) {
                    typeMethod = m;
                    break;
                }
            }

            Object typeObj = null;
            if (typeMethod != null) {
                typeObj = typeMethod.invoke(faker);
                Method[] propMethodArray = typeObj.getClass().getDeclaredMethods();
                Method propMethod = null;
                for (Method m : propMethodArray) {
                    if (m.getName().equalsIgnoreCase(property)) {
                        if ( m.getParameterCount() == 0 ) {
                            propMethod = m;
                            break;
                        }
                    }
                }
                if (propMethod != null){
                    Object valObj = propMethod.invoke(typeObj);
                    value = valObj.toString();
                }
            }
        }catch(Exception ex){
            logger.debug("Faker couldn't process '"+str+"'.  Error occurred:"+ex.getMessage());
//            ex.printStackTrace();
        }
        return value;
    }
}
