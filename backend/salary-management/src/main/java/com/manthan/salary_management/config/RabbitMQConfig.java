package com.manthan.salary_management.config;


import tools.jackson.databind.json.JsonMapper;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PAYROLL_QUEUE = "payroll.run";
    public static final String PAYROLL_EXCHANGE = "payroll.exchange";
    public static final String PAYROLL_ROUTING_KEY = "payroll.run.key";

    @Bean
    public Queue payrollQueue() {
        return new Queue(PAYROLL_QUEUE, true);
    }

    @Bean
    public DirectExchange payrollExchange() {
        return new DirectExchange(PAYROLL_EXCHANGE);
    }

    @Bean
    public Binding payrollBinding(Queue payrollQueue, DirectExchange payrollExchange) {
        return BindingBuilder.bind(payrollQueue).to(payrollExchange).with(PAYROLL_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        JsonMapper mapper = JsonMapper.builder().build();
        return new JacksonJsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
