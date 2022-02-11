package com.grispi.grispiimport.zendesk

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Document
data class CommentMap(
    @Id
    val ticketId: Long,
    val apiAvailable: Boolean? = null,
    val callingZendesk: Boolean? = false,
    val requested: Boolean? = false,
    val waiting: Boolean? = false,
    val fetched: Boolean? = false,
    val saved: Boolean? = false,
    val exception: String? = "",
)

@Repository
interface CommentMapRepository: MongoRepository<CommentMap, String>