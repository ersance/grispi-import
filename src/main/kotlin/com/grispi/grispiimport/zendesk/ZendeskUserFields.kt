package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.FieldType
import com.grispi.grispiimport.grispi.GrispiTicketFieldOption
import com.grispi.grispiimport.grispi.GrispiUserFieldRequest
import jodd.json.meta.JSON

class ZendeskUserFields {

    @JSON(name = "user_fields")
    val userFields: List<ZendeskUserField> = emptyList()

}

class ZendeskUserField {

    @JSON(name = "id")
    val id: Long = -1

    @JSON(name = "url")
    val url: String? = null

    @JSON(name = "type")
    val type: String? = null

    @JSON(name = "key")
    val key: String = ""

    @JSON(name = "title")
    val title: String = ""

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

    @JSON(name = "regexp_for_validation")
    val regexpForValidation: String? = null

    @JSON(name = "tag")
    val tag: String? = null

    @JSON(name = "custom_field_options")
    val customFieldOptions: List<CustomFieldOption>? = null

    fun toGrispiUserField(): GrispiUserFieldRequest {
        return GrispiUserFieldRequest.Builder()
            .key("uiz.${key}")
            .name("${title} ${key}")
            .type(mapType())
            .description(description)
            .descriptionForAgents(description)
            .titleForAgents(title)
            .order(position)
            .attributes(emptyList())
            .options(mapOptions())
            .build()
    }

    private fun mapType(): FieldType {
        return when (type) {
            "integer" -> FieldType.NUMBER_INTEGER
            "decimal" -> FieldType.NUMBER_DECIMAL
            "tagger" -> FieldType.SELECT
            "dropdown" -> FieldType.SELECT
            "multiselect" -> FieldType.MULTI_SELECT
            "date" -> FieldType.DATE
            else -> FieldType.TEXT
        }
    }

    private fun mapOptions(): List<GrispiTicketFieldOption>? {

        val type = mapType()
        if (!FieldType.TYPES_THAT_ALLOW_OPTIONS.contains(type)) {
            return null;
        }

        if (customFieldOptions == null || customFieldOptions.isEmpty()) {
            return emptyList()
        }

        val gOptions: MutableList<GrispiTicketFieldOption> = mutableListOf()
        for ((index, value) in customFieldOptions.withIndex()) {
            gOptions.add(index, GrispiTicketFieldOption(index, value.name.toString(), value.value.toString()))
        }

        return gOptions
    }

}
