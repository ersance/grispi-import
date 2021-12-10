package com.grispi.grispiimport.grispi

import jodd.json.JsonSerializer

/**
 * Created on November, 2021
 *
 * @author destan
 */
open class UserRequest private constructor(
    val email: String?,
    val password: String?,
    val phone: String?,
    val fullName: String?,
    val role: String?,
    val tags: Set<String>?,
    val fields: Map<String, String>?
): GrispiApiRequest() {

    class Builder {
        private var email: String? = null
        private var password: String? = null
        private var phone: String? = null
        private var fullName: String? = null
        private var role: String? = null
        private var tags: MutableSet<String> = mutableSetOf()
        private var fields: Map<String, String>? = mapOf()

        fun email(email: String?) = apply { this.email = email }
        fun password(password: String) = apply { this.password = password }
        fun phone(phone: String?) = apply { this.phone = phone }
        fun fullName(fullName: String) = apply { this.fullName = fullName }
        fun role(role: Role) = apply { this.role = role.authority }
        fun tags(vararg tags: String) = apply { this.tags.addAll(tags.toSet()) }
        fun tags(tags: Set<String>) = apply { this.tags.addAll(tags) }
        fun fields(fields: Map<String, String>?) = apply { this.fields = fields }

        fun build(): UserRequest {
            return UserRequest(email, password, phone, fullName, role, tags, fields)
        }

        fun toJson(): String {
            val userRequest = build()
            return userRequest.toJson()
        }
    }

    companion object {

        fun agent(): UserRequest {
            return Builder()
                .role(Role.AGENT)
                .build()
        }

        fun endUser(): UserRequest {
            return Builder()
                .role(Role.END_USER)
                .build()
        }

        fun admin(): UserRequest {
            return Builder()
                .role(Role.ADMIN)
                .build()
        }
    }

    class UserResponse(val id: Long, userRequest: UserRequest): UserRequest(
        email = userRequest.email,
        password = userRequest.password,
        phone = userRequest.phone,
        fullName = userRequest.fullName,
        role = userRequest.role,
        tags = userRequest.tags,
        fields = userRequest.fields
    )
}