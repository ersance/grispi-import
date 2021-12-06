package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.GrispiOrganizationRequest
import jodd.json.meta.JSON

class ZendeskOrganization {

    @JSON(name = "id")
    val id: Long = -1

    @JSON(name = "name")
    val name: String? = null

    @JSON(name = "details")
    val details: String? = null

    @JSON(name = "notes")
    val notes: String? = null

    @JSON(name = "tags")
    val tags: MutableSet<String> = mutableSetOf()

    fun toGrispiOrganizationRequest(): GrispiOrganizationRequest {
        return GrispiOrganizationRequest(name.toString(), details, notes, tags)
    }

}
