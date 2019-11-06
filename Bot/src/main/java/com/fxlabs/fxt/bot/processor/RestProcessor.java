package com.fxlabs.fxt.bot.processor;


import com.fxlabs.fxt.bot.amqp.Sender;
import com.fxlabs.fxt.bot.assertions.AssertionLogger;
import com.fxlabs.fxt.bot.assertions.AssertionValidator;
import com.fxlabs.fxt.bot.assertions.Context;
import com.fxlabs.fxt.dto.project.*;
import com.fxlabs.fxt.dto.run.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
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

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

/**
 * @author Intesar Shannan Mohammed
 * @author Mohammed Shoukath Ali
 */
@Component
public class RestProcessor {

    final Logger logger = LoggerFactory.getLogger(getClass());

    private Sender sender;
    //private ValidateProcessor validatorProcessor;
    private AssertionValidator assertionValidator;
    private RestTemplateUtil restTemplateUtil;
    private InitProcessor initProcessor;
    private CleanUpProcessor cleanUpProcessor;
    private DataResolver dataResolver;
    private HeaderUtils headerUtils;
    private DataCache dataCache;
    private CommandService commandService;

    @Autowired
    RestProcessor(Sender sender, AssertionValidator assertionValidator, RestTemplateUtil restTemplateUtil,
                  InitProcessor initProcessor, CleanUpProcessor cleanUpProcessor, DataResolver dataResolver,
                  HeaderUtils headerUtils, DataCache dataCache, CommandService commandService) {
        this.sender = sender;
        this.assertionValidator = assertionValidator;
        this.restTemplateUtil = restTemplateUtil;
        this.initProcessor = initProcessor;
        this.cleanUpProcessor = cleanUpProcessor;
        this.dataResolver = dataResolver;
        this.headerUtils = headerUtils;
        this.dataCache = dataCache;
        this.commandService = commandService;
    }

