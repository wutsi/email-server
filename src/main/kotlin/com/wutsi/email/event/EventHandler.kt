package com.wutsi.email.event

import com.sun.mail.smtp.SMTPSendFailedException
import com.wutsi.email.delegate.SendDelegate
import com.wutsi.email.delegate.UnsubscribeDelegate
import com.wutsi.stream.Event
import com.wutsi.stream.ObjectMapperBuilder
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.mail.MailException
import org.springframework.stereotype.Service

@Service
class EventHandler(
    private val unsubscribeDelegate: UnsubscribeDelegate,
    private val sendDelegate: SendDelegate
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(EventHandler::class.java)
    }

    @EventListener
    fun onEvent(event: Event) {
        LOGGER.info("onEvent(...)")

        if (event.type == EmailEventType.UNSUBSCRIPTION_SUBMITTED.urn) {
            onUnsubscription(event)
        } else if (event.type == EmailEventType.DELIVERY_SUBMITTED.urn) {
            onDelivery(event)
        } else {
            LOGGER.info("Event Ignored")
        }
    }

    private fun onUnsubscription(event: Event) {
        val payload = ObjectMapperBuilder().build().readValue(event.payload, UnsubscriptionSubmittedEventPayload::class.java)
        unsubscribeDelegate.invoke(
            siteId = payload.siteId,
            email = payload.email,
            userId = payload.userId
        )
    }

    private fun onDelivery(event: Event) {
        try {
            val payload = ObjectMapperBuilder().build().readValue(event.payload, DeliverySubmittedEventPayload::class.java)
            sendDelegate.invoke(payload.request)
        } catch (ex: MailException) {
            LOGGER.warn("Email delivery error", ex)
            handleException(ex)
        }
    }

    /**
     * Refer to https://en.wikipedia.org/wiki/List_of_SMTP_server_return_codes
     */
    private fun handleException(ex: MailException) {
        if (ex.cause !is SMTPSendFailedException)
            throw ex

        val returnCode = (ex.cause as SMTPSendFailedException).returnCode ?: throw ex
        val errorCategory = returnCode / 100
        if (errorCategory == 4 || errorCategory == 5) {
            // Permanent error
        } else {
            throw ex
        }
    }
}
