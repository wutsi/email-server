package com.wutsi.email.endpoint

import com.wutsi.email.dao.UnsubscribedRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.jdbc.Sql

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(value = ["/db/clean.sql", "/db/UnsubscribeController.sql"])
internal class UnsubscribeControllerTest : ControllerTestBase() {
    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var dao: UnsubscribedRepository

    @Test
    fun `unsubscribe from site`() {
        login("email")

        val url = "http://127.0.0.1:$port/v1/sites/1/list/members?email=ray.sponsible@gmail.com"
        delete(url)

        val entity = dao.findBySiteIdAndUserIdAndEmailIgnoreCase(1, null, "ray.sponsible@gmail.com")
        assertTrue(entity.isPresent)
    }

    @Test
    fun `unsubscribe from site - already unsubscibed`() {
        login("email")

        val url = "http://127.0.0.1:$port/v1/sites/100/list/members?email=ray.sponsible@gmail.com"
        delete(url)

        val entity = dao.findBySiteIdAndUserIdAndEmailIgnoreCase(100, null, "ray.sponsible@gmail.com")
        assertTrue(entity.isPresent)
    }

    @Test
    fun `unsubscribe from user`() {
        login("email")

        val url = "http://127.0.0.1:$port/v1/sites/11/list/members?email=ray.sponsible@gmail.com&user-id=77"
        delete(url)

        val entity = dao.findBySiteIdAndUserIdAndEmailIgnoreCase(11, 77, "ray.sponsible@gmail.com")
        assertTrue(entity.isPresent)
    }
}
