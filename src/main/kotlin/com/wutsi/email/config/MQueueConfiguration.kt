package com.wutsi.email.config

import com.wutsi.newsletter.event.NewsletterEventStream
import com.wutsi.stream.EventStream
import com.wutsi.stream.EventSubscription
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
public class MQueueConfiguration(
    private val eventStream: EventStream
) {
    @Bean
    fun wutsiBlogWebSubscription() = EventSubscription("wutsi-blog-web", eventStream)

    @Bean
    fun wutsiBlogServiceSubscription() = EventSubscription("wutsi-blog-service", eventStream)

    @Bean
    fun newsletterServiceSubscription() = EventSubscription(NewsletterEventStream.NAME, eventStream)
}
