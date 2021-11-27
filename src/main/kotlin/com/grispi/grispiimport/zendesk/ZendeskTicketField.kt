package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.*
import jodd.json.meta.JSON
import org.springframework.format.annotation.DateTimeFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType


class ZendeskTicketField {

    @JSON(name = "id")
    val id: Long? = null

    @JSON(name = "url")
    val url: String? = null
    
    @JSON(name = "type")
    val type: String? = null

    @JSON(name = "title")
    val title: String? = null

    @JSON(name = "raw_title")
    val rawTitle: String? = null

    @JSON(name = "description")
    val description: String? = null

    @JSON(name = "raw_description")
    val rawDescription: String? = null

    @JSON(name = "position")
    val position: Int? = null

    @JSON(name = "active")
    val active: Boolean? = null

    @JSON(name = "required")
    val required: Boolean = false

    @JSON(name = "collapsed_for_agents")
    val collapsedForAgents: Boolean? = null

    @JSON(name = "regexp_for_validation")
    val regexpForValidation: String? = null

    @JSON(name = "title_in_portal")
    val titleInPortal: String? = null

    @JSON(name = "raw_title_in_portal")
    val rawTitleInPortal: String? = null

    @JSON(name = "visible_in_portal")
    val visibleInPortal: Boolean? = null

    @JSON(name = "editable_in_portal")
    val editableInPortal: Boolean? = null

    @JSON(name = "required_in_portal")
    val requiredInPortal: Boolean = false

    @JSON(name = "tag")
    val tag: String? = null

//    @JSON(name = "created_at")
//    @DateTimeFormat(pattern = "yyyy-MM-dd'T'hh:mm:ssZ")
//    val createdAt: Date? = null
//
//    @JSON(name = "updated_at")
//    @DateTimeFormat(pattern = "yyyy-MM-dd'T'hh:mm:ssZ")
//    val updatedAt: Date? = null

    @JSON(name = "removable")
    val removable: Boolean? = null

    @JSON(name = "agent_description")
    val agentDescription: String? = null

    @JSON(name = "custom_field_options")
    val customFieldOptions: List<CustomFieldOption>? = null

    @JSON(name = "system_field_options")
    val systemFieldOptions: List<SystemFieldOption>? = null

    @JSON(name = "sub_type_id")
    val subTypeId: Long? = null

    fun toGrispiTicketField(): GrispiTicketField {
        return GrispiTicketField.Builder()
            .key(id.toString())
            .name(title.toString())
            .type(mapType())
            .titleForAgents(rawTitle.toString())
            .titleForEndUsers(title.toString())
            .descriptionForAgents(agentDescription.toString())
            .descriptionForEndUsers(description.toString())
            .required(mapRequired())
            .permission(mapPermission())
            .attributes(mapAttributes())
            .options(mapOptions())
            .build()
    }

    // TODO
    private fun mapAttributes(): List<String>? {
        return listOf("ALLOW_NEW_VALUES")
    }

    // TODO:
    private fun mapPermission(): FieldPermissions {
        return FieldPermissions.EDITABLE_BY_END_USERS
    }

    private fun mapOptions(): List<GrispiTicketFieldOption>? {
        if (customFieldOptions == null || customFieldOptions.isEmpty()) {
            return null
        }

        val gOptions: MutableList<GrispiTicketFieldOption>? = mutableListOf()
        for ((index, value) in customFieldOptions.withIndex()) {
            gOptions?.add(index, GrispiTicketFieldOption(index, value.name.toString(), value.value.toString()))
        }

        return gOptions
    }

    private fun mapType(): FieldType {
        return when (type) {
            "integer" -> FieldType.NUMBER_INTEGER
            "decimal" -> FieldType.NUMBER_DECIMAL
            "tagger" -> FieldType.MULTI_SELECT
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