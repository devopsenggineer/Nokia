package com.fxlabs.fxt.bot.processor;

import com.fxlabs.fxt.dto.project.MarketplaceDataTask;

/**
 * @author Intesar Shannan Mohammed
 */

public interface MarketplaceDataReader {

    public MarketplaceDataTask get(String projectId, String environmentId, String importName);

    public MarketplaceDataTask get(MarketplaceDataTask task);
}
