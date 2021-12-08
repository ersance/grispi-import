package com.grispi.grispiimport.zendesk

import jodd.json.meta.JSON

class ZendeskTickets {

    @JSON(name = "tickets")
    val tickets: List<ZendeskTicket> = emptyList()

    @JSON(name = "count")
    val count: Int = 0

}