package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.Role
import com.grispi.grispiimport.grispi.User
import com.grispi.grispiimport.grispi.UserRequest
import jodd.json.meta.JSON
import java.util.stream.Collectors

class ZendeskUser {

    @JSON(name = "id")
    val id: Long = -1

    @JSON(name = "name")
    val name: String? = null

    @JSON(name = "email")
    val email: String? = null

    @JSON(name = "phone")
    val phone: String? = null

    @JSON(name = "role")
    val role: String? = null

    @JSON(name = "tags")
    val tags: MutableSet<String> = mutableSetOf()

    @JSON(name = "user_fields")
    val userFields: Map<String, String> = mapOf()

    fun toGrispiUserRequest(): UserRequest {
        return UserRequest.Builder()
            .email(email ?: generateEmail())
            .password(User.NO_PASSWORD)
            .fullName(name.toString())
            .phone(if (PhoneNumberValidator.isValid(phone.toString())) phone else "null")
            .role(mapRole())
            .tags(tags)
            .tags("zendesk-import")
            .fields(userFields.mapKeys { "uiz.${it.key}" })
            .build()
    }

    private fun mapRole(): Role {
        return when (role) {
            "agent" -> Role.AGENT
            "admin" -> Role.ADMIN
            else -> Role.END_USER
        }
    }

    private fun generateEmail(): String? {
        return if (phone != null) {
            "${phone}@example.com"
        } else {
            "${System.currentTimeMillis()}@example.com" // TODO
        }
    }

}
