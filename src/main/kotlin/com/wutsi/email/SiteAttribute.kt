package com.wutsi.email

enum class SiteAttribute(val urn: String) {
    FROM("urn:attribute:wutsi:email:from-email"),
    UNSUBSCRIBED_EMAIL("urn:attribute:wutsi:email:unsubscribe-email"),
    UNSUBSCRIBED_URL("urn:attribute:wutsi:email:unsubscribe-url"),
}
