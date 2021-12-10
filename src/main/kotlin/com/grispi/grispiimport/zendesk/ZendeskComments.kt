package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.AttachmentRequest
import com.grispi.grispiimport.grispi.CommentCreator
import com.grispi.grispiimport.grispi.CommentRequest
import com.vdurmont.emoji.EmojiParser
import jodd.json.meta.JSON
import java.time.Instant

class ZendeskComments {

    @JSON(name = "tickets")
    val comments: List<ZendeskComment> = emptyList()

    @JSON(name = "count")
    val count: Int = 0

}

class ZendeskComment {

    @JSON(name = "id")
    val id: Long = -1

    @JSON(name = "author_id")
    val authorId: Long = -1

    @JSON(name = "body")
    val body: String = ""

    @JSON(name = "html_body")
    val htmlBody: String = ""

    @JSON(name = "plain_body")
    val plainBody: String = ""

    @JSON(name = "public")
    val public: Boolean = true

    @JSON(name = "audit_id")
    val auditId: Long = -1

    @JSON(name = "via")
    val channel: ZendeskTicketChannel = ZendeskTicketChannel()

    @JSON(name = "created_at")
    val createdAt: Instant = Instant.now()

    @JSON(name = "attachments")
    val attachments: List<ZendeskCommentAttachment> = mutableListOf()

    fun toCommentRequest(
        operationId: String,
        ticketKey: String,
        getGrispiUserId: (String, Long) -> Long
    ): CommentRequest {
        val grispiUserId = getGrispiUserId.invoke(operationId, authorId)
        val attachmentRequests = attachments.map { AttachmentRequest(it.url.toString(), it.filename.toString()) }
        return CommentRequest(EmojiParser.parseToAliases(htmlBody), public, ticketKey, createdAt.toString(), CommentCreator(grispiUserId, null, null), attachmentRequests)
    }

//    fun toCommentRequest(
//    ): CommentRequest {
//        return CommentRequest(htmlBody, public, "ticketKey", createdAt.toString(), CommentCreator(100, null, null), attachments)
//    }

}

class ZendeskCommentAttachment {

    @JSON(name = "content_url")
    val url: String? = null

    @JSON(name = "file_name")
    val filename: String? = null

}
