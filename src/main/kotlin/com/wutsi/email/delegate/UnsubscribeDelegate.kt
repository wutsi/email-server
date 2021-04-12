package com.wutsi.email.`delegate`

import com.wutsi.email.dao.UnsubscribedRepository
import com.wutsi.email.entity.Unsubscribed
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
public class UnsubscribeDelegate(
    @Autowired private val dao: UnsubscribedRepository
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(UnsubscribeDelegate::class.java)
    }

    @Transactional
    public fun invoke(
        siteId: Long,
        email: String,
        userId: Long? = null
    ) {
        val item = dao.findBySiteIdAndUserIdAndEmailIgnoreCase(siteId, userId, email)
        if (item.isPresent) {
            LOGGER.info("site_id=$siteId email=$email user_id=$userId - Email already removed from the list")
            return
        }

        dao.save(
            Unsubscribed(
                siteId = siteId,
                email = email.toLowerCase(),
                userId = userId
            )
        )
        LOGGER.info("site_id=$siteId email=$email user_id=$userId - Email removed from the list")
    }
}
