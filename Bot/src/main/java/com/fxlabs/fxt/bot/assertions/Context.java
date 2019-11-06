package com.fxlabs.fxt.bot.assertions;

import com.fxlabs.fxt.bot.processor.DataCache;
import com.fxlabs.fxt.bot.processor.JsonFormatUtil;
import com.fxlabs.fxt.dto.run.BotTask;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * @author Intesar Shannan Mohammed
 */
public class Context implements Serializable {
    private static final long serialVersionUID = 1L;

    private String suitename;
    private String projectId;
    private String environmentId;
    private AssertionLogger.LogType logType = AssertionLogger.LogType.DEBUG;
    // init & cleanup
    private Map<String, String> data = new HashMap<>();
    private Map<String, HttpHeaders> headerData = new HashMap<>();

    // suite
    private String url;
    private String request;
    private String method;
    private HttpHeaders requestHeaders;
    private HttpHeaders headers;
    private String response;
    private String statusCode;
    private Long time;
    private Integer size;
    private String auth;

    private Stack<BotTask> initTasks = new Stack();

    private AssertionLogger logs;

    private DataCache.MarketplaceData marketplaceData;

    // status - pass or fail
    private String result = "";
    private ThreadLocal<String> localResult = new InheritableThreadLocal<>();

    private Context parent;

    public Context(Context parent) {
        this.parent = parent;
        this.logs = parent.logs;
        this.logType = parent.logType;
        this.marketplaceData = parent.marketplaceData;
    }

    public Context(Context parent, String suitename) {
        this.parent = parent;
        this.suitename = suitename;
        this.logs = parent.logs;
        this.marketplaceData = parent.marketplaceData;
    }

    public Context(String projectId, String environmentId, String suitename, AssertionLogger logs, String logType) {
        this.projectId = projectId;
        this.suitename = suitename;
        this.environmentId = environmentId;
        this.logs = logs;
        if (StringUtils.equalsIgnoreCase(logType, AssertionLogger.LogType.ERROR.toString())) {
            this.logType = AssertionLogger.LogType.ERROR;
        }
    }

    public Context withSuiteData(String url, String method, String request, String maskedRequest, HttpHeaders requestHeaders, String response, String statusCode, HttpHeaders headers, Long time, Integer size, String auth, Boolean unmaskToken) {

        request = JsonFormatUtil.format(request);
        response = JsonFormatUtil.format(response);

        this.url = url;
        this.method = method;

        this.request = request;
        this.requestHeaders = requestHeaders;

        this.response = response;
        this.statusCode = statusCode;
        this.headers = headers;
        this.time = time;
        this.size = size;
        this.auth = auth;

        if (parent != null) {
            this.parent.request = request;
            this.parent.method = method;
            this.requestHeaders = requestHeaders;
            this.parent.response = response;
            this.parent.statusCode = statusCode;
            this.parent.headers = headers;
            this.parent.time = time;
            this.parent.size = size;
            this.parent.auth = auth;
        }

        String suite = this.suitename;
        if (StringUtils.isEmpty(this.suitename) && this.parent != null) {
            suite = this.parent.suitename;
        }

        // log
        this.log(suite, String.format("URL [%s]", url));
        this.log(suite, String.format("Method [%s]", method));
        this.log(suite, String.format("Auth [%s]", auth));
        this.log(suite, String.format("Request [%s]", JsonFormatUtil.format(maskedRequest)));

        //Masking Authorization Header

        String header = requestHeaders.toString();
        StringBuilder mask = new StringBuilder();
        boolean authorization = header.contains("Authorization");

        if(authorization && !unmaskToken) {
            int index = header.indexOf("Authorization");
            mask.append(header, 0, index);
            mask.append("Authorization=[**********]}");
            this.log(suite, String.format("Request-Headers [%s]", mask.toString()));
        }else {
            this.log(suite, String.format("Request-Headers [%s]", requestHeaders.toString()));
        }
        this.log(suite, String.format("Response [%s]", response));
        this.log(suite, String.format("Response-Headers [%s]", headers.toString()));
        this.log(suite, String.format("StatusCode [%s]", statusCode));
        this.log(suite, String.format("Time [%s]", time));
        this.log(suite, String.format("Size [%s]", size));

        return this;
    }

