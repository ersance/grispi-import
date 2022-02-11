package com.grispi.grispiimport.grispi

import jodd.json.JsonSerializer

/**
 * Created on November, 2021
 *
 * @author destan
 */
open class UserRequest private constructor(
    val email: String?,
    val phone: String?,
    val fullName: String?,
    val role: String?,
    val organizationId: Long?,
    val tags: Set<String>?,
    val groups: Set<Long>?,
    val fields: Set<TicketRequest.FieldFromUi_>?
): GrispiApiRequest() {

    class Builder {
        private var email: String? = null
        private var active: Boolean? = false
        private var phone: String? = null
        private var fullName: String? = null
        private var role: String? = null
        private var tags: MutableSet<String> = mutableSetOf()
        private var groups: Set<Long>? = mutableSetOf()
        private var organizationId: Long? = null
        private var fields: MutableSet<TicketRequest.FieldFromUi_> = mutableSetOf()

        fun email(email: String?) = apply { this.email = email }
        fun active(active: Boolean?) = apply { this.active = active }
        fun phone(phone: String?) = apply { this.phone = phone }
        fun fullName(fullName: String) = apply { this.fullName = fullName }
        fun role(role: Role) = apply { this.role = role.authority }
        fun tags(vararg tags: String) = apply { this.tags.addAll(tags.toSet()) }
        fun tags(tags: Set<String>) = apply { this.tags.addAll(tags) }
        fun groups(groups: Set<Long>?) = apply { this.groups = groups ?: emptySet() }
        fun organizationId(organizationId: Long?) = apply { this.organizationId = organizationId }
        fun fields(fields: MutableSet<TicketRequest.FieldFromUi_>) = apply { this.fields.addAll(fields) }

        fun build(): UserRequest {
            return UserRequest(email, phone, fullName, role, organizationId, tags, groups, fields)
        }

        fun toJson(): String {
            val userRequest = build()
            return userRequest.toJson()
        }
    }
}