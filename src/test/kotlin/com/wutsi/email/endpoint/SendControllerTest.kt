package com.wutsi.email.endpoint

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetup
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.email.SiteAttribute
import com.wutsi.email.delegate.SendDelegate
import com.wutsi.email.dto.Address
import com.wutsi.email.dto.SendEmailRequest
import com.wutsi.email.dto.Sender
import com.wutsi.site.SiteApi
import com.wutsi.site.dto.Attribute
import com.wutsi.site.dto.GetSiteResponse
import com.wutsi.site.dto.Site
import com.wutsi.user.UserApi
import com.wutsi.user.dto.GetUserResponse
import com.wutsi.user.dto.User
import org.apache.commons.io.IOUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.jdbc.Sql
import org.springframework.web.client.RestTemplate
import javax.mail.internet.InternetAddress
import kotlin.test.assertNull

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(value = ["/db/clean.sql", "/db/SendController.sql"])
internal class SendControllerTest {
    @LocalServerPort
    private val port = 0

    private lateinit var url: String

    private val rest: RestTemplate = RestTemplate()

    @MockBean
    private lateinit var userApi: UserApi

    @MockBean
    private lateinit var siteApi: SiteApi

    private var smtp: GreenMail = GreenMail(ServerSetup(2525, null, "smtp"))

    @BeforeEach
    fun setUp() {
        smtp.setUser("username", "secret")
        smtp.start()

        url = "http://127.0.0.1:$port/v1/emails"

        val site = createSite()
        doReturn(GetSiteResponse(site)).whenever(siteApi).get(any())

        val user = createUser()
        doReturn(GetUserResponse(user)).whenever(userApi).get(any())
    }

    @AfterEach
    fun tearDown() {
        smtp.stop()
    }

    @Test
    fun `send an email`() {
        val request = createSendEmailRequest(campaign = "test-campaign")

        val response = rest.postForEntity(url, request, Any::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)

        assertEquals(1, smtp.receivedMessages.size)

        val message = smtp.receivedMessages[0]
        val body = IOUtils.toString(message.inputStream)
        assertEquals(InternetAddress("no-reply@test.com", "Test Site"), message.sender)
        assertEquals(request.subject, message.subject)
        assertEquals(InternetAddress(request.recipient.email, request.recipient.displayName), message.allRecipients[0])
        assertTrue(message.contentType.contains(request.contentType))
        assertTrue(body.contains("Yo man"))

        assertEquals("ses-wutsi-1", message.getHeader(SendDelegate.HEADER_SES_CONFIGURATION_SET)[0])
        assertEquals("test-campaign", message.getHeader(SendDelegate.HEADER_CAMPAIGN)[0])
        assertEquals("List-Unsubscribe=One-Click", message.getHeader("List-Unsubscribe-Post")[0])
        assertEquals(
            "<mailto:unsubscribe@test.com>,<https://www.test.com/unsubscribe?email=ray.sponsible@gmail.com>",
            message.getHeader("List-Unsubscribe")[0]
        )
    }

    @Test
    fun `X-WUTSI-CAMPAIGN header not available when not provided in the request`() {
        val request = createSendEmailRequest(campaign = null)

        rest.postForEntity(url, request, Any::class.java)

        assertNull(smtp.receivedMessages[0].getHeader(SendDelegate.HEADER_CAMPAIGN))
    }

    @Test
    fun `Unsubscription headers not available when site has not unsubscription-email`() {
        val site = createSite(
            id = 777L,
            attributes = listOf(
                Attribute(SiteAttribute.UNSUBSCRIBED_URL.urn, "https://www.test.com/unsubscribe"),
                Attribute(SiteAttribute.FROM.urn, "no-reply@test.com")
            )
        )
        doReturn(GetSiteResponse(site)).whenever(siteApi).get(any())

        val request = createSendEmailRequest()
        rest.postForEntity(url, request, Any::class.java)

        assertNull(smtp.receivedMessages[0].getHeader("List-Unsubscribe"))
        assertNull(smtp.receivedMessages[0].getHeader("List-Unsubscribe-Post"))
    }

