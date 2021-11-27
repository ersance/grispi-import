package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.TicketChannel
import jodd.json.meta.JSON

class ZendeskTicketChannel {

    @JSON(name = "channel")
    val channel: String = "web"

    // TODO
    fun toGrispiChannel(): TicketChannel {
        return when (channel) {
            "email" -> TicketChannel.EMAIL
            "call" -> TicketChannel.CALL
            else -> TicketChannel.WEB
        }
    }

}
