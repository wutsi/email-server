package com.wutsi.email.dto

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import kotlin.Long
import kotlin.String

public data class SendEmailRequest(
    @get:NotNull
    public val siteId: Long = 0,
    public val sender: Sender = Sender(),
    @get:NotNull
    public val recipient: Address = Address(),
    @get:NotBlank
    public val subject: String = "",
    public val body: String = "",
    @get:NotBlank
    public val contentType: String = "",
    public val campaign: String? = null
)
