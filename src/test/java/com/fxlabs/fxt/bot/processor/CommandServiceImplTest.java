package com.fxlabs.fxt.bot.processor;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

/**
 * @author Intesar Shannan Mohammed
 */

@RunWith(SpringRunner.class)
@SpringBootTest()
@SpringBootApplication(scanBasePackages = {"com.fxlabs.fxt.bot"})
public class CommandServiceImplTest {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private CommandService commandService;// = new CommandServiceImpl();

    @Test
    public void exec() {
        // curl
        String val = null;
        String cmd = null;

        cmd = "curl https://fxlabs.io";
        val = commandService.exec(cmd);
        logger.info("Cmd {} val {}", cmd, val);
        Assert.assertTrue(StringUtils.isNotEmpty(val));

        cmd = "curl https://google.com";
        val = commandService.exec(cmd);
        logger.info("Cmd {} val {}", cmd, val);
        Assert.assertTrue(StringUtils.isNotEmpty(val));


        // ping
        cmd = "ping -c 3 fxlabs.io";
        val = commandService.exec(cmd);
        logger.info("Cmd {} val {}", cmd, val);
        Assert.assertTrue(StringUtils.isNotEmpty(val));

        // not cache
        cmd = "date";
        val = commandService.exec(cmd);
        logger.info("Cmd {} val {}", cmd, val);
        Assert.assertTrue(StringUtils.isNotEmpty(val));

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        cmd = "date";
        String val2 = commandService.exec(cmd);
        logger.info("Cmd {} val {}", cmd, val2);
        Assert.assertFalse(StringUtils.equals(val, val2));
    }

    @Test
    public void execInvalid() {

        // curl
        String val = null;
        String cmd = null;


        // ping
        Date sd = new Date();
        cmd = "ping fxlabs.io";
        val = commandService.exec(cmd);
        Date ed = new Date();
        logger.info("Cmd {} val {} time {}", cmd, val, ed.getTime() - sd.getTime());
        Assert.assertTrue(StringUtils.isNotEmpty(val));
        Assert.assertTrue(ed.getTime() - sd.getTime() < 16000);

    }

    @Test
    public void execAndCache() {

        String val = null;
        String cmd = null;

        cmd = "date";
        val = commandService.execAndCache(cmd);
        logger.info("Cmd {} val {}", cmd, val);
        Assert.assertTrue(StringUtils.isNotEmpty(val));

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        cmd = "date";
        String val2 = commandService.execAndCache(cmd);
        logger.info("Cmd {} val {}", cmd, val2);
        Assert.assertTrue(StringUtils.equals(val, val2));


    }
}