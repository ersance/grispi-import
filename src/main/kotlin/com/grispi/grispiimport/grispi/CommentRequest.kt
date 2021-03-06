package com.grispi.grispiimport.grispi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.grispi.grispiimport.zendesk.ZendeskCommentAttachment
import java.time.Instant

class CommentRequest(
    val body: String,
    val publicVisible: Boolean,
    val ticketKey: String,
    val createdAt: String,
    val creator: CommentCreator,
    val attachments: List<AttachmentRequest>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommentCreator(
    val id: String,
    val email: String?,
    val username: String?
)

data class AttachmentRequest(
    val url: String,
    val filename: String,
)