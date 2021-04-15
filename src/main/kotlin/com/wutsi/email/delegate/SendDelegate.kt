package com.wutsi.email.`delegate`

import com.wutsi.email.SiteAttribute
import com.wutsi.email.dao.UnsubscribedRepository
import com.wutsi.email.dto.SendEmailRequest
import com.wutsi.email.service.EmailBodyComposer
import com.wutsi.email.service.EmailStyleEnhancer
import com.wutsi.site.SiteApi
import com.wutsi.site.dto.Site
import com.wutsi.user.UserApi
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import javax.mail.Message
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

@Service
public class SendDelegate(
    @Autowired private val sender: JavaMailSender,
    @Autowired private val dao: UnsubscribedRepository,
    @Autowired private val userApi: UserApi,
    @Autowired private val siteApi: SiteApi,
    @Autowired private val bodyComposer: EmailBodyComposer,
    @Autowired private val styleEnhancer: EmailStyleEnhancer,
    @Value("\${spring.mail.properties.mail.smtp.from}") private val from: String

) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(SendDelegate::class.java)

        const val HEADER_SES_CONFIGURATION_SET = "X-SES-CONFIGURATION-SET"
        const val HEADER_CAMPAIGN = "X-WUTSI-CAMPAIGN"
    }

    public fun invoke(request: SendEmailRequest) {
        if (isUnsubscribed(request)) {
            LOGGER.info("site_id=${request.siteId} campaign=${request.campaign} recipient_email=${request.recipient.email} - User has unsubscribed from the list")
            return
        }

        LOGGER.info("site_id=${request.siteId} campaign=${request.campaign} recipient_email=${request.recipient.email} - Sending email")
        val site = siteApi.get(request.siteId).site
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

        LOGGER.info("--------------------------")
        LOGGER.info("----- Request.body\n${request.body}")
        LOGGER.info("--------------------------")
        LOGGER.info("----- Email.body\n$body")
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
        return if (request.sender.userId != null)
            userApi.get(request.sender.userId).user.fullName
        else
            site.displayName
    }

    private fun sesConfigSet(site: Site): String? =
        "ses-wutsi-${site.id}"

    private fun fromEmail(site: Site): String =
        site.attributes.find { it.urn == SiteAttribute.FROM.urn }?.value ?: this.from

    private fun unsubscribeEmail(site: Site): String? =
        site.attributes.find { it.urn == SiteAttribute.UNSUBSCRIBED_EMAIL.urn }?.value

    private fun unsubscribeUrl(request: SendEmailRequest, site: Site): String? =
        bodyComposer.unsubscribeUrl(request, site)
}
