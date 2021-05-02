package com.wutsi.email.endpoint

import com.wutsi.email.`delegate`.SendDelegate
import com.wutsi.email.dto.SendEmailRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.`annotation`.PostMapping
import org.springframework.web.bind.`annotation`.RequestBody
import org.springframework.web.bind.`annotation`.RestController
import javax.validation.Valid

@RestController
public class SendController(
    private val `delegate`: SendDelegate
) {
    @PostMapping("/v1/emails")
    @PreAuthorize(value = "hasAuthority('email')")
    public fun invoke(@Valid @RequestBody request: SendEmailRequest) {
        delegate.invoke(request)
    }
}
