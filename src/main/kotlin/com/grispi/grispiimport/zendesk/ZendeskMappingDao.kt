package com.grispi.grispiimport.zendesk

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import org.springframework.web.context.WebApplicationContext.SCOPE_REQUEST

@Service
class ZendeskMappingDao {

    private val userIdMap: MutableMap<Long, Long> = mutableMapOf() // FIXME: syncronized
    private val customFieldIdMap: MutableMap<Long, Long> = mutableMapOf()
    private val organizationIdMap: MutableMap<Long, Long> = mutableMapOf()
    private val groupIdMap: MutableMap<Long, Long> = mutableMapOf()

    fun addCustomFieldMapping(zendeskId: Long, grispiId: Long) {
        customFieldIdMap.put(zendeskId, grispiId)
    }

    fun getCustomFieldId(zendeskId: Long): Long? {
        return customFieldIdMap.get(zendeskId) ?: throw GrispiReferenceNotFoundException(zendeskId, "custom field")
    }

    fun addOrganizationMapping(zendeskId: Long, grispiId: Long) {
        organizationIdMap.put(zendeskId, grispiId)
    }

    fun getOrganizationId(zendeskId: Long): Long? {
        return organizationIdMap.get(zendeskId) ?: throw GrispiReferenceNotFoundException(zendeskId, "organization")
    }

    fun addUserMapping(zendeskId: Long, grispiId: Long) {
        userIdMap.put(zendeskId, grispiId)
    }

    fun getUserId(zendeskId: Long): Long? {
        return userIdMap.get(zendeskId) ?: throw GrispiReferenceNotFoundException(zendeskId, "user")
    }

    fun addGroupMapping(zendeskId: Long, grispiId: Long) {
        groupIdMap.put(zendeskId, grispiId)
    }

    fun getGroupId(zendeskId: Long): Long? {
        return groupIdMap.get(zendeskId)
    }

}

class GrispiReferenceNotFoundException(val zendeskId: Long, val resourceName: String) : RuntimeException() {
    fun message(): String {
        return "requested ${resourceName} with zendesk reference id: ${zendeskId} couldn't be found"
    }
}

