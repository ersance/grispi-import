package com.grispi.grispiimport.zendesk.ticketfield

import jodd.json.meta.JSON

class CustomFieldOption {

    @JSON(name = "id")
    var id: Long? = null

    @JSON(name = "name")
    var name: String? = null

    @JSON(name = "raw_name")
    var rawName: String? = null

    @JSON(name = "value")
    var value: String? = null

    @JSON(name = "default")
    var isDefault: Boolean? = null

}
