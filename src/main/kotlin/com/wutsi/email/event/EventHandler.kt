package com.wutsi.email.event

import com.wutsi.email.delegate.SendDelegate
import com.wutsi.email.delegate.UnsubscribeDelegate
import com.wutsi.stream.Event
import com.wutsi.stream.ObjectMapperBuilder
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
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
        LOGGER.info("onEvent($event)")

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
        val payload = ObjectMapperBuilder().build().readValue(event.payload, DeliverySubmittedEventPayload::class.java)
        sendDelegate.invoke(payload.request)
    }
}
