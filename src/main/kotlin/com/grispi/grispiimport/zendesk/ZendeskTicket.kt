package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.TicketPriority
import com.grispi.grispiimport.grispi.TicketRequest
import com.grispi.grispiimport.grispi.TicketStatus
import com.grispi.grispiimport.grispi.TicketType
import jodd.json.meta.JSON
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
    val description: String? = null

    @JSON(name = "priority")
    val priority: String? = null

    @JSON(name = "status")
    val status: String? = null

//    @JSON(name = "recipient")
//    val recipient: Any? = null

    @JSON(name = "requester_id")
    val requesterId: Long = -1

    @JSON(name = "submitter_id")
    val submitterId: Long? = null

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
    val tags: Set<String> = emptySet()

    @JSON(name = "ticket_form_id")
    val ticketFormId: Long = -1

    @JSON(name = "custom_fields")
    val fields: List<ZendeskCustomField> = emptyList()

    // TODO: 27.11.2021 comment - creator(email)
    fun toTicketRequest(): TicketRequest {
        return TicketRequest.Builder()
            .channel(channel.toGrispiChannel())
            .formId(ticketFormId)
            .subject(subject.toString())
            .creator(submitterId.toString(),"aa@aa.com", "+905551112233")
            .requester(requesterId.toString(),"aa@aa.com", "+905551112233")
            .assignee(groupId.toString(), assigneeId.toString())
            .status(mapStatus())
            .tags(tags)
            .type(mapType())
            .priority(mapPriority())
            .followers(followerIds)
            .emailCcs(emailCcIds)
            .customField(fields.stream().filter { field -> field.value != null }.collect(Collectors.toMap(ZendeskCustomField::id, ZendeskCustomField::value)))
            .build()
    }

    private fun mapType(): TicketType? {
        return when (status) {
            "question" -> TicketType.QUESTION
            "incident" -> TicketType.INCIDENT
            "task" -> TicketType.TASK
            "problem" -> TicketType.PROBLEM
            else -> null
        }
    }

    private fun mapStatus(): TicketStatus {
        return when (status) {
            "open" -> TicketStatus.OPEN
            "pending" -> TicketStatus.PENDING
            "new" -> TicketStatus.NEW
            else -> TicketStatus.SOLVED
        }
    }

    private fun mapPriority(): TicketPriority? {
        return when (status) {
            "low" -> TicketPriority.LOW
            "normal" -> TicketPriority.NORMAL
            "high" -> TicketPriority.HIGH
            "urgent" -> TicketPriority.URGENT
            else -> null
        }
    }

}
