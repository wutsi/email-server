package com.wutsi.email

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.transaction.`annotation`.EnableTransactionManagement
import kotlin.String

@SpringBootApplication
@EnableTransactionManagement
public class Application

public fun main(vararg args: String) {
    org.springframework.boot.runApplication<Application>(*args)
}
