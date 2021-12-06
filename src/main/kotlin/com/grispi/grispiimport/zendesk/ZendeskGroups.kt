package com.grispi.grispiimport.zendesk

import jodd.json.meta.JSON

class ZendeskGroups {

    @JSON(name = "groups")
    val groups: List<ZendeskGroup> = emptyList()

}
