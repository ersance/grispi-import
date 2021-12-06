package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.TicketPriority
import com.grispi.grispiimport.grispi.TicketRequest
import com.grispi.grispiimport.grispi.TicketStatus
import com.grispi.grispiimport.grispi.TicketType
import jodd.json.meta.JSON
import java.lang.IllegalArgumentException
import java.time.Instant
import java.time.LocalDateTime
import java.util.stream.Collectors

class ZendeskTicket {

    @JSON(name = "id")
    val id: Long? = null

    @JSON(name = "via")
    val channel: ZendeskTicketChannel = ZendeskTicketChannel()

    @JSON(name = "subject")
    val subject: String? = null

    @JSON(name = "raw_subject")
    val rawSubject: String? = null

    @JSON(name = "description")
    val description: String = ""

    @JSON(name = "priority")
    val priority: String? = null

    @JSON(name = "status")
    val status: String? = null

    @JSON(name = "type")
    val type: String? = null

    @JSON(name = "created_at")
    val createdAt: Instant = Instant.now()

//    @JSON(name = "updated_at")
//    @DateTimeFormat(pattern = "yyyy-MM-dd'T'hh:mm:ssZ")
//    val updatedAt: Date? = null

    @JSON(name = "requester_id")
    val requesterId: Long = -1

    @JSON(name = "submitter_id")
    val submitterId: Long = -1

    @JSON(name = "assignee_id")
    val assigneeId: Long? = null

    @JSON(name = "organization_id")
    val organizationId: Long? = null

    @JSON(name = "group_id")
    val groupId: Long? = null

    @JSON(name = "collaborator_ids")
    val collaboratorIds: Set<Long> = emptySet()

    @JSON(name = "follower_ids")
    val followerIds: Set<Long> = emptySet()

    @JSON(name = "followup_ids")
    val followupIds: Set<Long> = emptySet()

    @JSON(name = "email_cc_ids")
    val emailCcIds: Set<Long> = emptySet()

    @JSON(name = "tags")
    val tags: Set<String> = mutableSetOf()

    @JSON(name = "ticket_form_id")
    val ticketFormId: Long = -1

    @JSON(name = "custom_fields")
    val fields: List<ZendeskCustomField> = emptyList()

    // TODO: 27.11.2021 comment - creator(email)
    // TODO: 30.11.2021 Creator & requester cannot be null and the users for them should be imported
    fun toTicketRequest(
        getGrispiUserId: (Long) -> Long?,
        getGrispiGroupId: (Long) -> Long?
    ): TicketRequest {

        val grispiAssigneeId = if (assigneeId != null) getGrispiUserId.invoke(assigneeId) else null
        val grispiFollowerIds = mutableSetOf<Long>()
        if (followerIds.isNotEmpty()) {
            followerIds.stream().map { fId -> getGrispiUserId.invoke(fId) }.collect(Collectors.toCollection { grispiFollowerIds })
        }
        val grispiEmailCcIds = mutableSetOf<Long>()
        if (emailCcIds.isNotEmpty()) {
            emailCcIds.stream().map { fId -> getGrispiUserId.invoke(fId) }.collect(Collectors.toCollection { grispiEmailCcIds })
        }

        val grispiGroupId = if (groupId != null) getGrispiGroupId(groupId).toString() else null
        val grispiCreatorId = getGrispiUserId(submitterId).toString()
        val grispiRequesterId = getGrispiUserId(requesterId).toString()

        return TicketRequest.Builder()
            .channel(channel.toGrispiChannel())
            .formId(ticketFormId)
            .subject(subject.toString())
            .creator(grispiCreatorId,"null", "null")
            .requester(grispiRequesterId,"null", "null")
            .assignee(grispiGroupId, grispiAssigneeId.toString())
            .status(mapStatus())
            .tags(tags)
            .type(mapType())
            .priority(mapPriority())
            .followers(grispiFollowerIds)
            .emailCcs(grispiEmailCcIds)
            .comment(TicketRequest.Comment_(description, true, grispiCreatorId, createdAt))
            .customField(fields.stream().filter { field -> field.value != null }.collect(Collectors.toMap(ZendeskCustomField::toGrispiKey, ZendeskCustomField::value)))
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

}
