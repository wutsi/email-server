package com.wutsi.email.service

import com.wutsi.email.dto.Address
import com.wutsi.email.dto.SendEmailRequest
import com.wutsi.email.dto.Sender
import com.wutsi.site.SiteAttribute.EMAIL_FROM
import com.wutsi.site.SiteAttribute.EMAIL_SMALL_LOGO_URL
import com.wutsi.site.SiteAttribute.EMAIL_UNSUBSCRIBED_EMAIL
import com.wutsi.site.SiteAttribute.EMAIL_UNSUBSCRIBED_URL
import com.wutsi.site.dto.Attribute
import com.wutsi.site.dto.Site
import org.apache.commons.io.IOUtils
import kotlin.test.Test
import kotlin.test.assertEquals

internal class EmailBodyComposerTest {
    val composer = EmailBodyComposer()

    @Test
    fun defaultTemplate() {
        val site = createSite()
        val request = SendEmailRequest(
            body = IOUtils.toString(EmailBodyComposerTest::class.java.getResourceAsStream("/EmailBodyComposer/body.html"), "utf-8"),
            recipient = Address("Ray Sponsible", "ray.sponsible@gmail.com"),
            sender = Sender(userId = 33),
            siteId = 1,
            subject = "This is the subject",
            campaign = "CMP001"
        )
        val content = composer.compose(request, site, "default")

        println(content)
        val expected = IOUtils.toString(EmailBodyComposerTest::class.java.getResourceAsStream("/EmailBodyComposer/email.html"), "utf-8")
        assertEquals(sanitizeHtml(expected), sanitizeHtml(content))
    }

    private fun sanitizeHtml(html: String): String =
        html.replace("\\s+".toRegex(), " ")
            .trimIndent()
            .trim()

    private fun createSite(
        id: Long = 1L,
        attributes: List<Attribute> = listOf(
            Attribute(EMAIL_UNSUBSCRIBED_EMAIL.urn, "unsubscribe.test.com"),
            Attribute(EMAIL_UNSUBSCRIBED_URL.urn, "https://www.wutsi.com/unsubscribe"),
            Attribute(EMAIL_FROM.urn, "no-reply@test.com"),
            Attribute(EMAIL_SMALL_LOGO_URL.urn, "https://www.wutsi.com/logos/small.png")
        )
    ) = Site(
        id = id,
        name = "wutsi",
        displayName = "Wutsi",
        websiteUrl = "https://www.wutsi.com",
        attributes = attributes
    )
}
