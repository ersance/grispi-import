package com.grispi.grispiimport.zendesk

import jodd.json.meta.JSON

class ZendeskUsers {

    @JSON(name = "users")
    val users: List<ZendeskUser> = emptyList()

}