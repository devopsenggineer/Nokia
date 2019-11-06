package com.fxlabs.fxt.bot.processor;

import org.springframework.http.HttpMethod;

/**
 * @author Intesar Shannan Mohammed
 */
public class HttpMethodConverter {

    public static HttpMethod convert(com.fxlabs.fxt.dto.project.HttpMethod httpMethod) {

        if (httpMethod == null)
            throw new RuntimeException(String.format("Invalid Http method: ''"));

        switch (httpMethod) {
            case GET:
                return HttpMethod.GET;
            case POST:
                return HttpMethod.POST;
            case PUT:
                return HttpMethod.PUT;
            case DELETE:
                return HttpMethod.DELETE;
            case HEAD:
                return HttpMethod.HEAD;
            case TRACE:
                return HttpMethod.TRACE;
            case PATCH:
                return HttpMethod.PATCH;
            case OPTIONS:
                return HttpMethod.OPTIONS;
            case MOCK:
                return null;
            default:
                throw new RuntimeException(String.format("Invalid Http method: %s", httpMethod.toString()));
        }
    }
}