    public void process(BotTask task) {

        if (task.getPolicies() != null && task.getPolicies().getRepeat() != null && task.getPolicies().getRepeat() > 0) {
            for (int i = 0; i < task.getPolicies().getRepeat(); i++) {
                BotTask completeTask = run(task);
                if (completeTask == null) return;
                sender.sendTask(completeTask);
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
                BotTask completeTask = run(task);
                if (completeTask == null) return;
                if (completeTask.getTotalFailed() <= 0) {
                    sender.sendTask(completeTask);
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
            BotTask completeTask = run(task);
            if (completeTask == null) return;
            sender.sendTask(completeTask);
        }

    }

    public List<BotTask> process(LightWeightBotTask lightWeightBotTask) {

        List<BotTask> botTasks = new ArrayList<>();

        BotTask task = lightWeightBotTask.getBotTask();
        if (task.getPolicies() != null && task.getPolicies().getRepeat() != null && task.getPolicies().getRepeat() > 0) {
            for (int i = 0; i < task.getPolicies().getRepeat(); i++) {
                BotTask completeTask = runLighWeight(task);

                if (completeTask == null) break;
                if (completeTask != null) {
                    botTasks.add(completeTask);
                }
                //sender.sendTask(completeTask);
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
                BotTask completeTask = runLighWeight(task);
                if (completeTask == null) {
                    break;
                }
                if (completeTask.getTotalFailed() <= 0) {
                    botTasks.add(completeTask);
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
            BotTask completeTask = runLighWeight(task);
            if (completeTask != null) {
                botTasks.add(completeTask);
            }
            //  sender.sendTask(completeTask);
        }
        return botTasks;
    }

    private BotTask run(BotTask task) {
        //logger.debug("{}", i.incrementAndGet());
        if (task == null || task.getId() == null || task.getEndpoint() == null) {
            logger.warn("Skipping empty task");
            return null;
        }

        BotTask completeTask = new BotTask();
        final Suite suite = new Suite();
        List<TestCaseResponse> testCaseResponses = new ArrayList<>();
        boolean generateTestCases = task.isGenerateTestCaseResponse();

        AtomicLong totalFailed = new AtomicLong(0L);
        AtomicLong totalPassed = new AtomicLong(0L);
        AtomicLong totalTime = new AtomicLong(0L);
        AtomicLong totalSize = new AtomicLong(0L);

        try {
            suite.setTestSuiteId(task.getProjectDataSetId());  // testSuite Id
            suite.setRunId(task.getRunId());
            suite.setSuiteName(task.getSuiteName());
            if (task.getCategory() != null) {
                suite.setCategory(task.getCategory()); //.toUpperCase()
            } else {
                suite.setCategory(TestSuiteCategory.Bug.toString());
            }
            if (task.getSeverity() != null) {
                suite.setSeverity(TestSuiteSeverity.valueOf(task.getSeverity())); //.toUpperCase()
            } else {
                suite.setSeverity(TestSuiteSeverity.Major);
            }
            // handle GET requests
            if (CollectionUtils.isEmpty(task.getTestCases())) {
                TestCase testCase = new TestCase();
                testCase.setId(NumberUtils.INTEGER_ONE);
                task.setTestCases(Collections.singletonList(testCase));
            }

            completeTask.setId(task.getId());
            completeTask.setProjectId(task.getProjectId());
            completeTask.setProjectDataSetId(task.getProjectDataSetId());
            completeTask.setRequestStartTime(new Date());
            completeTask.setRunId(task.getRunId());
            completeTask.setSuiteName(task.getSuiteName());
            completeTask.setJobId(task.getJobId());
            completeTask.setCategory(task.getCategory());


            AssertionLogger logs = new AssertionLogger();

            String logType = null;
            if (task.getPolicies() != null) {
                logType = task.getPolicies().getLogger();
            }
            Context parentContext = new Context(task.getProjectId(), task.getEnvironmentId(), task.getSuiteName(), logs, logType);


            // execute init
            if (task.getPolicies() != null && StringUtils.equalsIgnoreCase(task.getPolicies().getInitExec(), "Suite")) {
                if (task.getInit() != null) {
                    //task.getInit().stream().forEach(t -> {
                    for (BotTask t : task.getInit()) {
                        logger.debug("Executing Init-Suite for task [{}] and init [{}]", task.getSuiteName(), t.getSuiteName());
                        initProcessor.process(t, parentContext);
                    }
                    //);
                }
            }

            //logger.debug("{} {} {} {}", task.getEndpoint(), task.getRequest(), task.getUsername(), task.getPassword());

            // execute request
            //RestTemplate restTemplate = new RestTemplate();
            //String url = task.getEndpoint();
            boolean isMock = task.getMethod().equals(com.fxlabs.fxt.dto.project.HttpMethod.MOCK);
            HttpMethod method = HttpMethodConverter.convert(task.getMethod());

            logger.debug("Suite [{}] Total tests [{}] auth [{}]", task.getProjectDataSetId(), task.getTestCases().size(), task.getAuth());

            Long count = 0L;

            if (task.getPolicies() != null && StringUtils.isNotEmpty(task.getPolicies().getRepeatModule())) {
                DataCache.MarketplaceData marketplaceData = dataCache.init(task.getProjectId(), task.getEnvironmentId(), task.getPolicies().getRepeatModule(), "repeatModule");
                parentContext.setMarketplaceData(marketplaceData);
                MarketplaceDataTask marketplaceDataTask = marketplaceData.task.get();
                if (marketplaceDataTask != null) {
                    count = marketplaceDataTask.getTotalElements();
                }
            }

            if (count > 0L) {
                if (!CollectionUtils.isEmpty(task.getAuth())) {
                    for (Auth auth_ : task.getAuth()) {
                        Auth auth = headerUtils.clone(auth_);
                        LongStream.range(0, count).forEach(lc -> {
                            HttpHeaders httpHeaders = new HttpHeaders();
                            httpHeaders.set("Content-Type", "application/json");
                            httpHeaders.set("Accept", "application/json");
                            headerUtils.copyHeaders(httpHeaders, task.getHeaders(), parentContext, task.getSuiteName());
                            headerUtils.copyAuth(auth, auth_, parentContext, task.getSuiteName());
                            task.getTestCases().parallelStream().forEach(testCase -> {
                                processTask(task, testCaseResponses, generateTestCases, totalFailed, totalPassed, totalTime, totalSize, logs, parentContext, method, isMock, httpHeaders, testCase, suite, auth_);
                            });
                            if (task.getPolicies().getRepeatDelay() != null && task.getPolicies().getRepeatDelay() > 0) {
                                try {
                                    Thread.sleep(task.getPolicies().getRepeatDelay());
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                } else {
                    LongStream.range(0, count).forEach(lc -> {
                        HttpHeaders httpHeaders = new HttpHeaders();
                        httpHeaders.set("Content-Type", "application/json");
                        httpHeaders.set("Accept", "application/json");
                        headerUtils.copyHeaders(httpHeaders, task.getHeaders(), parentContext, task.getSuiteName());
                        task.getTestCases().parallelStream().forEach(testCase -> {
                            processTask(task, testCaseResponses, generateTestCases, totalFailed, totalPassed, totalTime, totalSize, logs, parentContext, method, isMock, httpHeaders, testCase, suite, null);
                        });
                    });
                }
            } else {
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.set("Content-Type", "application/json");
                httpHeaders.set("Accept", "application/json");
                headerUtils.copyHeaders(httpHeaders, task.getHeaders(), parentContext, task.getSuiteName());
                if (!CollectionUtils.isEmpty(task.getAuth())) {
                    for (Auth auth_ : task.getAuth()) {
                        Auth auth = headerUtils.clone(auth_);
                        headerUtils.copyAuth(auth, auth_, parentContext, task.getSuiteName());
                        task.getTestCases().parallelStream().forEach(testCase -> {
                            processTask(task, testCaseResponses, generateTestCases, totalFailed, totalPassed, totalTime, totalSize, logs, parentContext, method, isMock, httpHeaders, testCase, suite, auth_);
                        });
                    }
                } else {
                    task.getTestCases().parallelStream().forEach(testCase -> {
                        processTask(task, testCaseResponses, generateTestCases, totalFailed, totalPassed, totalTime, totalSize, logs, parentContext, method, isMock, httpHeaders, testCase, suite, null);
                    });
                }
            }

            if (task.getPolicies() != null && StringUtils.equalsIgnoreCase(task.getPolicies().getCleanupExec(), "Suite")) {
                if (task.getCleanup() != null) {
                    //task.getCleanup().stream().forEach(t -> {
                    for (BotTask t : task.getCleanup()) {
                        logger.debug("Executing Cleanup-Suite for task [{}] and init [{}]", task.getSuiteName(), t.getSuiteName());
                        cleanUpProcessor.process(t, parentContext, StringUtils.EMPTY);
                    }
                    //);
                }
            }

            if (task.getPolicies() != null && StringUtils.equalsIgnoreCase(task.getPolicies().getInitExec(), "Suite")) {
                parentContext.getInitTasks().stream().forEach(initTask -> {
                    initTask.getInit().stream().forEach(t -> {
                        logger.debug("Executing Cleanup-Init-Suite for task [{}] and init [{}]", task.getSuiteName(), t.getSuiteName());
                        cleanUpProcessor.process(t, parentContext, t.getSuiteName());
                    });
                });
            }

            // send test suite complete

            completeTask.setTotalFailed(totalFailed.get());
            //completeTask.setTotalSkipped(totalSkipped.get());
            completeTask.setTotalPassed(totalPassed.get());
            completeTask.setTotalTests((long) task.getTestCases().size());

            completeTask.setLogs(JsonFormatUtil.clean(logs.getLogs()));

            completeTask.setRequestEndTime(new Date());
            completeTask.setTotalBytes(totalSize.get());
            completeTask.setRequestTime(completeTask.getRequestEndTime().getTime() - completeTask.getRequestStartTime().getTime());
            completeTask.setResult("SUITE");

        } catch (RuntimeException ex) {
            logger.warn(ex.getLocalizedMessage(), ex);
            completeTask.setRequestEndTime(new Date());
            completeTask.setLogs(completeTask.getLogs() + "\n " + ex.getLocalizedMessage());
        }

        suite.setFailed(totalFailed.get());
        suite.setTests(totalFailed.get() + totalPassed.get());
        suite.setSize(totalSize.get());
        suite.setTime(totalTime.get());
        this.sender.sendTask(suite);

        if (generateTestCases) {
            this.sender.sendTestCases(testCaseResponses);
        }

        return completeTask;
    }

    private void processTask(BotTask task, List<TestCaseResponse> testCaseResponses, boolean generateTestCases, AtomicLong totalFailed, AtomicLong totalPassed,
                             AtomicLong totalTime, AtomicLong totalSize, AssertionLogger logs, Context parentContext, HttpMethod method, boolean isMock,
                             HttpHeaders httpHeaders, TestCase testCase, Suite sutie, Auth auth) {
        //for (String req : task.getRequest()) {

        Context pContext = parentContext;
        Context context = new Context(pContext);

        boolean initFailed = false;
        logger.debug("Init {}", task.getCleanup());
        // execute init
        if (task.getPolicies() == null || StringUtils.isEmpty(task.getPolicies().getInitExec())
                || StringUtils.equalsIgnoreCase(task.getPolicies().getInitExec(), "Request")) {
            // create new context if InitExec policy is of type Request
            AssertionLogger logs_ = new AssertionLogger();

            String logType_ = null;
            if (task.getPolicies() != null) {
                logType_ = task.getPolicies().getLogger();
            }

            pContext = new Context(task.getProjectId(), task.getEnvironmentId(), task.getSuiteName(), logs, logType_);
            pContext.setMarketplaceData(parentContext.getMarketplaceData());
            context = new Context(pContext);

            if (task.getInit() != null) {
                //task.getInit().stream().forEach(t -> {
                for (BotTask t : task.getInit()) {
                    logger.debug("Executing Suite Init-Request for task [{}] and init [{}]", task.getSuiteName(), t.getSuiteName());
                    initProcessor.process(t, context);
                    if (StringUtils.equalsIgnoreCase(context.getResult(), "fail")) {
                        initFailed = true;
                    }
                }
                //);
            }
        }

        //BotTask newTask = new BotTask();
        //newTask.setId(task.getId());
        //newTask.setRequestStartTime(new Date());

        //logger.debug("Request: [{}]", req);
//        HttpEntity<String> request = new HttpEntity<>(testCase.getBody(), httpHeaders);

        //String endpoint = task.getEndpoint();
        String req = dataResolver.resolve(testCase.getBody(), pContext, task.getSuiteName());
        String maskedRequest = dataResolver.resolve(StringUtils.replaceAll(testCase.getBody(), "\\{\\{@Vault(.*?)\\}\\}", "*******"), pContext, task.getSuiteName());
        String url = dataResolver.resolve(task.getEndpoint(), pContext, task.getSuiteName());

        if (auth != null) {
            dataResolver.resolveHeader(httpHeaders, auth.getHeader_1());
            dataResolver.resolveHeader(httpHeaders, auth.getHeader_2());
            url = dataResolver.resolveQueryParam(url, auth.getHeader_3());
        }


        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        ResponseEntity<String> response = null;
        if (!isMock) {
            response = restTemplateUtil.execRequest(url, method, httpHeaders, req, auth);
        } else {
            response = new ResponseEntity<>(req, HttpStatus.OK);
        }

        stopWatch.stop();
        Long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        totalTime.getAndAdd(time);

        Integer responseSize = 0;
        if (response != null && StringUtils.isNotEmpty(response.getBody())) {
            responseSize = response.getBody().getBytes().length;
        }
        totalSize.getAndAdd(responseSize);

        //newTask.setRequestEndTime(new Date());
        //newTask.setRequestTime(newTask.getRequestEndTime().getTime() - newTask.getRequestStartTime().getTime());

        // validate assertions
        context.withSuiteData(url, isMock ? "MOCK" : method.name(), req, maskedRequest, httpHeaders, response.getBody(), String.valueOf(response.getStatusCodeValue()), response.getHeaders(), time, responseSize, auth != null ? auth.getName() : "", task.getPolicies() == null ? false : task.getPolicies().getUnmaskToken());

        StringBuilder assertionLogs = new StringBuilder();
        assertionValidator.validate(task.getAssertions(), context, assertionLogs);

        //validatorProcessor.process(task.getAssertions(), response, statusCode, logs, taskStatus);

        //newTask.setLogs(context.getLogs().toString());
        //newTask.setResult(context.getResult());

        //logger.debug("Result: [{}]", newTask.getResult());
        switch (context.getResult()) {
            case "pass":
                totalPassed.incrementAndGet();
                break;
            case "fail":
            default:
                totalFailed.incrementAndGet();
                break;
        }

        // Marking the test  suite as failed if init Fails
        if (initFailed) {
            context.setResult("fail");
        }


        logger.debug("Cleanup {}", task.getCleanup());
        // execute after
        if (task.getPolicies() == null || StringUtils.isEmpty(task.getPolicies().getCleanupExec())
                || StringUtils.equalsIgnoreCase(task.getPolicies().getCleanupExec(), "Request")) {
            if (task.getCleanup() != null) {
                for (BotTask t : task.getCleanup()) {
                    //task.getCleanup().stream().forEach(t -> {
                    logger.debug("Executing Cleanup-Request for task [{}] and init [{}]", task.getSuiteName(), t.getSuiteName());
                    cleanUpProcessor.process(t, context, StringUtils.EMPTY);
                }
                //);
            }
        }

        // clean-up init tasks
        final Context _pContext = pContext;
        if (!CollectionUtils.isEmpty(context.getInitTasks())) {
            context.getInitTasks().stream().forEach(initTask -> {
                initTask.getCleanup().stream().forEach(t -> {
                    logger.debug("Executing Cleanup-Init-Request for task [{}] and init [{}]", task.getSuiteName(), t.getSuiteName());
                    cleanUpProcessor.process(t, _pContext, initTask.getSuiteName());
                });
            });
        }

        // return processed task
        //sender.sendTask(newTask);
        String formattedRequest = null;
        try {
            formattedRequest = JsonFormatUtil.format(req);
        } catch (Exception e) {
            logger.warn(e.getLocalizedMessage());
        }
        String formattedResponse = null;
        if ((responseSize / 1048576) < 1) { //Check if response size is greater than 1 MB
            try {
                CharsetDecoder decoder =
                        StandardCharsets.UTF_8.newDecoder();
                try {
                    if (response.getBody() != null) {
                        decoder.decode(
                                ByteBuffer.wrap("".getBytes()));
                        if (StringUtils.startsWith(response.getBody(), decoder.replacement())) {
                            formattedResponse = "{\"INFO\": \"Response contains invalid non UTF-8 Char\"}";
                        } else {
                            formattedResponse = JsonFormatUtil.format(response.getBody());
                        }

                    }
                } catch (CharacterCodingException ex) {
                    formattedResponse = "{\"INFO\": \"Response contains invalid non UTF-8 Char\"}";
                }

            } catch (Exception e) {
                logger.warn(e.getLocalizedMessage());
            }
        } else {
            formattedResponse = "{\"INFO\": \"Data truncated by the bot, response too large\"}";
        }


        String formattedLogs = null;
        try {
            formattedLogs = logs.getLogs();
        } catch (Exception e) {
            logger.warn(e.getLocalizedMessage());
        }

        sutie.setStatusCode(String.valueOf(response.getStatusCodeValue()));
        // Test-Cases Responses
        if (generateTestCases) {
            TestCaseResponse tc = new TestCaseResponse();
            tc.setProject(task.getProject());
            tc.setProjectId(task.getProjectId());
            tc.setJob(task.getJob());
            tc.setJobId(task.getJobId());
            tc.setEnv(task.getEnv());
            tc.setEnvironmentId(task.getEnvironmentId());
            tc.setRegion(task.getRegion());
            tc.setSuite(task.getSuiteName());
            if (testCase.getId() != null) {
                tc.setTestCase(String.valueOf(testCase.getId()));
            }
            tc.setEndpointEval(url);
            tc.setRequestEval(formattedRequest);
            tc.setResponse(formattedResponse);
            tc.setStatusCode(String.valueOf(response.getStatusCodeValue()));
            tc.setResult(context.getLocalResult());
            tc.setTime(time);
            tc.setSize(responseSize);
            tc.setHeaders(response.getHeaders().toString());
            tc.setLogs(formattedLogs);
            tc.setEndpoint(task.getEndpoint());
            tc.setMethod(task.getMethod().name());
            tc.setRunId(task.getRunId());
            tc.setPath(task.getPath());
            tc.setCategory(task.getCategory());
            tc.setSeverity(task.getSeverity());
            tc.setAuth(auth != null ? auth.getName() : "");
            tc.setCreatedBy(task.getCreatedBy());
            // TODO - Assertions
            testCaseResponses.add(tc);

        }
    }


    private BotTask runLighWeight(BotTask task) {
        //logger.debug("{}", i.incrementAndGet());
//        if (task == null || task.getId() == null || task.getEndpoint() == null) {
//            logger.warn("Skipping empty task");
//            return null;
//        }

        BotTask completeTask = new BotTask();
        //  final Suite suite = new Suite();
        List<TestCaseResponse> testCaseResponses = new ArrayList<>();
        boolean generateTestCases = task.isGenerateTestCaseResponse();

        AtomicLong totalFailed = new AtomicLong(0L);
        AtomicLong totalPassed = new AtomicLong(0L);
        AtomicLong totalTime = new AtomicLong(0L);
        AtomicLong totalSize = new AtomicLong(0L);

        try {
            // handle GET requests
            if (CollectionUtils.isEmpty(task.getTestCases())) {
                task.setTestCases(Collections.singletonList(new TestCase()));
            }

            completeTask.setId(task.getId());
            completeTask.setProjectId(task.getProjectId());
            completeTask.setProjectDataSetId(task.getProjectDataSetId());
            completeTask.setRequestStartTime(new Date());


            AssertionLogger logs = new AssertionLogger();

            String logType = null;
            if (task.getPolicies() != null) {
                logType = task.getPolicies().getLogger();
            }
            Context parentContext = new Context(task.getProjectId(), task.getEnvironmentId(), task.getSuiteName(), logs, logType);


            // execute init
            if (task.getPolicies() != null && StringUtils.equalsIgnoreCase(task.getPolicies().getInitExec(), "Suite")) {
                if (task.getInit() != null) {
                    //task.getInit().stream().forEach(t -> {
                    for (BotTask t : task.getInit()) {
                        logger.debug("Executing Init-Suite for task [{}] and init [{}]", task.getSuiteName(), t.getSuiteName());
                        initProcessor.process(t, parentContext);
                    }
                    //);
                }
            }

            //logger.debug("{} {} {} {}", task.getEndpoint(), task.getRequest(), task.getUsername(), task.getPassword());

            // execute request
            //RestTemplate restTemplate = new RestTemplate();
            //String url = task.getEndpoint();

            boolean isMock = task.getMethod().equals(com.fxlabs.fxt.dto.project.HttpMethod.MOCK);
            HttpMethod method = HttpMethodConverter.convert(task.getMethod());


            logger.debug("Suite [{}] Total tests [{}] auth [{}]", task.getProjectDataSetId(), task.getTestCases().size(), task.getAuth());

            Long count = 0L;

            if (task.getPolicies() != null && StringUtils.isNotEmpty(task.getPolicies().getRepeatModule())) {
                DataCache.MarketplaceData marketplaceData = dataCache.init(task.getProjectId(), task.getEnvironmentId(), task.getPolicies().getRepeatModule(), "repeatModule");
                parentContext.setMarketplaceData(marketplaceData);
                count = marketplaceData.task.get().getTotalElements();
            }

            if (count > 0L) {
                if (!CollectionUtils.isEmpty(task.getAuth())) {
                    for (Auth auth_ : task.getAuth()) {
                        Auth auth = headerUtils.clone(auth_);
                        LongStream.range(0, count).forEach(lc -> {
                            HttpHeaders httpHeaders = new HttpHeaders();
                            httpHeaders.set("Content-Type", "application/json");
                            httpHeaders.set("Accept", "application/json");
                            headerUtils.copyHeaders(httpHeaders, task.getHeaders(), parentContext, task.getSuiteName());
                            headerUtils.copyAuth(auth, auth_, parentContext, task.getSuiteName());
                            task.getTestCases().parallelStream().forEach(testCase -> {
                                processTask(task, testCaseResponses, generateTestCases, totalFailed, totalPassed, totalTime, totalSize, logs, parentContext, method, isMock, httpHeaders, testCase, new Suite(), auth_);
                            });
                        });
                    }
                } else {
                    LongStream.range(0, count).forEach(lc -> {
                        HttpHeaders httpHeaders = new HttpHeaders();
                        httpHeaders.set("Content-Type", "application/json");
                        httpHeaders.set("Accept", "application/json");
                        headerUtils.copyHeaders(httpHeaders, task.getHeaders(), parentContext, task.getSuiteName());
                        task.getTestCases().parallelStream().forEach(testCase -> {
                            processTask(task, testCaseResponses, generateTestCases, totalFailed, totalPassed, totalTime, totalSize, logs, parentContext, method, isMock, httpHeaders, testCase, new Suite(), null);
                        });
                    });
                }


            } else {
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.set("Content-Type", "application/json");
                httpHeaders.set("Accept", "application/json");
                headerUtils.copyHeaders(httpHeaders, task.getHeaders(), parentContext, task.getSuiteName());
                if (!CollectionUtils.isEmpty(task.getAuth())) {
                    for (Auth auth_ : task.getAuth()) {
                        Auth auth = headerUtils.clone(auth_);
                        headerUtils.copyAuth(auth, auth_, parentContext, task.getSuiteName());
                        task.getTestCases().parallelStream().forEach(testCase -> {
                            processTask(task, testCaseResponses, generateTestCases, totalFailed, totalPassed, totalTime, totalSize, logs, parentContext, method, isMock, httpHeaders, testCase, new Suite(), auth_);
                        });
                    }
                } else {
                    task.getTestCases().parallelStream().forEach(testCase -> {
                        processTask(task, testCaseResponses, generateTestCases, totalFailed, totalPassed, totalTime, totalSize, logs, parentContext, method, isMock, httpHeaders, testCase, new Suite(), null);
                    });
                }
            }

            if (task.getPolicies() != null && StringUtils.equalsIgnoreCase(task.getPolicies().getCleanupExec(), "Suite")) {
                if (task.getCleanup() != null) {
                    //task.getCleanup().stream().forEach(t -> {
                    for (BotTask t : task.getCleanup()) {
                        logger.debug("Executing Cleanup-Suite for task [{}] and init [{}]", task.getSuiteName(), t.getSuiteName());
                        cleanUpProcessor.process(t, parentContext, StringUtils.EMPTY);
                    }
                    //);
                }
            }

            if (task.getPolicies() != null && StringUtils.equalsIgnoreCase(task.getPolicies().getInitExec(), "Suite")) {
                parentContext.getInitTasks().stream().forEach(initTask -> {
                    initTask.getInit().stream().forEach(t -> {
                        logger.debug("Executing Cleanup-Init-Suite for task [{}] and init [{}]", task.getSuiteName(), t.getSuiteName());
                        cleanUpProcessor.process(t, parentContext, t.getSuiteName());
                    });
                });
            }

            // send test suite complete

            completeTask.setTotalFailed(totalFailed.get());
            //completeTask.setTotalSkipped(totalSkipped.get());
            completeTask.setTotalPassed(totalPassed.get());
            completeTask.setTotalTests((long) task.getTestCases().size());

            completeTask.setLogs(JsonFormatUtil.clean(logs.getLogs()));

            completeTask.setRequestEndTime(new Date());
            completeTask.setTotalBytes(totalSize.get());
            completeTask.setRequestTime(completeTask.getRequestEndTime().getTime() - completeTask.getRequestStartTime().getTime());
            completeTask.setResult("SUITE");
            completeTask.setRunId(String.valueOf(testCaseResponses.get(0).getStatusCode()));

        } catch (RuntimeException ex) {
            logger.warn(ex.getLocalizedMessage(), ex);
            completeTask.setRequestEndTime(new Date());
            completeTask.setLogs(completeTask.getLogs() + "\n " + ex.getLocalizedMessage());
        }

        return completeTask;
    }


    public String process(CommandRequest request) {
        return commandService.exec(request.getCmd());
    }

}
