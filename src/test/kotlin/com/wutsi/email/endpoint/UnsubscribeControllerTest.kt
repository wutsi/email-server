package com.wutsi.email.endpoint

import com.wutsi.email.dao.UnsubscribedRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.jdbc.Sql
import org.springframework.web.client.RestTemplate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(value = ["/db/clean.sql", "/db/UnsubscribeController.sql"])
internal class UnsubscribeControllerTest {
    @LocalServerPort
    private val port = 0

    private lateinit var url: String

    private val rest: RestTemplate = RestTemplate()

    @Autowired
    private lateinit var dao: UnsubscribedRepository

    @BeforeEach
    fun setUp() {
        url = "http://127.0.0.1:$port/v1/sites/{site-id}/list/members?email={email}"
    }

    @Test
    fun `unsubscribe from site`() {
        rest.delete(url, 1, "ray.sponsible@gmail.com")

        val entity = dao.findBySiteIdAndUserIdAndEmailIgnoreCase(1, null, "ray.sponsible@gmail.com")
        assertTrue(entity.isPresent)
    }

    @Test
    fun `unsubscribe from site - already unsubscibed`() {
        rest.delete(url, 100, "ray.sponsible@gmail.com")

        val entity = dao.findBySiteIdAndUserIdAndEmailIgnoreCase(100, null, "ray.sponsible@gmail.com")
        assertTrue(entity.isPresent)
    }

    @Test
    fun `unsubscribe from user`() {
        rest.delete("$url&user-id={userId}", 11, "ray.sponsible@gmail.com", 77)

        val entity = dao.findBySiteIdAndUserIdAndEmailIgnoreCase(11, 77, "ray.sponsible@gmail.com")
        assertTrue(entity.isPresent)
    }
}
