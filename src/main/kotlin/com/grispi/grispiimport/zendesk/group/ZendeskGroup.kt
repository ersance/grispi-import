package com.grispi.grispiimport.zendesk.group

import com.grispi.grispiimport.grispi.GrispiGroupRequest
import com.grispi.grispiimport.zendesk.ZendeskEntity
import jodd.json.meta.JSON
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
class ZendeskGroup: ZendeskEntity() {

    @Id
    @JSON(name = "id")
    var id: Long = -1

    @JSON(name = "name")
    var name: String? = null

    @JSON(name = "description")
    var description: String? = null

    @JSON(name = "default")
    var default: Boolean = false

    fun toGrispiGroupRequest(): GrispiGroupRequest {
        return GrispiGroupRequest(name.toString())
    }

}

class ZendeskGroups {

    @JSON(name = "groups")
    val groups: List<ZendeskGroup> = emptyList()

}