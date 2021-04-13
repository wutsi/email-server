package com.wutsi.email.service

import com.github.mustachejava.DefaultMustacheFactory
import com.wutsi.email.SiteAttribute
import com.wutsi.email.dto.SendEmailRequest
import com.wutsi.site.dto.Site
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.InputStreamReader
import java.io.StringWriter
import java.util.regex.Pattern

@Service
class EmailBodyComposer {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(EmailBodyComposer::class.java)
        private val HREF_REGEXP = Pattern.compile("href=[\"|'](.*?)[\"|']")
    }

    fun compose(request: SendEmailRequest, site: Site, template: String = "default"): String {
        val content = generateBody(request, site, template)
        return injectUTMParameters(content, request.campaign)
    }

    private fun generateBody(request: SendEmailRequest, site: Site, template: String = "default"): String {
        val reader = InputStreamReader(EmailBodyComposer::class.java.getResourceAsStream("/templates/$template.html"))
        reader.use {
            val writer = StringWriter()
            writer.use {
                val mustache = DefaultMustacheFactory().compile(reader, "text")
                val scope = scope(request, site)
                mustache.execute(
                    writer,
                    mapOf(
                        "scope" to scope
                    )
                )
                writer.flush()
                return writer.toString()
            }
        }
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
                LOGGER.warn("Unable to replace $url by $xurl", ex)
            }
        }
        m.appendTail(sb)
        return sb.toString()
    }

    private fun scope(request: SendEmailRequest, site: Site) = mapOf(
        "unsubscribeUrl" to unsubscribeUrl(request, site),
        "websiteUrl" to site.websiteUrl,
        "siteDisplayName" to site.displayName,
        "smallLogoUrl" to smallLogoUrl(site),
        "body" to request.body
    )

    private fun appendUTMParametersToURL(url: String, campaign: String?): String {
        val params = "utm_source=email" +
            if (campaign == null) "" else "&utm_campaign=$campaign"
        return if (url.contains('?')) "$url&$params" else "$url?$params"
    }

    public fun unsubscribeUrl(request: SendEmailRequest, site: Site): String? {
        val url = site.attributes.find { it.urn == SiteAttribute.UNSUBSCRIBED_URL.urn }?.value ?: return null

        var params = "email=${request.recipient.email}"
        if (request.sender.userId != null)
            params += "&u=${request.sender.userId}"

        return if (url.contains('?'))
            "$url&$params"
        else
            "$url?$params"
    }

    private fun smallLogoUrl(site: Site): String? =
        site.attributes.find { it.urn == SiteAttribute.SMALL_LOGO_URL.urn }?.value
}
