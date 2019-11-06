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
public class CleanUpProcessor {

    final Logger logger = LoggerFactory.getLogger(getClass());

    private AssertionValidator assertionValidator;
    private RestTemplateUtil restTemplateUtil;
    private DataEvaluator operandEvaluator;
    private DataResolver dataResolver;
    private HeaderUtils headerUtils;

    @Autowired
    public CleanUpProcessor(AssertionValidator assertionValidator, RestTemplateUtil restTemplateUtil,
                            DataEvaluator operandEvaluator, DataResolver dataResolver, HeaderUtils headerUtils) {
        this.assertionValidator = assertionValidator;
        this.restTemplateUtil = restTemplateUtil;
        this.operandEvaluator = operandEvaluator;
        this.dataResolver = dataResolver;
        this.headerUtils = headerUtils;
    }

    public void process(BotTask task, Context parentContext, String parentSuite) {
        if (task.getPolicies() != null && task.getPolicies().getRepeat() != null && task.getPolicies().getRepeat() > 0) {
            for (int i = 0; i < task.getPolicies().getRepeat(); i++) {
                run(task, parentContext, parentSuite);

                if (task.getPolicies().getRepeatDelay() != null && task.getPolicies().getRepeatDelay() > 0) {
                    try {
                        Thread.sleep(task.getPolicies().getRepeatDelay());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (task.getPolicies() != null && task.getPolicies().getRepeatOnFailure() != null && task.getPolicies().getRepeatOnFailure() > 0) {
            for (int i = 0; i < task.getPolicies().getRepeatOnFailure(); i++) {

                run(task, parentContext, parentSuite);

                if (StringUtils.equalsIgnoreCase(parentContext.getResult(), "pass")) {
                    break;
                }
                if (task.getPolicies().getRepeatDelay() != null && task.getPolicies().getRepeatDelay() > 0) {
                    try {
                        Thread.sleep(task.getPolicies().getRepeatDelay());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            run(task, parentContext, parentSuite);

        }
    }

    private void run(BotTask task, Context parentContext, String parentSuite) {

        if (task == null || task.getEndpoint() == null) {
            return;
        }
        logger.debug("Executing after task [{}]", task.getEndpoint());
        //logger.debug("{} {} {} {}", task.getEndpoint(), task.getRequest(), task.getUsername(), task.getPassword());

        Context context = new Context(parentContext, task.getSuiteName());
        // Data Injection
        String url = dataResolver.resolve(task.getEndpoint(), parentContext, parentSuite);

        boolean isMock = task.getMethod().equals(com.fxlabs.fxt.dto.project.HttpMethod.MOCK);

        HttpMethod method = HttpMethodConverter.convert(task.getMethod());

        HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.set("Content-Type", "application/json");
        httpHeaders.set("Accept", "application/json");

        headerUtils.copyHeaders(httpHeaders, task.getHeaders(), context, task.getSuiteName());
        //TODO:  As of now not supproting mutli Auth for cleanup.  Assuming there will be only one Auth in cleanup picking the first one.
        Auth auth = headerUtils.clone(task.getAuth().get(0));
        headerUtils.copyAuth(auth, task.getAuth().get(0), parentContext, task.getSuiteName());

        logger.debug("Suite [{}] Total tests [{}] auth [{}] url [{}]", task.getSuiteName(), task.getTestCases().size(), task.getAuth(), url);

        AtomicInteger idx = new AtomicInteger(0);
        if (CollectionUtils.isEmpty(task.getTestCases())) {
            logger.debug("Executing Suite Cleanup for task [{}] and url [{}]", task.getSuiteName(), url);
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
                response = new ResponseEntity<>("",HttpStatus.OK);
            }
            stopWatch.stop();
            Long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            Integer size = 0;
            if (StringUtils.isNotEmpty(response.getBody())) {
                size = response.getBody().getBytes().length;
            }
            logger.debug("Suite [{}] Total tests [{}] auth [{}] url [{}] status [{}]", task.getSuiteName(), task.getTestCases().size(), task.getAuth(), url_, response.getStatusCode());
            context.withSuiteDataForPostProcessor(url_, isMock ? "MOCK" : method.name(), null, httpHeaders, response.getBody(), String.valueOf(response.getStatusCodeValue()), response.getHeaders(), time, size, task.getAuth().get(0).getName());

            assertionValidator.validate(task.getAssertions(), context, new StringBuilder());

            /*if (response != null && response.getStatusCodeValue() != 200) {
                context.getLogs().append(String.format("After StatusCode: [%s]", response.getStatusCode()));
            }*/

        } else {
            // TODO - Support request array
            task.getTestCases().parallelStream().forEach(testCase -> {
                // Data Injection (req)
                String req = dataResolver.resolve(testCase.getBody(), context, parentSuite);
                logger.debug("Executing Suite Cleanup for task [{}] and url [{}]", task.getSuiteName(), url);
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
                context.withSuiteDataForPostProcessor(url_, isMock ? "MOCK" : method.name(), req, httpHeaders, response.getBody(), String.valueOf(response.getStatusCodeValue()), response.getHeaders(), time, size,task.getAuth().get(0).getName());
                assertionValidator.validate(task.getAssertions(), context, new StringBuilder());
                //context.getLogs().append(String.format("After StatusCode: [%s]", response.getStatusCode()));
            });

        }
    }

}
