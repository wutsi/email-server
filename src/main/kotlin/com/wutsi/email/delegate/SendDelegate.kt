package com.wutsi.email.`delegate`

import com.wutsi.email.SiteAttribute
import com.wutsi.email.dao.UnsubscribedRepository
import com.wutsi.email.dto.Address
import com.wutsi.email.dto.SendEmailRequest
import com.wutsi.site.SiteApi
import com.wutsi.site.dto.Site
import com.wutsi.user.UserApi
import org.apache.commons.text.StringEscapeUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import java.util.regex.Pattern
import javax.mail.Message
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

@Service
public class SendDelegate(
    @Autowired private val sender: JavaMailSender,
    @Autowired private val dao: UnsubscribedRepository,
    @Autowired private val userApi: UserApi,
    @Autowired private val siteApi: SiteApi,

    @Value("\${spring.mail.properties.mail.smtp.from}") private val from: String

) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(SendDelegate::class.java)
        private val HREF_REGEXP = Pattern.compile("href=[\"|'](.*?)[\"|']")

        const val HEADER_SES_CONFIGURATION_SET = "X-SES-CONFIGURATION-SET"
        const val HEADER_CAMPAIGN = "X-WUTSI-CAMPAIGN"
    }

    public fun invoke(request: SendEmailRequest) {
        if (isUnsubscribed(request)) {
            LOGGER.info("site_id=${request.siteId} recipient_email=${request.recipient.email} - User has unsubscribed from the list")
            return
        }
        val site = siteApi.get(request.siteId).site
        val message = createMessage(request, site)
        sender.send(message)
    }

    private fun createMessage(request: SendEmailRequest, site: Site): MimeMessage {
        val unsubscribeUrl = unsubscribeUrl(request, site)
        val unsubscribeEmail = unsubscribeEmail(site)
        val body = getBody(request, unsubscribeUrl, site)
        val from = InternetAddress(fromEmail(site), fromDisplayName(request, site))

        val message = sender.createMimeMessage()
        message.addRecipients(Message.RecipientType.TO, request.recipient.email)
        message.setFrom(from)
        message.sender = from
        message.subject = request.subject
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

    private fun getBody(request: SendEmailRequest, unsubscribeUrl: String?, site: Site): String? {
        val body = injectVariables(request.body, unsubscribeUrl, request.recipient, site)
        return injectUTMParameters(body, request.campaign)
    }

    private fun injectVariables(body: String, unsubscribeUrl: String?, recipient: Address, site: Site): String {
        var result = body.replace("%RECIPIENT_NAME%", StringEscapeUtils.escapeHtml4(recipient.displayName))
            .replace("%WEBSITE_URL%", site.websiteUrl)
        if (unsubscribeUrl != null) {
            result = result.replace("%UNSUBSCRIBE_URL%", unsubscribeUrl)
        }
        return result
    }

    /**
     * Modify URL to inject Google Analytics UTM parameters.
     * See https://support.google.com/analytics/answer/1033863?hl=en
     */
    private fun injectUTMParameters(body: String, campaign: String?): String {
        val m = HREF_REGEXP.matcher(body)
        val sb = StringBuffer()
        while (m.find()) {
            val url = m.group(0)
            val xurl = appendUTMParametersToURL(url.substring(6, url.length - 1), campaign)
            try {
                m.appendReplacement(sb, "href=\"$xurl\"")
            } catch (ex: Exception) {
                LOGGER.error("Unable to replace $url by $xurl", ex)
                throw ex
            }
        }
        m.appendTail(sb)
        return sb.toString()
    }

    private fun appendUTMParametersToURL(url: String, campaign: String?): String {
        val params = "utm_source=email" +
            if (campaign == null) "" else "&utm_campaign=$campaign"
        return if (url.contains('?')) "$url&$params" else "$url?$params"
    }

    private fun sesConfigSet(site: Site): String? =
        "ses-wutsi-${site.id}"

    private fun fromEmail(site: Site): String? {
        val value = site.attributes.find { it.urn == SiteAttribute.FROM.urn }?.value
        return if (value != null)
            value
        else
            this.from
    }

    private fun unsubscribeEmail(site: Site): String? =
        site.attributes.find { it.urn == SiteAttribute.UNSUBSCRIBED_EMAIL.urn }?.value

    private fun unsubscribeUrl(request: SendEmailRequest, site: Site): String? {
        val url = site.attributes.find { it.urn == SiteAttribute.UNSUBSCRIBED_URL.urn }?.value ?: return null

        var params = "email=${request.recipient.email}"
        if (request.sender?.userId != null)
            params += "&u=${request.sender.userId}"

        return if (url.contains('?'))
            "$url?$params"
        else
            "$url&$params"
    }
}
