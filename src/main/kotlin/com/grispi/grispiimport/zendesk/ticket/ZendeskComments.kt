package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.AttachmentRequest
import com.grispi.grispiimport.grispi.CommentCreator
import com.grispi.grispiimport.grispi.CommentRequest
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketChannel
import com.vdurmont.emoji.EmojiParser
import jodd.json.meta.JSON
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import kotlin.reflect.KFunction1

class ZendeskComments {

    @JSON(name = "tickets")
    var comments: List<ZendeskComment> = emptyList()

}

@Document
class ZendeskComment: ZendeskEntity() {

    @Id
    @JSON(name = "id")
    var id: Long = -1

    @JSON(name = "author_id")
    var authorId: Long = -1

    @JSON(name = "body")
    var body: String = ""

    @JSON(name = "html_body")
    var htmlBody: String = ""

    @JSON(name = "plain_body")
    var plainBody: String = ""

    @JSON(name = "public")
    var public: Boolean = true

    @JSON(name = "audit_id")
    var auditId: Long = -1

    @JSON(name = "via")
    var channel: ZendeskTicketChannel = ZendeskTicketChannel()

    @JSON(name = "created_at")
    var createdAt: Instant = Instant.now()

    @JSON(name = "attachments")
    var attachments: List<ZendeskCommentAttachment> = mutableListOf()

    var ticketId: Long? = null

    fun toCommentRequest(
        getGrispiTicketKey: KFunction1<Long, String>,
        getGrispiUserId: KFunction1<Long, String>
    ): CommentRequest {
        val ticketKey = getGrispiTicketKey.invoke(id)
        val grispiUserId = getGrispiUserId.invoke(authorId)
        val attachmentRequests = attachments.map { AttachmentRequest(it.url.toString(), it.filename.toString()) }
        return CommentRequest(EmojiParser.parseToAliases(htmlBody), public, ticketKey, createdAt.toString(), CommentCreator(grispiUserId.toLong(), null, null), attachmentRequests)
    }
}

class ZendeskCommentAttachment {

    @JSON(name = "content_url")
    var url: String? = null

    @JSON(name = "file_name")
    var filename: String? = null

}
