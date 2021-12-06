package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.TicketChannel
import jodd.json.meta.JSON

class ZendeskTicketChannel {

    @JSON(name = "channel")
    val channel: String = "web"

    // TODO: 30.11.2021 define all channel types
    fun toGrispiChannel(): TicketChannel {
        if (channel.startsWith("api")) {
            return TicketChannel.CALL
        }
        else if (channel.startsWith("email")) {
            return TicketChannel.EMAIL
        }
        else if (channel.startsWith("web")) {
            return TicketChannel.WEB
        }
        else {
            return TicketChannel.OTHER
        }
    }

}
