package com.fxlabs.fxt.bot.amqp;

import com.fxlabs.fxt.dto.clusters.ClusterPing;
import com.fxlabs.fxt.dto.project.MarketplaceDataTask;
import com.fxlabs.fxt.dto.run.BotTask;
import com.fxlabs.fxt.dto.run.Suite;
import com.fxlabs.fxt.dto.run.TestCaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Intesar Shannan Mohammed
 */
@Component
public class Sender {

    final Logger logger = LoggerFactory.getLogger(getClass());

    private AmqpTemplate template;
    private String exchange;
    private String routingKey;
    private String marketPlaceRoutingKey;

    ParameterizedTypeReference<MarketplaceDataTask> typeReference = new ParameterizedTypeReference<MarketplaceDataTask>() {
    };


    @Autowired
    public Sender(AmqpTemplate template,
                  @Value("${fx.exchange}") String exchange,
                  @Value("${fx.default.response.queue.routingkey}") String routingKey,
                  @Value("${fx.marketplace.routingkey}") String marketPlaceRoutingKey) {

        this.template = template;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.marketPlaceRoutingKey = marketPlaceRoutingKey;
    }


    public void sendTask(BotTask task) {
        this.template.convertAndSend(exchange, routingKey, task);
    }

    public void sendTask(Suite suite) {
        this.template.convertAndSend(exchange, routingKey, suite);
    }

    public void sendTask(ClusterPing task) {
        this.template.convertAndSend(exchange, routingKey, task);
    }

    public void sendTestCases(List<TestCaseResponse> list) {
        this.template.convertAndSend(exchange, routingKey, list);
    }

    public MarketplaceDataTask processMarketplaceRequest(MarketplaceDataTask task) {
        Object obj = this.template.convertSendAndReceive(exchange, routingKey, task);
        return (MarketplaceDataTask) obj;
    }
}
