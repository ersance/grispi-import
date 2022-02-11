package com.grispi.grispiimport.zendesk.organization

import com.grispi.grispiimport.grispi.GrispiOrganizationRequest
import com.grispi.grispiimport.zendesk.ZendeskEntity
import jodd.json.meta.JSON
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
class ZendeskOrganization: ZendeskEntity() {

    @Id
    @JSON(name = "id")
    var id: Long = -1

    @JSON(name = "name")
    var name: String? = null

    @JSON(name = "details")
    var details: String? = null

    @JSON(name = "notes")
    var notes: String? = null

    @JSON(name = "tags")
    var tags: MutableSet<String> = mutableSetOf()

    fun toGrispiOrganizationRequest(): GrispiOrganizationRequest {
        return GrispiOrganizationRequest(name.toString(), details, notes, tags)
    }

}

class ZendeskOrganizations {

    @JSON(name = "organizations")
    val organizations: List<ZendeskOrganization> = emptyList()

}
