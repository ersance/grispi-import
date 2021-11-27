package com.grispi.grispiimport.zendesk

import jodd.json.meta.JSON

class CustomFieldOption {

    @JSON(name = "id")
    val id: Long? = null

    @JSON(name = "name")
    val name: String? = null

    @JSON(name = "raw_name")
    val rawName: String? = null

    @JSON(name = "value")
    val value: String? = null

    @JSON(name = "default")
    val isDefault: Boolean? = null

}
