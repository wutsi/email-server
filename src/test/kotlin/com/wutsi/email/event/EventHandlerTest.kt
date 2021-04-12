package com.wutsi.email.event

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.wutsi.email.delegate.UnsubscribeDelegate
import com.wutsi.stream.Event
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EventHandlerTest {
    private lateinit var delegate: UnsubscribeDelegate
    private lateinit var handler: EventHandler

    @BeforeEach
    fun setUp() {
        delegate = mock()
        handler = EventHandler(delegate)
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

        verify(delegate).invoke(11L, "ray.sponsible@gmail.com", 111L)
    }
}