    public Context withSuiteDataForPostProcessor(String url, String method, String request, HttpHeaders requestHeaders, String response, String statusCode, HttpHeaders headers, Long time, Integer size, String auth) {

        request = JsonFormatUtil.format(request);
        response = JsonFormatUtil.format(response);

        this.url = url;
        this.method = method;
        this.request = request;
        this.requestHeaders = requestHeaders;
        this.response = response;
        this.statusCode = statusCode;
        this.headers = headers;
        this.time = time;
        this.size = size;
        this.auth = auth;

        String suite = this.suitename;
        if (StringUtils.isEmpty(this.suitename) && this.parent != null) {
            suite = this.parent.suitename;
        }

        // log
        this.log(suite, String.format("URL [%s]", url));
        this.log(suite, String.format("Method [%s]", method));
        this.log(suite, String.format("Request [%s]", request));
        this.log(suite, String.format("Auth [%s]", auth));
        this.log(suite, String.format("Request-Headers [%s]", requestHeaders.toString()));
        this.log(suite, String.format("Response [%s]", response));
        this.log(suite, String.format("Response-Headers [%s]", headers.toString()));
        this.log(suite, String.format("StatusCode [%s]", statusCode));
        this.log(suite, String.format("Time [%s]", time));
        this.log(suite, String.format("Size [%s]", size));

        return this;
    }

    public Context withRequest(String name, String request) {
        request = JsonFormatUtil.format(request);
        this.data.put(name, request);
        // log


        if (parent != null) {
            parent.withRequest(name, request);
        }
        return this;
    }

    public Context withResponse(String name, String response) {
        response = JsonFormatUtil.format(response);
        this.data.put(name, response);
        // log


        if (parent != null) {
            parent.withResponse(name, response);
        }
        return this;
    }

    public Context withHeaders(String name, HttpHeaders headers) {
        this.headerData.put(name, headers);
        // log
        this.log(name, String.format("Headers [%s]", headers.toString()));

        if (parent != null) {
            parent.withHeaders(name, headers);
        }
        return this;
    }

    public Context withTask(BotTask task) {
        // add clean-up tasks to Stack
        if (task != null) {
            this.initTasks.push(task);
        }
        return this;
    }

    private void log(String suite, String msg) {
        if ((this.parent != null && this.parent.logType == AssertionLogger.LogType.DEBUG) ||
                this.logType == AssertionLogger.LogType.DEBUG) {

            if (this.parent != null) {
                this.parent.getLogs().append(this.parent.logType, suite, msg);
            } else {
                this.getLogs().append(this.logType, suite, msg);
            }
        }
    }

    public String getRequest(String suite) {
        if (StringUtils.isEmpty(suite)) {
            return request;
        } else {
            return this.data.get(suite + "_Request");
        }
    }

    public String getResponse(String suite) {
        if (StringUtils.isEmpty(suite)) {
            return response;
        } else {
            return this.data.get(suite + "_Response");
        }
    }

    public String getStatusCode() {
        return statusCode;
    }

    public HttpHeaders getHeaders(String suite) {
        if (StringUtils.isEmpty(suite)) {
            return headers;
        } else {
            return this.headerData.get(suite + "_Headers");
        }
    }

    public Long getResponseTime() {
        return this.time;
    }

    public Integer getResponseSize() {
        return this.size;
    }

    public AssertionLogger getLogs() {
        if (parent != null) {
            return parent.logs;
        }
        return logs;
    }

    public String getResult() {
        if (parent != null) {
            return this.parent.result;
        }
        return result;
    }

    public String getLocalResult() {
        return this.localResult.get();
    }

    public void setResult(String result) {
        if (parent != null) {
            if (StringUtils.equalsIgnoreCase(this.parent.result, "fail")
                    || StringUtils.equalsIgnoreCase(this.parent.result, "skip")
                    || StringUtils.isEmpty(result)) {

            } else {
                this.parent.result = result;
                // log
                //this.log(this.parent.suitename, String.format("Result [%s]", result));
            }
        } else {
            if (StringUtils.equalsIgnoreCase(this.result, "fail")
                    || StringUtils.equalsIgnoreCase(this.result, "skip")
                    || StringUtils.isEmpty(result)) {

            } else {
                this.result = result;
                // log
                //this.log(this.suitename, String.format("Result [%s]", result));
            }
        }
        if (!StringUtils.equalsIgnoreCase(this.localResult.get(), "fail")) {
            this.localResult.set(result);
        }
    }

    public String get(String key) {
        return data.get(key);
    }

    public Stack<BotTask> getInitTasks() {
        return this.initTasks;
    }

    public String getSuitename() {
        if (parent != null) {
            return this.parent.suitename;
        } else {
            return suitename;
        }
    }

    public String getProjectId() {
        if (parent != null) {
            return this.parent.projectId;
        } else {
            return projectId;
        }
    }

    public String getEnvironmentId() {
        if (parent != null) {
            return this.parent.environmentId;
        } else {
            return environmentId;
        }
    }

    public DataCache.MarketplaceData getMarketplaceData() {
        return marketplaceData;
    }

    public void setMarketplaceData(DataCache.MarketplaceData marketplaceData) {
        this.marketplaceData = marketplaceData;
    }
}
