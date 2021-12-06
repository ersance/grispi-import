package com.grispi.grispiimport.zendesk

import jodd.json.meta.JSON

class ZendeskTickets {

    @JSON(name = "tickets")
    val tickets: List<ZendeskTicket> = emptyList()

}