package com.grispi.grispiimport.grispi

class TicketStatus(val id: Long, val name: String) {

    companion object {
        val NEW: TicketStatus = TicketStatus(1, "New")
        val OPEN: TicketStatus = TicketStatus(1, "Open")
        val PENDING: TicketStatus = TicketStatus(1, "Pending")
        val ON_HOLD: TicketStatus = TicketStatus(1, "On Hold")
        val SOLVED: TicketStatus = TicketStatus(1, "Solved")
        val CLOSED: TicketStatus = TicketStatus(1, "Closed")
    }

}