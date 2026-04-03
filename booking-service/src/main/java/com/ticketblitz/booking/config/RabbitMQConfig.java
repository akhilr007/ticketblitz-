package com.ticketblitz.booking.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration — Booking Service (Publisher Side)
 *
 * TOPOLOGY:
 * =========
 * Exchange: ticketblitz.exchange (topic)
 *   └─ Routing Key: booking.confirmed
 *        └─ Queue: ticketblitz.booking.confirmed.queue
 *              └─ DLX: ticketblitz.dlx.exchange
 *                   └─ DLQ: ticketblitz.booking.confirmed.dlq
 *
 * WHY TOPIC EXCHANGE:
 * ====================
 * Topic exchange allows pattern-based routing. Future events
 * (booking.cancelled, booking.refunded) can reuse the same exchange
 * without reconfiguration.
 *
 * WHY DLQ:
 * =========
 * Messages that fail processing after max retries are routed to a
 * dead-letter queue for manual inspection and replay.
 *
 * @author Akhil
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "ticketblitz.exchange";

    public static final String BOOKING_CONFIRMED_QUEUE = "ticketblitz.booking.confirmed.queue";
    public static final String BOOKING_CONFIRMED_ROUTING_KEY = "booking.confirmed";

    // retry
    public static final String RETRY_QUEUE = "ticketblitz.booking.retry.queue";
    public static final String RETRY_ROUTING_KEY = "booking.retry";

    // Dead Letter
    public static final String DLX_EXCHANGE = "ticketblitz.dlx.exchange";
    public static final String DLQ_QUEUE = "ticketblitz.booking.confirmed.dlq";
    public static final String BOOKING_DLQ_ROUTING_KEY = "booking.confirmed.dlq";


    @Bean
    public TopicExchange ticketblitzExchange() {
        return ExchangeBuilder
                .topicExchange(EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder
                .directExchange(DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue bookingConfirmedQueue() {
        return QueueBuilder
                .durable(BOOKING_CONFIRMED_QUEUE)
                .deadLetterExchange(EXCHANGE_NAME)
                .deadLetterRoutingKey(RETRY_ROUTING_KEY)
                .withArgument("x-queue-mode", "lazy")
                .build();
    }

    @Bean
    public Binding bookingConfirmedBinding() {
        return BindingBuilder
                .bind(bookingConfirmedQueue())
                .to(ticketblitzExchange())
                .with(BOOKING_CONFIRMED_ROUTING_KEY);
    }

    // retry queue (30 sec delay)
    @Bean
    public Queue retryQueue() {
        return QueueBuilder
                .durable(RETRY_QUEUE)
                .deadLetterExchange(EXCHANGE_NAME)
                .deadLetterRoutingKey(BOOKING_CONFIRMED_ROUTING_KEY)
                .ttl(30000)
                .withArgument("x-queue-mode", "lazy")
                .build();
    }

    @Bean
    public Binding retryBinding() {
        return BindingBuilder
                .bind(retryQueue())
                .to(ticketblitzExchange())
                .with(RETRY_ROUTING_KEY);
    }

    // dead letter queue
    @Bean
    public Queue dlqQueue() {
        return QueueBuilder
                .durable(DLQ_QUEUE)
                .withArgument("x-queue-mode", "lazy")
                .build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder
                .bind(dlqQueue())
                .to(dlxExchange())
                .with(BOOKING_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}