package com.grispi.grispiimport.grispi

import jodd.json.JsonSerializer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter


/**
 * Created on November, 2021
 *
 * @author destan
 */
class TicketRequest private constructor(
    val channel: TicketChannel?,
    val formId: Long?,
    val comments: Set<Comment_?>,
    val fields: Set<FieldFromUi_>?,
) {

    companion object {
        fun isSystemField(key: String): Boolean {
            return key.startsWith("ts.");
        }
    }

    fun toJson(): String {
        return JsonSerializer().deep(true).serialize(this)
    }

    override fun toString(): String {
        return "TicketRequest(fields=$fields)"
    }


    data class User_(val id: String?, val email: String, val phoneNumber: String?) {
        override fun toString(): String {
            return "${id}:${email}:${phoneNumber}"
        }
    }

    /**
     * creator id is grispi user id
     */
    data class Comment_ private constructor(val body: String, val publicVisible: Boolean = true, val creator: User_? = null, val createdAt: String, val attachmentIds: Set<String>? = null) {
        constructor(
            body: String,
            publicVisible: Boolean = true,
            creator: String,
            createdAt: Instant,
            attachmentIds: Set<String>? = null,
        ) : this(body, publicVisible, User_(creator,"import@example.com", null), createdAt.toString(), attachmentIds)
    }

    data class FieldFromUi_(val key: String, val value: String?)

    class Builder {
        private var channel: TicketChannel? = null
        private var formId: Long? = null
        private var comment: Comment_? = null
        private var fields: MutableSet<FieldFromUi_> = mutableSetOf()

        fun channel(channel: TicketChannel) = apply { this.channel = channel }
        fun formId(formId: Long?) = apply { this.formId = formId }
        fun comment(comment: Comment_) = apply { this.comment = comment }

        // System Fields
        fun subject(subject: String) = apply { fields.add(FieldFromUi_("ts.subject", subject)) }
        fun creator(userId: String?, email: String, phoneNumber: String?) =
            apply { fields.add(FieldFromUi_("ts.creator", "${userId}:${email}:${phoneNumber}")) }

        fun requester(userId: String?, email: String, phoneNumber: String?) =
            apply { fields.add(FieldFromUi_("ts.requester", "${userId}:${email}:${phoneNumber}")) }

        fun assignee(groupId: String?, userId: String?) = apply {
            if (groupId != null) {
                fields.add(FieldFromUi_("ts.assignee", "${groupId}:${userId}"))
            }
        }
        fun assignee(group: Group, user: User?) = apply { assignee(group.id.toString(), user?.id.toString()) }
        fun status(status: TicketStatus) = apply { fields.add(FieldFromUi_("ts.status", status.name)) }
        fun tags(vararg tags: String) = apply { fields.add(FieldFromUi_("ts.tags", tags.joinToString(","))) }
        fun tags(tags: Set<String>) = apply { fields.add(FieldFromUi_("ts.tags", tags.joinToString(","))) }
        fun type(type: TicketType?) = apply { fields.add(FieldFromUi_("ts.type", type?.name)) }
        fun priority(priority: TicketPriority?) = apply { fields.add(FieldFromUi_("ts.priority", priority?.name)) }
        fun followers(vararg followers: User_) = apply { fields.add(FieldFromUi_("ts.followers", followers.joinToString(","))) }
        fun followers(followers: Set<Long>) = apply { fields.add(FieldFromUi_("ts.followers", followers.joinToString(","))) }
        fun emailCcs(vararg emailCcs: User_) = apply { fields.add(FieldFromUi_("ts.email_ccs", emailCcs.joinToString(","))) }
        fun emailCcs(emailCcs: Set<Long>) = apply { fields.add(FieldFromUi_("ts.email_ccs", emailCcs.joinToString(","))) }

        // Custom fields
        fun customField(key: String, value: String) = apply {
            assert(!isSystemField(key)) { "This function can only be used for custom fields" }
            fields.add(FieldFromUi_(key, value))
        }

        fun customField(cFields: Map<String, Any?>?) = apply {
            cFields?.entries?.forEach { entry ->
                run {
                    if (entry.value is Collection<*>) {
                        fields.add(FieldFromUi_(entry.key, (entry.value as Collection<*>).joinToString(",")))
                    }
                    else {
                        val value = convertDateToMillis(entry.value.toString()) ?: entry.value.toString()
                        fields.add(FieldFromUi_(entry.key, value))
                    }
                }
            }
        }

        fun build(): TicketRequest {
            val fieldsResult = if (fields.isEmpty()) null else fields
            return TicketRequest(channel, formId, setOf(comment), fieldsResult)
        }

        fun toJson(): String {
            val ticketRequest = build()
            return ticketRequest.toJson()
        }

        private fun convertDateToMillis(value: String): String? {
            try {
                val localDate = LocalDate.parse(value, DateTimeFormatter.ISO_DATE)
                val instant = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                return instant.toEpochMilli().toString()
            } catch (ex: RuntimeException) {
                return null;
            }
        }

    }
}