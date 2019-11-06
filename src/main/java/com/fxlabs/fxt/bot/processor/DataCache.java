package com.fxlabs.fxt.bot.processor;

import com.fxlabs.fxt.dto.project.DataRecord;
import com.fxlabs.fxt.dto.project.MarketplaceDataTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Intesar Shannan Mohammed
 */
@Component
public class DataCache {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private MarketplaceDataReader reader;

    public MarketplaceData init(String projectId, String environmentId, String importName, String policy) {
        try {
            MarketplaceDataTask marketplaceDataTask = new MarketplaceDataTask();

            marketplaceDataTask.setPolicy(policy);
            marketplaceDataTask.setCurrentPage(0);
            marketplaceDataTask.setProjectId(projectId);
            marketplaceDataTask.setEnvironmentId(environmentId);
            marketplaceDataTask.setImportName(importName);

            logger.info("Reading page [{}] module [{}] for project [{}]", marketplaceDataTask.getCurrentPage(), importName, projectId);
            marketplaceDataTask = reader.get(marketplaceDataTask);

            MarketplaceData marketplaceData = new MarketplaceData();

            marketplaceData.task.set(marketplaceDataTask);

            marketplaceData.records.set(marketplaceDataTask.getRecords());
            marketplaceData.pointer.set(new AtomicInteger(0));

            marketplaceData.inUse.set(Boolean.TRUE);

            return marketplaceData;
        } catch (Exception e) {
            logger.warn("MarketplaceData init failed - [{}]", e.getLocalizedMessage());
            return new MarketplaceData();
        }
    }

    public MarketplaceDataTask get(MarketplaceData marketplaceData) {
        if (marketplaceData == null) {
            return null;
        }
        if (isEmpty(marketplaceData)) {
            return null;
        }
        marketplaceData.task.get().setEval(marketplaceData.records.get().get(marketplaceData.pointer.get().getAndIncrement()).getRecord());

        return marketplaceData.task.get();
    }

    private boolean isEmpty(MarketplaceData marketplaceData) {
        try {
            ensureData(marketplaceData);
            if (!marketplaceData.inUse.get()) {
                return true;
            }
            logger.info("records [{}]", marketplaceData.records.get() != null ? marketplaceData.records.get().size() : 0);
            logger.info("pointer [{}]", marketplaceData.pointer.get() != null ? marketplaceData.pointer.get().get() : 0);
            return marketplaceData.records.get().size() <= marketplaceData.pointer.get().get();
        } catch (Exception e) {
            return true;
        }
    }

    private void ensureData(MarketplaceData marketplaceData) {
        try {
            // pointer >= size && currentPage * 100 < totalElements
            if (marketplaceData.inUse.get() && (marketplaceData.pointer.get().get() >= marketplaceData.records.get().size()) && (marketplaceData.task.get().getCurrentPage() + 1) * 100 < marketplaceData.task.get().getTotalElements()) {

                MarketplaceDataTask marketplaceDataTask = marketplaceData.task.get();

                marketplaceDataTask.setCurrentPage(marketplaceDataTask.getCurrentPage() + 1);

                logger.info("Reading page [{}] module [{}] for project [{}]", marketplaceDataTask.getCurrentPage(), marketplaceDataTask.getImportName(), marketplaceDataTask.getProjectId());
                marketplaceDataTask = reader.get(marketplaceData.task.get());

                marketplaceData.task.set(marketplaceDataTask);

                marketplaceData.records.set(marketplaceDataTask.getRecords());
                marketplaceData.pointer.set(new AtomicInteger(0));
            }
        } catch (Exception e) {
            logger.warn("Not able to ensure data for MarketplaceData - [{}]", e.getLocalizedMessage());
        }
    }

    public class MarketplaceData {
        public final InheritableThreadLocal<MarketplaceDataTask> task = new InheritableThreadLocal<>();
        private final InheritableThreadLocal<List<DataRecord>> records = new InheritableThreadLocal<>();
        private final InheritableThreadLocal<AtomicInteger> pointer = new InheritableThreadLocal<>();
        private final InheritableThreadLocal<Boolean> inUse = new InheritableThreadLocal<>();
    }


}
