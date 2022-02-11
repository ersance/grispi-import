package com.grispi.grispiimport.zendesk.ticket

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.grispi.grispiimport.grispi.AttachmentRequest
import com.grispi.grispiimport.grispi.CommentCreator
import com.grispi.grispiimport.grispi.CommentRequest
import com.grispi.grispiimport.zendesk.ZendeskComment
import com.mongodb.BasicDBObject
import org.bson.codecs.pojo.annotations.BsonProperty
import org.springframework.data.mongodb.MongoExpression
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.*
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
class ZendeskTicketCommentAggregationRepository(
    private val mongoTemplate: MongoTemplate
) {

    fun findCommentedTickets(operationId: String): MutableList<GroupedComments> {

        val commentAggregation = newAggregation (
            match(Criteria.where("operationId").`is`(operationId)),
            lookup("zendeskMapping", "ticketId", "zendeskId", "ticketKeyMapping"),
            lookup("zendeskMapping", "authorId", "zendeskId", "authorIdMapping"),
            unwind("ticketKeyMapping", "0"),
            unwind("authorIdMapping", "0"),
            project(Fields.from(
                Fields.field("ticketKey", "\$ticketKeyMapping.grispiId"),
                Fields.field("publicVisible", "\$public"),
                Fields.field("body"),
                Fields.field("createdAt"),
                Fields.field("attachments"),
            )).and("creator").nested(Fields.from(Fields.field("creatorId", "\$authorIdMapping.grispiId"))),
            group("ticketKey").last("ticketKey").`as`("ticketKey").push("\$\$ROOT").`as`("comments")
        ).withOptions(AggregationOptions.builder().allowDiskUse(true).build())

        return mongoTemplate.aggregate(commentAggregation, ZendeskComment::class.java, GroupedComments::class.java).mappedResults
    }

}

@JsonIgnoreProperties
data class GroupedComments(val ticketKey: String, val comments: List<CommentedTickets>)

data class CommentAuthor(
    val creatorId: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommentedTickets(val ticketKey: String, val creator: CommentAuthor, val publicVisible: Boolean, val body: String, val createdAt: Instant, val attachments: List<AttachmentRequest>?) {

    fun toCommentRequest(): CommentRequest {
        return CommentRequest(
            this.body,
            this.publicVisible,
            this.ticketKey,
            this.createdAt.toString(),
            CommentCreator(this.creator.creatorId, null, null),
            this.attachments ?: emptyList()
        )
    }

}