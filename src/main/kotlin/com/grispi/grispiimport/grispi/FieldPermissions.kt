package com.grispi.grispiimport.grispi

enum class FieldPermissions {
    AGENT_ONLY, EDITABLE_BY_END_USERS, READONLY_FOR_END_USERS, READONLY;

    fun isEndUserVisible(): Boolean {
        return this == EDITABLE_BY_END_USERS || this == READONLY_FOR_END_USERS
    }

}
