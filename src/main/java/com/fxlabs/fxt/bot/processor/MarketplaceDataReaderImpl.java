package com.fxlabs.fxt.bot.processor;

import com.fxlabs.fxt.bot.amqp.Sender;
import com.fxlabs.fxt.dto.project.MarketplaceDataTask;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * @author Intesar Shannan Mohammed
 */
@Component
public class MarketplaceDataReaderImpl implements MarketplaceDataReader {

    private Sender sender;
    public MarketplaceDataReaderImpl(Sender sender) {
        this.sender = sender;
    }

    @Override
    public MarketplaceDataTask get(String projectId, String environmentId, String importName) {
        MarketplaceDataTask task = new MarketplaceDataTask();
        task.setProjectId(projectId);
        task.setEnvironmentId(environmentId);
        task.setImportName(importName);
        return sender.processMarketplaceRequest(task);
    }

    @Override
    @Cacheable(value = "marketplaceDataCache", key = "#task.projectId + #task.importName + #task.currentPage", sync = true)
    public MarketplaceDataTask get(MarketplaceDataTask task) {
        return sender.processMarketplaceRequest(task);
    }
}
