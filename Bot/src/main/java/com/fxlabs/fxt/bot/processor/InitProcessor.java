package com.fxlabs.fxt.bot.processor;


import com.fxlabs.fxt.bot.assertions.AssertionValidator;
import com.fxlabs.fxt.bot.assertions.Context;
import com.fxlabs.fxt.dto.project.Auth;
import com.fxlabs.fxt.dto.run.BotTask;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Intesar Shannan Mohammed
 */
@Component
public class InitProcessor {

    final Logger logger = LoggerFactory.getLogger(getClass());

    private AssertionValidator assertionValidator;
    private RestTemplateUtil restTemplateUtil;
    private DataEvaluator operandEvaluator;
    private DataResolver dataResolver;
    private HeaderUtils headerUtils;

    @Autowired
    public InitProcessor(AssertionValidator assertionValidator, RestTemplateUtil restTemplateUtil,
                         DataEvaluator operandEvaluator, DataResolver dataResolver, HeaderUtils headerUtils) {
        this.assertionValidator = assertionValidator;
        this.restTemplateUtil = restTemplateUtil;
        this.operandEvaluator = operandEvaluator;
        this.dataResolver = dataResolver;
        this.headerUtils = headerUtils;
    }

    public void process(BotTask task, Context context) {

        if (task == null || task.getEndpoint() == null) {
            return;
        }
        logger.debug("Executing after task [{}]", task.getEndpoint());
        //logger.info("{} {} {} {}", task.getEndpoint(), task.getRequest(), task.getUsername(), task.getPassword());

        // Data Injection
        final String url = dataResolver.resolve(task.getEndpoint(), context, task.getSuiteName());

        boolean isMock = task.getMethod().equals(com.fxlabs.fxt.dto.project.HttpMethod.MOCK);

        HttpMethod method = HttpMethodConverter.convert(task.getMethod());

        HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.set("Content-Type", "application/json");
        httpHeaders.set("Accept", "application/json");

        headerUtils.copyHeaders(httpHeaders, task.getHeaders(), context, task.getSuiteName());
        //TODO:  As of now not supproting mutli Auth for init.  Assuming there will be only one Auth in init picking the first one.
        Auth auth = headerUtils.clone(task.getAuth().get(0));
        headerUtils.copyAuth(auth, task.getAuth().get(0), context, task.getSuiteName());

        logger.debug("Suite [{}] Total tests [{}] auth [{}]", task.getProjectDataSetId(), task.getTestCases().size(), task.getAuth());

        AtomicInteger idx = new AtomicInteger(0);
        if (CollectionUtils.isEmpty(task.getTestCases())) {
            logger.debug("Executing Suite Init for task [{}] and url [{}]", task.getSuiteName(), url);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            String url_ = url;
            if (task.getAuth() != null) {
                dataResolver.resolveHeader(httpHeaders, task.getAuth().get(0).getHeader_1());
                dataResolver.resolveHeader(httpHeaders, task.getAuth().get(0).getHeader_2());
                url_ = dataResolver.resolveQueryParam(url, task.getAuth().get(0).getHeader_3());
            }
            ResponseEntity<String> response = null;
            if (!isMock) {
                response = restTemplateUtil.execRequest(url_, method, httpHeaders, null, task.getAuth().get(0));
            }else {
                response = new ResponseEntity<>(null, HttpStatus.OK);
            }

            stopWatch.stop();
            Long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            Integer size = 0;
            if (StringUtils.isNotEmpty(response.getBody())) {
                size = response.getBody().getBytes().length;
            }

            Context initContext = new Context(context);
            initContext.withSuiteData(url_, isMock ? "MOCK" : method.name(), null, null, httpHeaders, response.getBody(), String.valueOf(response.getStatusCodeValue()), response.getHeaders(), time, size,task.getAuth().get(0).getName(), task.getPolicies().getUnmaskToken());

            assertionValidator.validate(task.getAssertions(), initContext, new StringBuilder());

            /*if (response != null && response.getStatusCodeValue() != 200) {
                context.getLogs().append(String.format("After StatusCode: [%s]", response.getStatusCode()));
            }*/

            context.withRequest(task.getSuiteName() + "_Request", null)
                    .withResponse(task.getSuiteName() + "_Response", response.getBody())
                    .withHeaders(task.getSuiteName() + "_Headers", response.getHeaders())
                    .withTask(task);

            context.withRequest(task.getSuiteName() + "_Request[0]", null)
                    .withResponse(task.getSuiteName() + "_Response[0]", response.getBody())
                    .withHeaders(task.getSuiteName() + "_Headers[0]", response.getHeaders());
        } else {
            // TODO - Support request array
            boolean isOneReq = task.getTestCases().size() == 1;
            task.getTestCases().parallelStream().forEach(testCase -> {
                // Data Injection (req)
                String req = dataResolver.resolve(testCase.getBody(), context, task.getSuiteName());
                String maskedRequest = dataResolver.resolve(StringUtils.replaceAll(testCase.getBody(), "\\{\\{@Vault(.*?)\\}\\}", "*******"), context, task.getSuiteName());
                logger.debug("Executing Suite Init for task [{}] and url [{}]", task.getSuiteName(), url);
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                String url_ = url;
                if (task.getAuth() != null) {
                    dataResolver.resolveHeader(httpHeaders, task.getAuth().get(0).getHeader_1());
                    dataResolver.resolveHeader(httpHeaders, task.getAuth().get(0).getHeader_2());
                    url_ = dataResolver.resolveQueryParam(url, task.getAuth().get(0).getHeader_3());
                }
                ResponseEntity<String> response = null;
                if (!isMock) {
                    response = restTemplateUtil.execRequest(url_, method, httpHeaders, req, task.getAuth().get(0));
                }else {
                    response = new ResponseEntity<>(req,HttpStatus.OK);
                }

                stopWatch.stop();
                Long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

                Integer size = 0;
                if (StringUtils.isNotEmpty(response.getBody())) {
                    size = response.getBody().getBytes().length;
                }

                Context initContext = new Context(context, task.getSuiteName());
                initContext.withSuiteData(url_, isMock ? "MOCK" : method.name(), req, maskedRequest, httpHeaders, response.getBody(), String.valueOf(response.getStatusCodeValue()), response.getHeaders(), time, size,task.getAuth().get(0).getName(), task.getPolicies()== null ? false : task.getPolicies().getUnmaskToken());
                assertionValidator.validate(task.getAssertions(), initContext, new StringBuilder());

                //context.getLogs().append(String.format("After StatusCode: [%s]", response.getStatusCode()));

                context.setResult(initContext.getResult());

                //if (isOneReq) {
                context.withRequest(task.getSuiteName() + "_Request", req)
                        .withResponse(task.getSuiteName() + "_Response", response.getBody())
                        .withHeaders(task.getSuiteName() + "_Headers", response.getHeaders())
                        .withTask(task);
                //}

                context.withRequest(task.getSuiteName() + "_Request[" + idx.getAndIncrement() + "]", req)
                        .withResponse(task.getSuiteName() + "_Response[" + idx.getAndIncrement() + "]", response.getBody())
                        .withHeaders(task.getSuiteName() + "_Headers[" + idx.getAndIncrement() + "]", response.getHeaders());

            });

        }
    }

}
