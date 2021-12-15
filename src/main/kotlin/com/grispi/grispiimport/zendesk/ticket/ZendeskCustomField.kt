package com.grispi.grispiimport.zendesk.ticket

import jodd.json.meta.JSON

class ZendeskCustomField {

    @JSON(name = "id")
    var id: Long? = null

    @JSON(name = "value")
    var value: Any? = null

    fun toGrispiKey(): String {
        return "tiz.${id}"
    }

}