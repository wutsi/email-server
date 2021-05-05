package com.wutsi.email

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.`annotation`.EnableCaching
import org.springframework.scheduling.`annotation`.EnableAsync
import org.springframework.scheduling.`annotation`.EnableScheduling
import org.springframework.security.config.`annotation`.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.`annotation`.web.configuration.EnableWebSecurity
import org.springframework.transaction.`annotation`.EnableTransactionManagement
import kotlin.String

@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
@EnableCaching
@EnableScheduling
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class Application

public fun main(vararg args: String) {
    org.springframework.boot.runApplication<Application>(*args)
}
