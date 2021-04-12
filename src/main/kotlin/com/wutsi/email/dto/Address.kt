package com.wutsi.email.dto

import javax.validation.constraints.NotBlank
import kotlin.String

public data class Address(
    public val displayName: String? = null,
    @get:NotBlank
    public val email: String = ""
)
