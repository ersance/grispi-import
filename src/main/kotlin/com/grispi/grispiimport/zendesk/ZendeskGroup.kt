package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.GrispiGroupRequest
import jodd.json.meta.JSON

class ZendeskGroup {

    @JSON(name = "id")
    val id: Long = -1

    @JSON(name = "name")
    val name: String? = null

    @JSON(name = "description")
    val description: String? = null

    @JSON(name = "default")
    val default: Boolean = false

    fun toGrispiGroupRequest(): GrispiGroupRequest {
        return GrispiGroupRequest(name.toString())
    }


}
