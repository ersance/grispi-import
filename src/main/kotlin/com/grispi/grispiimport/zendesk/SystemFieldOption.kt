package com.grispi.grispiimport.zendesk

import jodd.json.meta.JSON

class SystemFieldOption {
    @JSON(name = "name")
    val name: String? = null

    @JSON(name = "value")
    val value: String? = null
}
