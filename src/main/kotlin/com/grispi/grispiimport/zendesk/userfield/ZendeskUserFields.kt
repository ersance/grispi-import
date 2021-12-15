package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.FieldType
import com.grispi.grispiimport.grispi.GrispiTicketFieldOption
import com.grispi.grispiimport.grispi.GrispiUserFieldRequest
import com.grispi.grispiimport.zendesk.ticketfield.CustomFieldOption
import jodd.json.meta.JSON
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

class ZendeskUserFields {

    @JSON(name = "user_fields")
    val userFields: List<ZendeskUserField> = emptyList()

}

@Document
class ZendeskUserField: ZendeskEntity() {

    @Id
    @JSON(name = "id")
    var id: Long = -1

    @JSON(name = "url")
    var url: String? = null

    @JSON(name = "type")
    var type: String? = null

    @JSON(name = "key")
    var key: String = ""

    @JSON(name = "title")
    var title: String = ""

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

    @JSON(name = "regexp_for_validation")
    var regexpForValidation: String? = null

    @JSON(name = "tag")
    var tag: String? = null

    @JSON(name = "custom_field_options")
    var customFieldOptions: List<CustomFieldOption>? = null

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

        if (customFieldOptions == null || customFieldOptions!!.isEmpty()) {
            return emptyList()
        }

        val gOptions: MutableList<GrispiTicketFieldOption> = mutableListOf()
        for ((index, value) in customFieldOptions!!.withIndex()) {
            gOptions.add(index, GrispiTicketFieldOption(index, value.name.toString(), value.value.toString()))
        }

        return gOptions
    }

}
