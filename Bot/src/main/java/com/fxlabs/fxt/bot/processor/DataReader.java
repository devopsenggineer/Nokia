package com.fxlabs.fxt.bot.processor;

import com.fxlabs.fxt.bot.assertions.Context;
import com.fxlabs.fxt.dto.project.MarketplaceDataTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Intesar Shannan Mohammed
 */
@Component
public class DataReader {

    @Autowired
    private DataCache cache;

    @Autowired
    private MarketplaceDataReader reader;

    public MarketplaceDataTask get(String projectId, String environmentId, String importName) {
        return reader.get(projectId, environmentId, importName);
    }

    public MarketplaceDataTask get(String projectId, String environmentId, String importName, Context context) {
        MarketplaceDataTask task = cache.get(context.getMarketplaceData());

        if (task != null) {
            return task;
        }

        return reader.get(projectId, environmentId, importName);
    }
}
