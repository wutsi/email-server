package com.wutsi.email.event

import com.wutsi.email.delegate.UnsubscribeDelegate
import com.wutsi.stream.Event
import com.wutsi.stream.ObjectMapperBuilder
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class EventHandler(private val delegate: UnsubscribeDelegate) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(EventHandler::class.java)
    }

    @EventListener
    fun onEvent(event: Event) {
        LOGGER.info("onEvent($event)")

        if (event.type == EmailEventType.UNSUBSCRIPTION_SUBMITTED.urn) {
            val payload = ObjectMapperBuilder().build().readValue(event.payload, UnsubscriptionRequestedEventPayload::class.java)
            delegate.invoke(
                siteId = payload.siteId,
                email = payload.email,
                userId = payload.userId
            )
        } else {
            LOGGER.info("Event Ignored")
        }
    }
}
