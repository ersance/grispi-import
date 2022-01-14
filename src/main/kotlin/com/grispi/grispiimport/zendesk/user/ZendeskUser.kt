package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.*
import jodd.json.meta.JSON
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.stream.Collectors

@Document
class ZendeskUser: ZendeskEntity() {

    @Id
    @JSON(name = "id")
    var id: Long = -1

    @JSON(name = "active")
    var active: Boolean = true

    @JSON(name = "name")
    var name: String? = null

    @JSON(name = "email")
    var email: String? = null

    @JSON(name = "phone")
    var phone: String? = null

    @JSON(name = "role")
    var role: String? = null

    @JSON(name = "tags")
    var tags: MutableSet<String> = mutableSetOf()

    @JSON(name = "external_id")
    var externalId: String? = null

    @JSON(name = "default_group_id")
    var defaultGroupId: Long? = null

    @JSON(name = "organization_id")
    var organizationId: Long? = null

    @JSON(name = "user_fields")
    var userFields: Map<String, String> = mapOf()

    fun toGrispiUserRequest(findGrispiGroupIds: (Long) -> Set<String>?, findGrispiOrganizationId: (Long) -> String?): UserRequest {
        val userFieldSet = mutableSetOf<TicketRequest.FieldFromUi_>()
        if (PhoneNumberValidator.isValid(phone.toString())) {
            userFieldSet.add(TicketRequest.FieldFromUi_(GrispiUserFieldRequest.Builder.ZENDESK_PHONE_USER_FIELD_KEY, phone))
        }
        if (externalId != null) {
            userFieldSet.add(TicketRequest.FieldFromUi_(GrispiUserFieldRequest.Builder.ZENDESK_EXTERNAL_ID_USER_FIELD_KEY, externalId))
        }

        val groups = if (defaultGroupId != null) {
            findGrispiGroupIds.invoke(id)!!.stream().map { it.toLong() }.collect(Collectors.toSet())
        } else null

        val organizationId = if (organizationId != null) findGrispiOrganizationId.invoke(organizationId!!)?.toLong() else null

        userFields.map { TicketRequest.FieldFromUi_("uiz.${it.key}", it.value) }.toCollection(userFieldSet)

        return UserRequest.Builder()
            .email(email ?: generateEmail())
            .fullName(name.toString())
            .phone(if (PhoneNumberValidator.isValid(phone.toString())) phone else "null")
            .role(mapRole())
            .tags(tags)
            .tags("zendesk-import")
            .groups(groups)
            .organizationId(organizationId)
            .fields(userFieldSet)
            .build()
    }

    private fun mapRole(): Role {
        return when (role) {
            "agent" -> Role.AGENT
            "admin" -> Role.ADMIN
            else -> Role.END_USER
        }
    }

    private fun generateEmail(): String {
        return "${phone?:id}@example.com"
    }

}

class ZendeskUsers {

    @JSON(name = "users")
    val users: List<ZendeskUser> = emptyList()

}

class ZendeskDeletedUsers {

    @JSON(name = "deleted_users")
    val users: List<ZendeskUser> = emptyList()

}
