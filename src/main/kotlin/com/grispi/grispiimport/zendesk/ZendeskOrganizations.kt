package com.grispi.grispiimport.zendesk

import jodd.json.meta.JSON

class ZendeskOrganizations {

    @JSON(name = "organizations")
    val organizations: List<ZendeskOrganization> = emptyList()

}
