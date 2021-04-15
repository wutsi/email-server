package com.wutsi.email.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.sun.mail.smtp.SMTPSendFailedException
import com.wutsi.email.delegate.SendDelegate
import com.wutsi.email.delegate.UnsubscribeDelegate
import com.wutsi.email.dto.SendEmailRequest
import com.wutsi.stream.Event
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mail.MailException
import org.springframework.mail.MailSendException

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

    @Test
    fun `ignore 4xx email error code`() {
        val request = SendEmailRequest()
        val event = Event(
            type = EmailEventType.DELIVERY_SUBMITTED.urn,
            payload = ObjectMapper().writeValueAsString(DeliverySubmittedEventPayload(request))
        )

        doThrow(createMailException(421)).whenever(sendDelegate).invoke(any())

        handler.onEvent(event)
    }

    @Test
    fun `ignore 5xx email error code`() {
        val request = SendEmailRequest()
        val event = Event(
            type = EmailEventType.DELIVERY_SUBMITTED.urn,
            payload = ObjectMapper().writeValueAsString(DeliverySubmittedEventPayload(request))
        )

        doThrow(createMailException(500)).whenever(sendDelegate).invoke(any())

        handler.onEvent(event)
    }

    @Test
    fun `rethrow any error`() {
        val request = SendEmailRequest()
        val event = Event(
            type = EmailEventType.DELIVERY_SUBMITTED.urn,
            payload = ObjectMapper().writeValueAsString(DeliverySubmittedEventPayload(request))
        )

        doThrow(IllegalStateException::class).whenever(sendDelegate).invoke(any())

        assertThrows<IllegalStateException> { handler.onEvent(event) }
    }

    private fun createMailException(returnCode: Int): MailException {
        return MailSendException("Yo", SMTPSendFailedException("XxX", returnCode, "Man", null, emptyArray(), emptyArray(), emptyArray()))
    }
}
