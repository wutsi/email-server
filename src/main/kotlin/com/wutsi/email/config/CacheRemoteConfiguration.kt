package com.wutsi.email.config

import net.rubyeye.xmemcached.MemcachedClient
import org.springframework.beans.factory.`annotation`.Value
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.CacheManager
import org.springframework.context.`annotation`.Bean
import org.springframework.context.`annotation`.Configuration
import kotlin.Int
import kotlin.String

@Configuration
@ConditionalOnProperty(
    value = ["memcached.enabled"],
    havingValue = "true"
)
public class CacheRemoteConfiguration(
    @Value(value = "\${memcached.username}")
    private val username: String,
    @Value(value = "\${memcached.password}")
    private val password: String,
    @Value(value = "\${memcached.servers}")
    private val servers: String,
    @Value(value = "\${memcached.ttl}")
    private val ttl: Int
) {
    @Bean
    public fun memcachedClient(): MemcachedClient =
        com.wutsi.spring.memcached.MemcachedClientBuilder()
            .withServers(servers)
            .withPassword(password)
            .withUsername(username)
            .build()

    @Bean
    public fun cacheManager(): CacheManager {
        val cacheManager = org.springframework.cache.support.SimpleCacheManager()
        cacheManager.setCaches(
            listOf(
                com.wutsi.spring.memcached.MemcachedCache("default", ttl, memcachedClient())
            )
        )
        return cacheManager
    }

    @Bean
    public fun memcachedHealthIndicator(): HealthIndicator =
        com.wutsi.spring.memcached.MemcachedHealthIndicator(memcachedClient())
}
