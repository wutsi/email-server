package com.wutsi.email.endpoint

import com.wutsi.email.`delegate`.UnsubscribeDelegate
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.`annotation`.DeleteMapping
import org.springframework.web.bind.`annotation`.PathVariable
import org.springframework.web.bind.`annotation`.RequestParam
import org.springframework.web.bind.`annotation`.RestController
import kotlin.Long
import kotlin.String

@RestController
public class UnsubscribeController(
    private val `delegate`: UnsubscribeDelegate
) {
    @DeleteMapping("/v1/sites/{site-id}/list/members")
    @PreAuthorize(value = "hasAuthority('email')")
    public fun invoke(
        @PathVariable(name = "site-id") siteId: Long,
        @RequestParam(name = "email", required = false) email: String,
        @RequestParam(name = "user-id", required = false) userId: Long? = null
    ) {
        delegate.invoke(siteId, email, userId)
    }
}
