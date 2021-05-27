package com.wutsi.email.`delegate`

import com.wutsi.email.dao.UnsubscribedRepository
import com.wutsi.email.dto.SendEmailRequest
import com.wutsi.email.service.EmailBodyComposer
import com.wutsi.email.service.EmailStyleEnhancer
import com.wutsi.platform.site.SiteProvider
import com.wutsi.site.SiteAttribute
import com.wutsi.site.dto.Site
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.springframework.util.DigestUtils
import java.nio.charset.Charset
import javax.mail.Message
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

@Service
public class SendDelegate(
    @Autowired private val sender: JavaMailSender,
    @Autowired private val dao: UnsubscribedRepository,
    @Autowired private val siteProvider: SiteProvider,
    @Autowired private val bodyComposer: EmailBodyComposer,
    @Autowired private val styleEnhancer: EmailStyleEnhancer,
    @Autowired private val cacheManager: CacheManager,
    @Value("\${spring.mail.properties.mail.smtp.from}") private val from: String

) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(SendDelegate::class.java)

        const val HEADER_SES_CONFIGURATION_SET = "X-SES-CONFIGURATION-SET"
        const val HEADER_CAMPAIGN = "X-WUTSI-CAMPAIGN"
    }

    public fun invoke(request: SendEmailRequest) {
        if (wasEmailRecentlySent(request))
            return

        send(request)
        emailSent(request)
    }

    private fun wasEmailRecentlySent(request: SendEmailRequest): Boolean {
        val key = cacheKey(request)
        return cacheManager.getCache("default")?.get(key, String::class.java) != null
    }

    private fun emailSent(request: SendEmailRequest) {
        val key = cacheKey(request)
        cacheManager.getCache("default")?.put(key, "1")
    }

    private fun cacheKey(request: SendEmailRequest): String {
        val key = request.recipient.email.toLowerCase() + " " +
            request.subject.toLowerCase() + " " +
            request.siteId
        return "email_" + DigestUtils.md5DigestAsHex(key.toByteArray(Charset.defaultCharset()))
    }

    private fun send(request: SendEmailRequest) {
        if (request.recipient.email.isNullOrEmpty()) {
            LOGGER.warn("site_id=${request.siteId} campaign=${request.campaign} recipient_email=${request.recipient.email} - No recipient email")
            return
        }
        if (isUnsubscribed(request)) {
            LOGGER.warn("site_id=${request.siteId} campaign=${request.campaign} recipient_email=${request.recipient.email} - User has unsubscribed from the list")
            return
        }

        LOGGER.info("site_id=${request.siteId} campaign=${request.campaign} recipient_email=${request.recipient.email} subject=${request.subject} - Sending email")
        val site = siteProvider.get(request.siteId)
        val message = createMessage(request, site)
        sender.send(message)
    }

    private fun createMessage(request: SendEmailRequest, site: Site): MimeMessage {
        val unsubscribeUrl = unsubscribeUrl(request, site)
        val unsubscribeEmail = unsubscribeEmail(site)
        val body = generateBody(request, site)
        val from = InternetAddress(fromEmail(site), fromDisplayName(request, site))

        val message = sender.createMimeMessage()
        message.addRecipients(Message.RecipientType.TO, request.recipient.email)
        message.setFrom(from)
        message.sender = from
        message.subject = request.subject
        message.contentLanguage = arrayOf(request.contentLanguage)
        message.setContent(body, request.contentType)

        if (!unsubscribeEmail.isNullOrEmpty() && !unsubscribeUrl.isNullOrEmpty()) {
            message.addHeader("List-Unsubscribe", "<mailto:$unsubscribeEmail>,<$unsubscribeUrl>")
            message.addHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click")
        }

        val configSet = sesConfigSet(site)
        if (!configSet.isNullOrEmpty())
            message.addHeader(HEADER_SES_CONFIGURATION_SET, configSet)

        if (!request.campaign.isNullOrEmpty())
            message.addHeader(HEADER_CAMPAIGN, request.campaign)

        return message
    }

    private fun generateBody(request: SendEmailRequest, site: Site): String {
        val body = bodyComposer.compose(request, site, "default")
        return styleEnhancer.enhance(body)
    }

    private fun isUnsubscribed(request: SendEmailRequest): Boolean =
        dao.findBySiteIdAndUserIdAndEmailIgnoreCase(
            siteId = request.siteId,
            userId = request.sender.userId,
            email = request.recipient.email
        ).isPresent

    private fun fromDisplayName(request: SendEmailRequest, site: Site): String? {
        return if (request.sender.fullName.isNullOrEmpty())
            site.displayName
        else
            request.sender.fullName
    }

    private fun sesConfigSet(site: Site): String? =
        "ses-wutsi"

    private fun fromEmail(site: Site): String =
        site.attributes.find { it.urn == SiteAttribute.EMAIL_FROM.urn }?.value ?: this.from

    private fun unsubscribeEmail(site: Site): String? =
        site.attributes.find { it.urn == SiteAttribute.EMAIL_UNSUBSCRIBED_EMAIL.urn }?.value

    private fun unsubscribeUrl(request: SendEmailRequest, site: Site): String? =
        bodyComposer.unsubscribeUrl(request, site)
}
