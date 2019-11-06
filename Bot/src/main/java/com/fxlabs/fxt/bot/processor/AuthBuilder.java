package com.fxlabs.fxt.bot.processor;

import org.apache.commons.codec.binary.Base64;

import java.nio.charset.Charset;

/**
 * @author Intesar Shannan Mohammed
 */
public class AuthBuilder {

    public static String createBasicAuth(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(Charset.forName("US-ASCII")));
        String authHeader = "Basic " + new String(encodedAuth);
        return authHeader;

    }
}
