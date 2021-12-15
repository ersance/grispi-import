package com.grispi.grispiimport.zendesk.ticketfield

import com.grispi.grispiimport.grispi.*
import com.grispi.grispiimport.zendesk.SystemFieldOption
import com.grispi.grispiimport.zendesk.ZendeskEntity
import jodd.json.meta.JSON
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
class ZendeskTicketField: ZendeskEntity() {

    @Id
    @JSON(name = "id")
    var id: Long = -1

    @JSON(name = "url")
    var url: String? = null
    
    @JSON(name = "type")
    var type: String? = null

    @JSON(name = "title")
    var title: String? = null

    @JSON(name = "raw_title")
    var rawTitle: String? = null

    @JSON(name = "description")
    var description: String? = null

    @JSON(name = "raw_description")
    var rawDescription: String? = null

    @JSON(name = "position")
    var position: Int? = null

    @JSON(name = "active")
    var active: Boolean? = null

    @JSON(name = "required")
    var required: Boolean = false

    @JSON(name = "collapsed_for_agents")
    var collapsedForAgents: Boolean? = null

    @JSON(name = "regexp_for_validation")
    var regexpForValidation: String? = null

    @JSON(name = "title_in_portal")
    var titleInPortal: String? = null

    @JSON(name = "raw_title_in_portal")
    var rawTitleInPortal: String? = null

    @JSON(name = "visible_in_portal")
    var visibleInPortal: Boolean = false

    @JSON(name = "editable_in_portal")
    var editableInPortal: Boolean = false

    @JSON(name = "required_in_portal")
    var requiredInPortal: Boolean = false

    @JSON(name = "tag")
    var tag: String? = null

    @JSON(name = "removable")
    var removable: Boolean? = null

    @JSON(name = "agent_description")
    var agentDescription: String? = null

    @JSON(name = "custom_field_options")
    var customFieldOptions: List<CustomFieldOption>? = null

    @JSON(name = "system_field_options")
    var systemFieldOptions: List<SystemFieldOption>? = null

    @JSON(name = "sub_type_id")
    var subTypeId: Long? = null

    fun toGrispiTicketFieldRequest(): GrispiTicketFieldRequest {
        val permission = mapPermission()
        return GrispiTicketFieldRequest.Builder()
            .key("tiz.$id")
            .name("${title.toString()} ${id}")
            .type(mapType())
            .titleForAgents(title.toString())
            .titleForEndUsers(if (permission.isEndUserVisible()) titleInPortal.toString() else "")
            .descriptionForAgents(agentDescription.toString())
            .descriptionForEndUsers(if (permission.isEndUserVisible()) description.toString() else "")
            .required(mapRequired())
            .permission(permission)
            .attributes(mapAttributes())
            .options(mapOptions())
            .build()
    }

    companion object {
        val SYSTEM_FIELDS: Set<String> = setOf("subject", "description", "status", "tickettype", "group", "priority", "assignee")
    }

    private fun mapAttributes(): List<String> {
        val type = mapType()
        if (FieldType.TYPES_THAT_ALLOW_NEW_VALUES.contains(type)) {
            return listOf("ALLOW_NEW_VALUES")
        } else {
            return emptyList()
        }
    }

    private fun mapPermission(): FieldPermissions {
        return if (editableInPortal) {
            FieldPermissions.EDITABLE_BY_END_USERS
        } else if (visibleInPortal) {
            FieldPermissions.READONLY_FOR_END_USERS
        } else {
            FieldPermissions.AGENT_ONLY
        }
    }

    private fun mapOptions(): List<GrispiTicketFieldOption>? {

        val type = mapType()
        if (!FieldType.TYPES_THAT_ALLOW_OPTIONS.contains(type)) {
            return null;
        }

        if (customFieldOptions == null || customFieldOptions!!.isEmpty()) {
            return emptyList()
        }

        val gOptions: MutableList<GrispiTicketFieldOption>? = mutableListOf()
        for ((index, value) in customFieldOptions!!.withIndex()) {
            gOptions?.add(index, GrispiTicketFieldOption(index, value.name.toString(), value.value.toString()))
        }

        return gOptions
    }

    private fun mapType(): FieldType {
        return when (type) {
            "integer" -> FieldType.NUMBER_INTEGER
            "decimal" -> FieldType.NUMBER_DECIMAL
            "tagger" -> FieldType.SELECT
            "multiselect" -> FieldType.MULTI_SELECT
            "date" -> FieldType.DATE
            else -> FieldType.TEXT
        }
    }

    private fun mapRequired(): RequiredStatus {
        return if (required) {
            if (requiredInPortal) RequiredStatus.REQUIRED_TO_SOLVE else RequiredStatus.REQUIRED_TO_SUBMIT
        } else {
            RequiredStatus.NOT_REQUIRED
        }
    }

}

class ZendeskTicketFields {

    @JSON(name = "ticket_fields")
    val ticketFields: List<ZendeskTicketField> = emptyList()

}