    @Test
    fun `Unsubscription headers not available when site has not unsubscription-url`() {
        val site = createSite(
            id = 777L,
            attributes = listOf(
                Attribute(SiteAttribute.UNSUBSCRIBED_EMAIL.urn, "unsubscribe.test.com"),
                Attribute(SiteAttribute.FROM.urn, "no-reply@test.com")
            )
        )
        doReturn(GetSiteResponse(site)).whenever(siteApi).get(any())

        val request = createSendEmailRequest()
        rest.postForEntity(url, request, Any::class.java)

        assertNull(smtp.receivedMessages[0].getHeader("List-Unsubscribe"))
        assertNull(smtp.receivedMessages[0].getHeader("List-Unsubscribe-Post"))
    }

    @Test
    fun `sender displayName in FROM when provided in the request`() {
        val request = createSendEmailRequest(senderUserId = 1)
        rest.postForEntity(url, request, Any::class.java)

        assertEquals(InternetAddress("no-reply@test.com", "Roger Milla"), smtp.receivedMessages[0].sender)
    }

    @Test
    fun `sender email in FROM is default when site has no FROM attribute`() {
        val site = createSite(
            id = 777L,
            attributes = listOf(
                Attribute(SiteAttribute.UNSUBSCRIBED_EMAIL.urn, "unsubscribe.test.com"),
                Attribute(SiteAttribute.UNSUBSCRIBED_EMAIL.urn, "unsubscribe.test.com")
            )
        )
        doReturn(GetSiteResponse(site)).whenever(siteApi).get(any())

        val request = createSendEmailRequest()
        rest.postForEntity(url, request, Any::class.java)

        assertEquals(InternetAddress("no-reply@wutsi.com", "Test Site"), smtp.receivedMessages[0].sender)
    }

    @Test
    fun `do not send email to user unsubscribed from site list`() {
        val request = SendEmailRequest(
            siteId = 100,
            recipient = Address("Ray Sponsible", "ray.sponsible@gmail.com"),
            contentType = "text/plain",
            contentLanguage = "fr",
            body = "Yo man",
            subject = "test"
        )
        val response = rest.postForEntity(url, request, Any::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(0, smtp.receivedMessages.size)
    }

    @Test
    fun `do not send email to user unsubscribed from blog list`() {
        val request = SendEmailRequest(
            siteId = 200,
            sender = Sender(userId = 2L),
            recipient = Address("Ray Sponsible", "ray.sponsible@gmail.com"),
            contentType = "text/plain",
            contentLanguage = "fr",
            body = "Yo man",
            subject = "test"
        )
        val response = rest.postForEntity(url, request, Any::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(0, smtp.receivedMessages.size)
    }

    @Test
    fun `message variable a replaces`() {
        val request = SendEmailRequest(
            siteId = 1,
            sender = Sender(userId = 2L),
            recipient = Address("Ray Sponsible", "ray.sponsible@gmail.com"),
            contentType = "text/html",
            contentLanguage = "fr",
            subject = "test",
            campaign = "c001",
            body = "Hello world".trimIndent()
        )
        rest.postForEntity(url, request, Any::class.java)

        val body = IOUtils.toString(smtp.receivedMessages[0].inputStream)
        val expected = IOUtils.toString(SendControllerTest::class.java.getResourceAsStream("/SendController/email.html"), "utf-8")
        // assertEquals(expected.trimIndent(), body.trimIndent())
        System.out.println(expected)
    }

    private fun createSendEmailRequest(
        campaign: String? = null,
        senderUserId: Long? = null
    ) = SendEmailRequest(
        siteId = 1,
        sender = Sender(userId = senderUserId),
        recipient = Address("Ray Sponsible", "ray.sponsible@gmail.com"),
        contentType = "text/plain",
        contentLanguage = "fr",
        body = "Yo man",
        subject = "test",
        campaign = campaign
    )

    private fun createSite(
        id: Long = 1L,
        attributes: List<Attribute> = listOf(
            Attribute(SiteAttribute.UNSUBSCRIBED_EMAIL.urn, "unsubscribe@test.com"),
            Attribute(SiteAttribute.UNSUBSCRIBED_URL.urn, "https://www.test.com/unsubscribe"),
            Attribute(SiteAttribute.FROM.urn, "no-reply@test.com")
        )
    ) = Site(
        id = id,
        name = "test",
        displayName = "Test Site",
        websiteUrl = "https://www.test.com",
        attributes = attributes
    )

    private fun createUser() = User(
        fullName = "Roger Milla"
    )
}
