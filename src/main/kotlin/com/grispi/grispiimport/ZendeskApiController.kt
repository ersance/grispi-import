package com.grispi.grispiimport

import com.grispi.grispiimport.grispi.*
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.group.ZendeskGroupRepository
import com.grispi.grispiimport.zendesk.organization.ZendeskOrganizationRepository
import com.grispi.grispiimport.zendesk.ticket.*
import com.grispi.grispiimport.zendesk.ticketfield.ZendeskTicketFieldRepository
import com.grispi.grispiimport.zendesk.ticketform.ZendeskTicketFormRepository
import com.grispi.grispiimport.zendesk.user.ZendeskUserAggregationRepository
import com.grispi.grispiimport.zendesk.user.ZendeskUserRepository
import com.grispi.grispiimport.zendesk.user.ZendeskUserService
import com.grispi.grispiimport.zendesk.userfield.ZendeskUserFieldRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*

/**
 * this controller is only for debugging things on development!
 */
@Profile("dev")
@RestController
class ZendeskApiController(
  @Autowired val zendeskApi: ZendeskApi,
  @Autowired val grispiApi: GrispiApi,
  @Autowired val zendeskOrganizationRepository: ZendeskOrganizationRepository,
  @Autowired val zendeskGroupRepository: ZendeskGroupRepository,
  @Autowired val zendeskTicketFieldRepository: ZendeskTicketFieldRepository,
  @Autowired val zendeskUserFieldRepository: ZendeskUserFieldRepository,
  @Autowired val zendeskUserService: ZendeskUserService,
  @Autowired val zendeskUserAggregationRepository: ZendeskUserAggregationRepository,
  @Autowired val grispiUserService: GrispiUserImportService,
  @Autowired val zendeskUserRepository: ZendeskUserRepository,
  @Autowired val zendeskTicketRepository: ZendeskTicketRepository,
  @Autowired val zendeskTicketAggregationRepository: ZendeskTicketAggregationRepository,
  @Autowired val zendeskTicketService: ZendeskTicketService,
  @Autowired val zendeskTicketCommentRepository: ZendeskTicketCommentRepository,
  @Autowired val zendeskTicketFormRepository: ZendeskTicketFormRepository,
  @Autowired val zendeskMappingQueryRepository: ZendeskMappingQueryRepository,
  @Autowired val grispiTicketCommentImportService: GrispiTicketCommentImportService,
  @Autowired val zendeskTicketCommentAggregationRepository: ZendeskTicketCommentAggregationRepository,
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  companion object {
    val apiCredentials =
      ZendeskApiCredentials("dugunbuketi", "bilgehan@dugunbuketi.com", "BApUrJvH7zpa8obvOFRE3LCuRUuVbMRrSHTRrnop")
  }

  @GetMapping("/users")
  fun users(@RequestParam("limit") limit: Long?): MutableList<UserRequest>? {
    val opId = "45aa9bfa-de2a-47e0-a2f9-0dea0b14f63d"

    val start = System.currentTimeMillis()
    val findActiveUsers = zendeskUserAggregationRepository.findActiveUsers(opId)
    val end = System.currentTimeMillis()

    logger.info("${end.minus(start)} ms for (${findActiveUsers.count()}) users")

    return findActiveUsers.stream().limit(5).map { it.user.toGrispiUserRequest(it.grispiGroupIds, it.grispiOrganizationId) }.toList()
  }

  @GetMapping("/users_old")
  fun users_old(@RequestParam("limit") limit: Long?): UserRequest {
    val opId = "45aa9bfa-de2a-47e0-a2f9-0dea0b14f63d"

    val start = System.currentTimeMillis()
    val findActiveUsers = zendeskUserRepository.findById(1905241556334).get()
    val end = System.currentTimeMillis()

    logger.info("${end.minus(start)} ms for (${findActiveUsers}) users")

    return findActiveUsers.toGrispiUserRequest(emptySet(), zendeskMappingQueryRepository.findGrispiOrganizationId(findActiveUsers.organizationId!!)?.toLong())
  }

  @GetMapping("/tickets")
  fun getir(@RequestParam("limit") limit: Long?): List<TicketRequest> {
    val opId = "45aa9bfa-de2a-47e0-a2f9-0dea0b14f63d"

    val startTicket = System.currentTimeMillis()
    val findAllTickets = zendeskTicketAggregationRepository.findAllTickets(opId)
    val findById = zendeskTicketRepository.findById(81517).get().toTicketRequest(zendeskMappingQueryRepository::findGrispiUserId, zendeskMappingQueryRepository::findGrispiGroupId, zendeskMappingQueryRepository::findGrispiTicketFormId)

    logger.info("${System.currentTimeMillis().minus(startTicket)} ms for (${findAllTickets.count()}) tickets")

    return listOf(findAllTickets.stream().limit(5).map { it.ticket.toTicketRequest(it.mappings()) }.findFirst().get(),findById)
  }

  @GetMapping("/comments")
  fun groupedComments(@RequestParam("limit") limit: Long?): MutableList<GroupedComments>? {
    val opId = "45aa9bfa-de2a-47e0-a2f9-0dea0b14f63d"

    val startTicketComment = System.currentTimeMillis()
    val findCommentedTickets = zendeskTicketCommentAggregationRepository.findCommentedTickets(opId)
    logger.info("${System.currentTimeMillis().minus(startTicketComment)} ms for (${findCommentedTickets.count()}) ticket comments")

    return findCommentedTickets.stream().limit(5).toList()
  }

  @GetMapping("/allInOne")
  fun tickets(@RequestParam("limit") limit: Long?): String {
    val opId = "45aa9bfa-de2a-47e0-a2f9-0dea0b14f63d"

    val startUser = System.currentTimeMillis()
    val findActiveUsers = zendeskUserAggregationRepository.findActiveUsers(opId)
    logger.info("${System.currentTimeMillis().minus(startUser)} ms for (${findActiveUsers.count()}) tickets")

    val startTicket = System.currentTimeMillis()
    val findAllTickets = zendeskTicketAggregationRepository.findAllTickets(opId)
    logger.info("${System.currentTimeMillis().minus(startTicket)} ms for (${findAllTickets.count()}) tickets")

    val startTicketComment = System.currentTimeMillis()
    val findCommentedTickets = zendeskTicketCommentAggregationRepository.findCommentedTickets(opId)
    logger.info("${System.currentTimeMillis().minus(startTicketComment)} ms for (${findCommentedTickets.count()}) ticket comments")

    return "tamam"
  }

}