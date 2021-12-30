package com.grispi.grispiimport.grispi

class GrispiUserFieldRequest(
    val key: String?,
    val name: String?,
    val type: FieldType?,
    val description: String?,
    val descriptionForAgents: String?,
    val titleForAgents: String?,
    val order: Int?,
    val attributes: List<String>? = null,
    val options: List<GrispiTicketFieldOption>?
): GrispiApiRequest() {

    class Builder {
        private var key: String? = null
        private var name: String? = null
        private var type: FieldType? = null
        private var description: String? = null
        private var descriptionForAgents: String? = null
        private var titleForAgents: String? = null
        private var order: Int? = null
        private var attributes: List<String>? = null
        private var options: List<GrispiTicketFieldOption>? = null

        fun key(key: String) = apply { this.key = key }
        fun name(name: String) = apply { this.name = name }
        fun type(type: FieldType) = apply { this.type = type }
        fun description(description: String?) = apply { this.description = description }
        fun descriptionForAgents(descriptionForAgents: String?) = apply { this.descriptionForAgents = descriptionForAgents }
        fun titleForAgents(titleForAgents: String) = apply { this.titleForAgents = titleForAgents }
        fun order(order: Int?) = apply { this.order = order }
        fun attributes(attributes: List<String>?) = apply { this.attributes = attributes }
        fun options(options: List<GrispiTicketFieldOption>?) = apply { this.options = options }

        fun build(): GrispiUserFieldRequest {
            return GrispiUserFieldRequest(key, name, type, description, descriptionForAgents, titleForAgents, order, attributes, options)
        }

        fun buildPhoneNumberField(): GrispiUserFieldRequest {
            return GrispiUserFieldRequest(
                ZENDESK_PHONE_USER_FIELD_KEY,
                "zendeskPhone",
                FieldType.TEXT,
                "zendesk phone number",
                "zendesk phone number",
                "zendesk phone number",
                null, emptyList(), null)
        }

        fun buildExternalId(): GrispiUserFieldRequest {
            return GrispiUserFieldRequest(
                ZENDESK_EXTERNAL_ID_USER_FIELD_KEY,
                "zendeskExternalId",
                FieldType.TEXT,
                "zendesk external id",
                "zendesk external id",
                "zendesk external id",
                null, emptyList(), null)
        }

        companion object {
            const val ZENDESK_PHONE_USER_FIELD_KEY = "uizg.phone"
            const val ZENDESK_EXTERNAL_ID_USER_FIELD_KEY = "uizg.external_id"
        }

    }

}
