package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.Role
import com.grispi.grispiimport.grispi.User
import com.grispi.grispiimport.grispi.UserRequest
import jodd.json.meta.JSON

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

    // TODO: 30.11.2021 validate phone number and send null if invalid
    fun toGrispiUserRequest(): UserRequest {
        return UserRequest.Builder()
            .email(email ?: generateEmail())
            .password(User.NO_PASSWORD)
            .fullName(name.toString())
            .phone(if (PhoneNumberValidator.isValid(phone.toString())) phone else "null")
            .role(mapRole())
            .tags(tags)
            .tags("zendesk-import")
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
