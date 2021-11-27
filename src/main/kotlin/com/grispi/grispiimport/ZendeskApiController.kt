package com.grispi.grispiimport

import com.grispi.grispiimport.grispi.*
import com.grispi.grispiimport.zendesk.*
import jodd.json.JsonObject
import jodd.json.JsonParser
import jodd.json.JsonSerializer
import jodd.json.ValueConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

@RestController
class ZendeskApiController(@Autowired val zendeskApi: ZendeskApi, @Autowired val grispiApi: GrispiApi) {

  companion object {
    val apiCredentials =
      ZendeskApiCredentials("dedeler7787", "ersan@grispi.com", "YEUHw0VWd4B3upwJEoWtssZlDYeDrR9DE7MmOmiQ")
  }

  @GetMapping("/ticketFields")
  fun ticketFields(): MutableList<GrispiTicketField>? {
    val ticketFields = zendeskApi.getTicketFields(apiCredentials)

    val zendeskTicketFields = JsonParser().parse(ticketFields.bodyRaw(), ZendeskTicketFields::class.java)

    val toGrispiTicketField = zendeskTicketFields.ticketFields?.stream()?.map { ticketField -> ticketField.toGrispiTicketField() }?.collect(Collectors.toList())

    return toGrispiTicketField
  }

  @GetMapping("/users")
  fun users(): MutableList<UserRequest>? {
    val users = zendeskApi.getUsers(apiCredentials)

    val zendeskUsers = JsonParser().parse(users.bodyRaw(), ZendeskUsers::class.java)

    val userRequests = zendeskUsers.users?.stream()?.map { user -> user.toGrispiUserRequest() }?.collect(Collectors.toList())

    return userRequests
  }

  @GetMapping("/tickets")
  fun tickets(): MutableList<TicketRequest>? {
    val tickets = zendeskApi.getTickets(apiCredentials)

    val zendeskTickets = JsonParser().parse(tickets.bodyRaw(), ZendeskTickets::class.java)

    val ticketRequests =
      zendeskTickets.tickets?.stream()?.map { ticket -> ticket.toTicketRequest() }?.collect(Collectors.toList())

    return ticketRequests
  }

}