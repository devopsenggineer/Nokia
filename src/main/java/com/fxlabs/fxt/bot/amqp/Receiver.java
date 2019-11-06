package com.fxlabs.fxt.bot.amqp;

import com.fxlabs.fxt.bot.processor.RestProcessor;
import com.fxlabs.fxt.dto.cloud.PingTask;
import com.fxlabs.fxt.dto.run.BotTask;
import com.fxlabs.fxt.dto.run.CommandRequest;
import com.fxlabs.fxt.dto.run.LightWeightBotTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Intesar Shannan Mohammed
 */
@Component
public class Receiver {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    RestProcessor restProcessor;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${delay:0}") int delay;

    public void receiveMessage(BotTask task) {
        logger.info("Received task id [{}] name [{}]", task.getId(), task.getSuiteName());
        restProcessor.process(task);
        if (delay > 0 ){
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                logger.warn(e.getMessage());
            }
        }
    }

    public List<BotTask> receiveMessage(LightWeightBotTask task) {
        logger.info("Received light-weight task id [{}] name [{}]", task.getBotTask().getId(), task.getBotTask().getSuiteName());
        return restProcessor.process(task);
    }

    public String receiveMessage(CommandRequest task) {
        logger.info("Received command-request received [{}]", task.getId());
        return restProcessor.process(task);
    }

    public String receiveMessage(PingTask pingTask) {
        logger.info("Received ping-task...");
        BuildProperties buildProperties = applicationContext.getBean(BuildProperties.class);
        return "Build Timestamp: " + buildProperties.get("buildTimeStamp");
    }

}