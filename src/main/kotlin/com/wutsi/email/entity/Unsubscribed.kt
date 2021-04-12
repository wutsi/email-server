package com.wutsi.email.entity

import java.time.OffsetDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "T_UNSUBSCRIBED")
data class Unsubscribed(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "site_id")
    var siteId: Long = -1,

    @Column(name = "user_id")
    var userId: Long? = null,

    val email: String = "",

    @Column(name = "unsubscribed_date_time")
    var unsubscribedDateTime: OffsetDateTime = OffsetDateTime.now()
)
