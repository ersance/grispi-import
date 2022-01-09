package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.*
import com.grispi.grispiimport.zendesk.ticket.ZendeskCustomField
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketChannel
import com.vdurmont.emoji.EmojiParser
import jodd.json.meta.JSON
import org.apache.commons.lang3.StringUtils
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.lang.IllegalArgumentException
import java.time.Instant
import java.util.stream.Collectors
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2

@Document
class ZendeskTicket: ZendeskEntity() {

    @Id
    @JSON(name = "id")
    var id: Long = -1

    @JSON(name = "via")
    var channel: ZendeskTicketChannel = ZendeskTicketChannel()

    @JSON(name = "subject")
    var subject: String? = null

    @JSON(name = "raw_subject")
    var rawSubject: String? = null

    @JSON(name = "description")
    var description: String = ""

    @JSON(name = "priority")
    var priority: String? = null

    @JSON(name = "status")
    var status: String? = null

    @JSON(name = "type")
    var type: String? = null

    @JSON(name = "created_at")
    var createdAt: Instant = Instant.now()

    @JSON(name = "requester_id")
    var requesterId: Long = -1

    @JSON(name = "submitter_id")
    var submitterId: Long = -1

    @JSON(name = "assignee_id")
    var assigneeId: Long? = null

    @JSON(name = "organization_id")
    var organizationId: Long? = null

    @JSON(name = "group_id")
    var groupId: Long? = null

    @JSON(name = "brand_id")
    var brandId: Long? = null

    @JSON(name = "collaborator_ids")
    var collaboratorIds: Set<Long> = emptySet()

    @JSON(name = "follower_ids")
    var followerIds: Set<Long> = emptySet()

    @JSON(name = "followup_ids")
    var followupIds: Set<Long> = emptySet()

    @JSON(name = "email_cc_ids")
    var emailCcIds: Set<Long> = emptySet()

    @JSON(name = "tags")
    var tags: Set<String> = mutableSetOf()

    @JSON(name = "ticket_form_id")
    var ticketFormId: Long = -1

    @JSON(name = "comment_count")
    var commentCount: Int = 1

    @JSON(name = "custom_fields")
    var fields: MutableList<ZendeskCustomField> = mutableListOf()

    // TODO: 27.11.2021 comment - creator(email)
    // TODO: 30.11.2021 Creator & requester cannot be null and the users for them should be imported
    fun toTicketRequest(
        getGrispiUserId: KFunction1<Long, String>,
        getGrispiGroupId: KFunction1<Long, String?>,
        getGrispiTicketFormId: KFunction1<Long, String>
    ): TicketRequest {
        val grispiAssigneeId = if (assigneeId != null) getGrispiUserId.invoke(assigneeId!!) else null
        val grispiFollowerIds = mutableSetOf<Long>()
        if (followerIds.isNotEmpty()) {
            followerIds.stream().map { fId -> getGrispiUserId.invoke(fId) }.collect(Collectors.toCollection { grispiFollowerIds })
        }
        val grispiEmailCcIds = mutableSetOf<Long>()
        if (emailCcIds.isNotEmpty()) {
            emailCcIds.stream().map { fId -> getGrispiUserId.invoke(fId) }.collect(Collectors.toCollection { grispiEmailCcIds })
        }

        val grispiGroupId = if (groupId != null) getGrispiGroupId(groupId!!).toString() else null
        val grispiCreatorId = getGrispiUserId(submitterId)
        val grispiRequesterId = getGrispiUserId(requesterId)

        val subjectSafe = if (StringUtils.isBlank(subject.toString())) {
            description.subSequence(0, 50).toString()
        } else {
            subject.toString()
        }

        return TicketRequest.Builder()
            .channel(channel.toGrispiChannel())
            .formId(getGrispiTicketFormId.invoke(ticketFormId).toLong())
            .subject(subjectSafe)
            .creator(grispiCreatorId,"null", "null")
            .requester(grispiRequesterId,"null", "null")
            .assignee(grispiGroupId, grispiAssigneeId.toString())
            .status(mapStatus())
            .tags(tags)
            .type(mapType())
            .priority(mapPriority())
            .followers(grispiFollowerIds)
            .emailCcs(grispiEmailCcIds)
            .comment(TicketRequest.Comment_(EmojiParser.parseToAliases(description), true, grispiCreatorId, createdAt))
            .customField(GrispiTicketFieldRequest.Builder.ZENDESK_ID_CUSTOM_FIELD, id.toString())
            .customField(GrispiTicketFieldRequest.Builder.ZENDESK_BRAND_ID_CUSTOM_FIELD, brandId.toString())
            .customField(fields.stream()
                .filter { field -> field.value != null }
                .collect(Collectors.toMap(ZendeskCustomField::toGrispiKey, ZendeskCustomField::value))
            )
            .build()
    }

    private fun mapType(): TicketType? {
        if (type == null) return null

        return when (type) {
            "question" -> TicketType.QUESTION
            "incident" -> TicketType.INCIDENT
            "task" -> TicketType.TASK
            "problem" -> TicketType.PROBLEM
            else -> throw IllegalArgumentException("Unexpected zendesk ticket type: '${type}'")
        }
    }

    private fun mapStatus(): TicketStatus {
        return when (status) {
            "open" -> TicketStatus.OPEN
            "pending" -> TicketStatus.PENDING
            "new" -> TicketStatus.NEW
            "solved" -> TicketStatus.SOLVED
            "closed" -> TicketStatus.CLOSED
            "hold" -> TicketStatus.PENDING
            else -> throw IllegalArgumentException("Unexpected zendesk ticket status: '${status}'")
        }
    }

    private fun mapPriority(): TicketPriority? {
        if (priority == null) return null

        return when (priority) {
            "low" -> TicketPriority.LOW
            "normal" -> TicketPriority.NORMAL
            "high" -> TicketPriority.HIGH
            "urgent" -> TicketPriority.URGENT
            else -> throw IllegalArgumentException("Unexpected zendesk ticket priority: '${priority}'")
        }
    }

    override fun toString(): String {
        return "ZendeskTicket(id=$id, subject=$subject, createdAt=$createdAt)"
    }

}

class ZendeskTickets {

    @JSON(name = "tickets")
    val tickets: List<ZendeskTicket> = emptyList()

}