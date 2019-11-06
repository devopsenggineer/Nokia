package com.fxlabs.fxt.bot.ping;

import com.fxlabs.fxt.bot.amqp.Sender;
import com.fxlabs.fxt.dto.clusters.ClusterPing;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PingSender {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Sender sender;

    @Value("${fx.default.queue}")
    String queueName;
    @Value("${maxConcurrentConsumers}")
    int maxConcurrentConsumers;

    private String botId = "FxBot-" + RandomStringUtils.randomAlphabetic(6);

    @Scheduled(initialDelay = 50000, fixedDelay = 1000 * 120)
    public void ping() {
        try {
            logger.info("Sending ping [{}]....", botId);
            ClusterPing ping = new ClusterPing();

            ping.setBotId(botId);
            ping.setKey(queueName);
            ping.setTotalVBots(maxConcurrentConsumers);

            sender.sendTask(ping);
        } catch (RuntimeException ex) {
            logger.warn(ex.getLocalizedMessage(), ex);
        }
    }
}
