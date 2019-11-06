package com.fxlabs.fxt.bot.amqp;

import org.jasypt.util.text.BasicTextEncryptor;
import org.jasypt.util.text.TextEncryptor;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Intesar Shannan Mohammed
 */
@SpringBootApplication(scanBasePackages = {"com.fxlabs.fxt.bot"})
@EnableScheduling
@EnableCaching
public class Application {

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
                                             MessageListenerAdapter listenerAdapter,
                                             @Value("${fx.default.queue}") String queueName,
                                             @Value("${concurrentConsumers}") int concurrentConsumers,
                                             @Value("${maxConcurrentConsumers}") int maxConcurrentConsumers,
                                             TextEncryptor encryptor) {


        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        String queue = encryptor.decrypt(queueName);
        container.setQueueNames(queue);
        container.setConcurrentConsumers(concurrentConsumers);
        container.setMaxConcurrentConsumers(maxConcurrentConsumers);
        container.setDefaultRequeueRejected(false);
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        //container.setMessageConverter(new Jackson2JsonMessageConverter());
        container.setMessageListener(listenerAdapter);
        return container;
    }

    /*@Bean
    public org.springframework.amqp.support.converter.MessageConverter jsonConverter() {
        return new Jackson2JsonMessageConverter();
    }*/

    @Bean
    MessageListenerAdapter listenerAdapter(Receiver receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }

    @Bean
    public TextEncryptor encrypt() {
        BasicTextEncryptor encryptor = new BasicTextEncryptor();
        encryptor.setPasswordCharArray("fx-secret".toCharArray());
        return encryptor;
    }

    @Bean
    public CachingConnectionFactory rabbitConnectionFactory(RabbitProperties config, TextEncryptor encryptor)
            throws Exception {

        RabbitConnectionFactoryBean factory = new RabbitConnectionFactoryBean();

        factory.setPort(config.determinePort());
        factory.setHost(config.determineHost());

        if (config.determineUsername() != null) {
            factory.setUsername(config.determineUsername());
        }
        if (config.determinePassword() != null) {
            String pass = encryptor.decrypt(config.determinePassword());
            factory.setPassword(pass);
        }
        if (config.determineVirtualHost() != null) {
            factory.setVirtualHost(config.determineVirtualHost());
        }
        if (config.getRequestedHeartbeat() != null) {
            //factory.setRequestedHeartbeat(config.getRequestedHeartbeat().getSeconds());
        }
        RabbitProperties.Ssl ssl = config.getSsl();
        if (ssl.isEnabled()) {
            factory.setUseSSL(true);
            if (ssl.getAlgorithm() != null) {
                factory.setSslAlgorithm(ssl.getAlgorithm());
            }
            factory.setSkipServerCertificateValidation(true);
            /*factory.setKeyStore(ssl.getKeyStore());
            factory.setKeyStorePassphrase(ssl.getKeyStorePassword());
            factory.setTrustStore(ssl.getTrustStore());
            factory.setTrustStorePassphrase(ssl.getTrustStorePassword());*/
        }
        if (config.getConnectionTimeout() != null) {
            //factory.setConnectionTimeout(config.getConnectionTimeout());
        }
        factory.afterPropertiesSet();
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
                factory.getObject());
        connectionFactory.setAddresses(config.determineAddresses());
        connectionFactory.setPublisherConfirms(config.isPublisherConfirms());
        connectionFactory.setPublisherReturns(config.isPublisherReturns());

        if (config.getCache().getChannel().getSize() != null) {
            connectionFactory
                    .setChannelCacheSize(config.getCache().getChannel().getSize());
        }
        if (config.getCache().getConnection().getMode() != null) {
            connectionFactory
                    .setCacheMode(config.getCache().getConnection().getMode());
        }
        if (config.getCache().getConnection().getSize() != null) {
            connectionFactory.setConnectionCacheSize(
                    config.getCache().getConnection().getSize());
        }
        if (config.getCache().getChannel().getCheckoutTimeout() != null) {
            //connectionFactory.setChannelCheckoutTimeout(config.getCache().getChannel().getCheckoutTimeout());
        }
        return connectionFactory;
    }


}