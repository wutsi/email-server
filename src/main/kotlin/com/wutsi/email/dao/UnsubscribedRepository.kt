package com.wutsi.email.dao

import com.wutsi.email.entity.Unsubscribed
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UnsubscribedRepository : CrudRepository<Unsubscribed, Long> {
    fun findBySiteIdAndUserIdAndEmailIgnoreCase(siteId: Long, userId: Long?, email: String): Optional<Unsubscribed>
}
