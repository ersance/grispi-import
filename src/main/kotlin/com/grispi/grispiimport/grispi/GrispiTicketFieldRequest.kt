package com.grispi.grispiimport.grispi

class GrispiTicketFieldRequest(
    val key: String?,
    val name: String?,
    val type: FieldType?,
    val descriptionForAgents: String?,
    val descriptionForEndUsers: String?,
    val titleForAgents: String?,
    val titleForEndUsers: String?,
    val required: RequiredStatus?,
    val permission: FieldPermissions?,
    val attributes: List<String>?,
    val options: List<GrispiTicketFieldOption>?
): GrispiApiRequest() {

    class Builder {
        private var key: String? = null
        private var name: String? = null
        private var type: FieldType? = null
        private var descriptionForAgents: String? = null
        private var descriptionForEndUsers: String? = null
        private var titleForAgents: String? = null
        private var titleForEndUsers: String? = null
        private var required: RequiredStatus? = null
        private var permission: FieldPermissions? = null
        private var attributes: List<String>? = null
        private var options: List<GrispiTicketFieldOption>? = null

        fun key(key: String) = apply { this.key = key }
        fun name(name: String) = apply { this.name = name }
        fun type(type: FieldType) = apply { this.type = type }
        fun descriptionForAgents(descriptionForAgents: String) = apply { this.descriptionForAgents = descriptionForAgents }
        fun descriptionForEndUsers(descriptionForEndUsers: String?) = apply { this.descriptionForEndUsers = descriptionForEndUsers }
        fun titleForAgents(titleForAgents: String) = apply { this.titleForAgents = titleForAgents }
        fun titleForEndUsers(titleForEndUsers: String?) = apply { this.titleForEndUsers = titleForEndUsers }
        fun required(required: RequiredStatus) = apply { this.required = required }
        fun permission(permission: FieldPermissions) = apply { this.permission = permission }
        fun attributes(attributes: List<String>?) = apply { this.attributes = attributes }
        fun options(options: List<GrispiTicketFieldOption>?) = apply { this.options = options }

        fun build(): GrispiTicketFieldRequest {
            return GrispiTicketFieldRequest(key, name, type, descriptionForAgents, descriptionForEndUsers, titleForAgents, titleForEndUsers, required, permission, attributes, options)
        }
    }

}