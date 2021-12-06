package com.grispi.grispiimport.zendesk

import jodd.json.meta.JSON

class ZendeskTicketFields {

    @JSON(name = "ticket_fields")
    val ticketFields: List<ZendeskTicketField> = emptyList()

}