package com.grispi.grispiimport.zendesk

import jodd.json.meta.JSON

class ZendeskCustomField {

    @JSON(name = "id")
    val id: Long? = null

    // String or List<String>
    @JSON(name = "value")
    val value: Any? = null

    fun toGrispiKey(): String {
        return "tiz.${id}"
    }

}
