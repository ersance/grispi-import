package com.grispi.grispiimport.grispi

import jodd.json.JsonSerializer

/**
 * Created on November, 2021
 *
 * @author destan
 */
class TicketRequest private constructor(
    val channel: TicketChannel?,
    val formId: Long?,
    val comment: Comment_?,
    val fields: Set<FieldFromUi_>?
) {

    companion object {
        fun isSystemField(key: String): Boolean {
            return key.startsWith("ticket.system.");
        }
    }

    fun toJson(): String {
        return JsonSerializer().deep(true).serialize(this)
    }

    data class User_(val userId: String?, val email: String, val phoneNumber: String?) {
        override fun toString(): String {
            return "${userId}:${email}:${phoneNumber}"
        }
    }

    data class Comment_(val body: String, val publicVisible: Boolean = true, val creator: User_? = null, val attachmentIds: Set<String>? = null)

    data class FieldFromUi_(val key: String, val value: String?)

    class Builder {
        private var channel: TicketChannel? = null
        private var formId: Long? = null
        private var comment: Comment_? = null
        private var fields: MutableSet<FieldFromUi_> = mutableSetOf()

        fun channel(channel: TicketChannel) = apply { this.channel = channel }
        fun formId(formId: Long) = apply { this.formId = formId }
        fun comment(comment: Comment_) = apply { this.comment = comment }

        // System Fields
        fun subject(subject: String) = apply { fields.add(FieldFromUi_("ticket.system.subject", subject)) }
        fun creator(userId: String?, email: String, phoneNumber: String?) =
            apply { fields.add(FieldFromUi_("ticket.system.key", "${userId}:${email}:${phoneNumber}")) }

        fun requester(userId: String?, email: String, phoneNumber: String?) =
            apply { fields.add(FieldFromUi_("ticket.system.requester", "${userId}:${email}:${phoneNumber}")) }

        fun assignee(groupId: String, userId: String?) = apply { fields.add(FieldFromUi_("ticket.system.assignee", "${groupId}:${userId}")) }
        fun assignee(group: Group, user: User?) = apply { assignee(group.id.toString(), user?.id.toString()) }
        fun status(status: TicketStatus) = apply { fields.add(FieldFromUi_("ticket.system.status", status.name)) }
        fun tags(vararg tags: String) = apply { fields.add(FieldFromUi_("ticket.system.tags", tags.joinToString(","))) }
        fun tags(tags: Set<String>) = apply { fields.add(FieldFromUi_("ticket.system.tags", tags.joinToString(","))) }
        fun type(type: TicketType?) = apply { fields.add(FieldFromUi_("ticket.system.type", type?.name)) }
        fun priority(priority: TicketPriority?) = apply { fields.add(FieldFromUi_("ticket.system.priority", priority?.name)) }
        fun followers(vararg followers: User_) = apply { fields.add(FieldFromUi_("ticket.system.followers", followers.joinToString(","))) }
        fun followers(followers: Set<Long>) = apply { fields.add(FieldFromUi_("ticket.system.followers", followers.joinToString(","))) }
        fun emailCcs(vararg emailCcs: User_) = apply { fields.add(FieldFromUi_("ticket.system.email_ccs", emailCcs.joinToString(","))) }
        fun emailCcs(emailCcs: Set<Long>) = apply { fields.add(FieldFromUi_("ticket.system.email_ccs", emailCcs.joinToString(","))) }

        // Custom fields
        fun customField(key: String, value: String) = apply {
            assert(!isSystemField(key)) { "This function can only be used for custom fields" }
            fields.add(FieldFromUi_(key, value))
        }
        fun customField(cFields: Map<Long, Any?>?) = apply {
            cFields?.entries?.forEach { entry ->
                run {
                    if (entry.value is Collection<*>) {
                        fields.add(FieldFromUi_(entry.key.toString(), (entry.value as Collection<*>).joinToString(",")))
                    } else {
                        fields.add(FieldFromUi_(entry.key.toString(), entry.value.toString()))
                    }
                }
            }
        }

//        fun customField(customFieldDefinition: FieldDefinition, value: String) = apply { customField(customFieldDefinition.key, value) }
//        fun customField(customFieldDefinition: FieldDefinition, value: User_) = apply { customField(customFieldDefinition.key, value.toString()) }
//        fun customField(customFieldDefinition: FieldDefinition, vararg value: User_) =
//            apply { customField(customFieldDefinition.key, value.joinToString(",")) }

        fun build(): TicketRequest {
            val fieldsResult = if (fields.isEmpty()) null else fields
            return TicketRequest(channel, formId, comment, fieldsResult)
        }

        fun toJson(): String {
            val ticketRequest = build()
            return ticketRequest.toJson()
        }
    }
}