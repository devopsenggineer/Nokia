package com.fxlabs.fxt.bot.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Intesar Shannan Mohammed
 * @author Shoukath Ali
 */

@Service
public class CommandServiceImpl implements CommandService {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public String exec(String cmd) {
        return execCmd(cmd);
    }

    @Override
    @Cacheable(value = "cmdAuthTokenCache", key = "#cmd", sync = true)
    public String execAndCache(String cmd) {
        return execCmd(cmd);
    }

    private String execCmd(String cmd) {

        try {
            boolean isWindows = System.getProperty("os.name")
                    .toLowerCase().startsWith("windows");

            ProcessBuilder builder = new ProcessBuilder();
            if (isWindows) {
                builder.command("cmd.exe", "/c", cmd);
            } else {
                builder.command("sh", "-c", cmd);
            }
            builder.directory(new File(System.getProperty("user.home")));
            Process process = null;

            process = builder.start();
            StringBuilder sb = new StringBuilder();

            StreamGobbler streamGobbler =
                    new StreamGobbler(process.getInputStream(), process.getErrorStream(), (x) -> {
                        sb.append(x);
                    });

            Executors.newSingleThreadExecutor().submit(streamGobbler);

            process.waitFor(15, TimeUnit.SECONDS);

            return sb.toString();

        } catch (IOException e) {
            logger.warn(e.getLocalizedMessage(), e);
        } catch (InterruptedException e) {
            logger.warn(e.getLocalizedMessage(), e);
        }
        return "";
    }

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private InputStream errorStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, InputStream errorStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.errorStream = errorStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);

            new BufferedReader(new InputStreamReader(errorStream)).lines()
                    .forEach(consumer);
        }
    }
}
