package com.grispi.grispiimport

import com.grispi.grispiimport.grispi.*
import com.grispi.grispiimport.zendesk.*
import jodd.json.JsonException
import jodd.json.JsonParser
import jodd.json.JsonSerializer
import jodd.json.ValueConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.util.stream.Collectors


@RestController
class ZendeskApiController(
  @Autowired val zendeskApi: ZendeskApi,
  @Autowired val grispiApi: GrispiApi,
  @Autowired val zendeskMappingDao: ZendeskMappingDao,
) {

  companion object {
    val apiCredentials =
      ZendeskApiCredentials("dedeler7787", "ersan@grispi.com", "YEUHw0VWd4B3upwJEoWtssZlDYeDrR9DE7MmOmiQ")
  }

  @GetMapping("/ticketFields")
  fun ticketFields(): MutableList<GrispiTicketFieldRequest>? {
    val ticketFields = zendeskApi.getTicketFields(apiCredentials)

    val zendeskTicketFields = JsonParser().parse(ticketFields.bodyRaw(), ZendeskTicketFields::class.java)

    val toGrispiTicketField = zendeskTicketFields.ticketFields.stream().filter { ticketField -> !ZendeskTicketField.SYSTEM_FIELDS.contains(ticketField.type) }
      ?.map { ticketField -> ticketField.toGrispiTicketFieldRequest() }?.collect(Collectors.toList())

    return toGrispiTicketField
  }

  @GetMapping("/organizations")
  fun organizations(): List<ZendeskOrganization> {
    val organizations = zendeskApi.getOrganizations(apiCredentials)

    val zendeskOrganizations = JsonParser().parse(organizations.bodyRaw(), ZendeskOrganizations::class.java)

    return zendeskOrganizations.organizations
  }

  @GetMapping("/groups")
  fun groups(): List<ZendeskGroup> {
    val organizations = zendeskApi.getGroups(apiCredentials)

    val zendeskOrganizations = JsonParser().parse(organizations.bodyRaw(), ZendeskGroups::class.java)

    return zendeskOrganizations.groups
  }

  @GetMapping("/users")
  fun users(): ZendeskUsers? {
    val users = zendeskApi.getUsers(apiCredentials)

    val zendeskUsers = JsonParser().parse(users.bodyRaw(), ZendeskUsers::class.java)

    val userRequests = zendeskUsers.users?.stream()?.map { user -> user.toGrispiUserRequest() }?.collect(Collectors.toList())

    return zendeskUsers
  }

  @GetMapping("/tickets")
  fun tickets(): ZendeskTickets? {
    val tickets = zendeskApi.getTickets(apiCredentials)

    val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
    val valueConverter: ValueConverter<String, Instant?> = ValueConverter { source -> dateFormat.parse(source).toInstant() }

    val zendeskTickets = JsonParser().withValueConverter("tickets.values.createdAt", valueConverter).parse(tickets.bodyRaw(), ZendeskTickets::class.java)

//    val ticketRequests = zendeskTickets.tickets?.stream()?.map {
//        ticket -> ticket.toTicketRequest(zendeskMappingDao::getUserId, zendeskMappingDao::getGroupId)
//    }?.collect(Collectors.toList())

    return zendeskTickets
  }

  @GetMapping("/date")
  fun dateTest(): Instant? {
//    val comment_ = TicketRequest.Comment_("test",
//      true,
//      TicketRequest.User_("213", "testcomment@example.com", null),
//      Instant.now())
    val date = "2021-11-30T13:22:17Z"

    val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
    val valueConverter: ValueConverter<String, Instant?> = ValueConverter { source -> dateFormat.parse(source).toInstant() }

    val instant = valueConverter.convert(date)

    return instant
  }

}