package com.fxlabs.fxt.bot.processor;

/**
 * @author Shoukath Ali
 */

public interface CommandService {

    public String exec(String cmd);

    public String execAndCache(String cmd);

}
