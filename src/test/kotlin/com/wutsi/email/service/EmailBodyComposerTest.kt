package com.wutsi.email.service

import com.wutsi.email.SiteAttribute.FROM
import com.wutsi.email.SiteAttribute.SMALL_LOGO_URL
import com.wutsi.email.SiteAttribute.UNSUBSCRIBED_EMAIL
import com.wutsi.email.SiteAttribute.UNSUBSCRIBED_URL
import com.wutsi.email.dto.Address
import com.wutsi.email.dto.SendEmailRequest
import com.wutsi.email.dto.Sender
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
            body = "Hello <a href='https://www.google.com'>World</a>",
            recipient = Address("Ray Sponsible", "ray.sponsible@gmail.com"),
            sender = Sender(userId = 33),
            siteId = 1,
            subject = "This is the subject",
            campaign = "CMP001"
        )
        val content = composer.compose(request, site, "default")

        val expected = IOUtils.toString(EmailBodyComposerTest::class.java.getResourceAsStream("/EmailBodyComposer/default.html"), "utf-8")
        assertEquals(expected.trimIndent(), content.trimMargin())
    }

    private fun createSite(
        id: Long = 1L,
        attributes: List<Attribute> = listOf(
            Attribute(UNSUBSCRIBED_EMAIL.urn, "unsubscribe.test.com"),
            Attribute(UNSUBSCRIBED_URL.urn, "https://www.wutsi.com/unsubscribe"),
            Attribute(FROM.urn, "no-reply@test.com"),
            Attribute(SMALL_LOGO_URL.urn, "https://www.wutsi.com/logos/small.png")
        )
    ) = Site(
        id = id,
        name = "wutsi",
        displayName = "Wutsi",
        websiteUrl = "https://www.wutsi.com",
        attributes = attributes
    )
}
