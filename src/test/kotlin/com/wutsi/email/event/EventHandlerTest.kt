package com.wutsi.email.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.wutsi.email.delegate.SendDelegate
import com.wutsi.email.delegate.UnsubscribeDelegate
import com.wutsi.email.dto.SendEmailRequest
import com.wutsi.stream.Event
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EventHandlerTest {
    private lateinit var unsubscribeDelegate: UnsubscribeDelegate
    private lateinit var sendDelegate: SendDelegate
    private lateinit var handler: EventHandler

    @BeforeEach
    fun setUp() {
        unsubscribeDelegate = mock()
        sendDelegate = mock()
        handler = EventHandler(unsubscribeDelegate, sendDelegate)
    }

    @Test
    fun `handle unsubscription event`() {
        val event = Event(
            type = EmailEventType.UNSUBSCRIPTION_SUBMITTED.urn,
            payload = """
                {
                    "siteId": 11,
                    "userId": 111,
                    "email": "ray.sponsible@gmail.com"
                }
            """.trimIndent()
        )

        handler.onEvent(event)

        verify(unsubscribeDelegate).invoke(11L, "ray.sponsible@gmail.com", 111L)
    }

    @Test
    fun `handle email event`() {
        val request = SendEmailRequest()
        val event = Event(
            type = EmailEventType.DELIVERY_SUBMITTED.urn,
            payload = ObjectMapper().writeValueAsString(DeliverySubmittedEventPayload(request))
        )

        handler.onEvent(event)

        verify(sendDelegate).invoke(request)
    }
}
