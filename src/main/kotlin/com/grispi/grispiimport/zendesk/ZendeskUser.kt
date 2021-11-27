package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.Role
import com.grispi.grispiimport.grispi.User
import com.grispi.grispiimport.grispi.UserRequest
import jodd.json.meta.JSON

class ZendeskUser {

    @JSON(name = "id")
    val id: Long? = null

    @JSON(name = "name")
    val name: String? = null

    @JSON(name = "email")
    val email: String? = null

    @JSON(name = "phone")
    val phone: String? = null

    @JSON(name = "role")
    val role: String? = null

    @JSON(name = "tags")
    val tags: Set<String> = emptySet()

    fun toGrispiUserRequest(): UserRequest {
        return UserRequest.Builder()
            .email(email.toString())
            .password(User.NO_PASSWORD)
            .fullName(name.toString())
            .phone(phone.toString())
            .role(mapRole())
            .tags(tags)
            .build()
    }

    private fun mapRole(): Role {
        return when (role) {
            "agent" -> Role.AGENT
            "admin" -> Role.ADMIN
            else -> Role.END_USER
        }
    }

}
