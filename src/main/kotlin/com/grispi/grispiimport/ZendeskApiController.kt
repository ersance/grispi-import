package com.grispi.grispiimport

import com.grispi.grispiimport.grispi.*
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.group.ZendeskGroupRepository
import com.grispi.grispiimport.zendesk.organization.ZendeskOrganization
import com.grispi.grispiimport.zendesk.organization.ZendeskOrganizationRepository
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketCommentRepository
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketRepository
import com.grispi.grispiimport.zendesk.ticketfield.ZendeskTicketFieldRepository
import com.grispi.grispiimport.zendesk.ticketform.ZendeskTicketForm
import com.grispi.grispiimport.zendesk.ticketform.ZendeskTicketFormRepository
import com.grispi.grispiimport.zendesk.ticketform.ZendeskTicketFormService
import com.grispi.grispiimport.zendesk.user.ZendeskUserRepository
import com.grispi.grispiimport.zendesk.userfield.ZendeskUserFieldRepository
import jodd.http.HttpRequest
import jodd.json.JsonParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@Profile("dev")
@RestController
class ZendeskApiController(
  @Autowired val zendeskApi: ZendeskApi,
  @Autowired val grispiApi: GrispiApi,
  @Autowired val zendeskOrganizationRepository: ZendeskOrganizationRepository,
  @Autowired val zendeskGroupRepository: ZendeskGroupRepository,
  @Autowired val zendeskTicketFieldRepository: ZendeskTicketFieldRepository,
  @Autowired val zendeskUserFieldRepository: ZendeskUserFieldRepository,
  @Autowired val zendeskUserRepository: ZendeskUserRepository,
  @Autowired val zendeskTicketRepository: ZendeskTicketRepository,
  @Autowired val zendeskTicketCommentRepository: ZendeskTicketCommentRepository,
  @Autowired val zendeskTicketFormRepository: ZendeskTicketFormRepository,
  @Autowired val zendeskMappingQueryRepository: ZendeskMappingQueryRepository,
  @Autowired val testApiLimit: TestApiLimit
) {

  companion object {
    val apiCredentials =
      ZendeskApiCredentials("dugunbuketi", "bilgehan@dugunbuketi.com", "BApUrJvH7zpa8obvOFRE3LCuRUuVbMRrSHTRrnop")
  }

  @GetMapping("/getir")
  fun getir(): Page<ZendeskComment> {
    val opId = "4a7cdcb3-9da0-4473-93cd-cab1049b2435"
//    val organizations = zendeskOrganizationRepository.findAllByOperationId(opId, Pageable.unpaged())
//    val groups = zendeskGroupRepository.findAllByOperationId(opId, Pageable.unpaged())
//    val tf = zendeskTicketFieldRepository.findAllByOperationId(opId, Pageable.unpaged())
//    val uf = zendeskUserFieldRepository.findAllByOperationId(opId, Pageable.unpaged())
//    val zu = zendeskUserRepository.findAllByOperationId(opId, Pageable.unpaged())
//    val zt = zendeskTicketRepository.findAllByOperationId(opId, Pageable.unpaged())
//    val ztc = zendeskTicketCommentRepository.findAllByOperationId(opId)

//    return listOf(organizations, groups, tf, uf, zu, zt)
    return zendeskTicketCommentRepository.findAll(PageRequest.of(0, 100).withSort(Sort.by("createdAt").descending()))
  }

  @GetMapping("/mock")
  fun testMockServer() {
    testApiLimit.test()
  }

  @GetMapping("/deleted-users")
  fun deletedUsers() {
    val deletedUserCount = zendeskUserRepository.countAllByOperationIdAndActiveFalse("66a40cc1-3602-4f58-b115-943f1f5754d7")

    println("deleted user import process is started for ${deletedUserCount} users at: ${LocalDateTime.now()}")

    val to = BigDecimal(deletedUserCount).divide(BigDecimal(GrispiUserImportService.PAGE_SIZE), RoundingMode.UP).toInt()
    for (index in 0 until to) {
      var users = zendeskUserRepository.findAllByOperationIdAndActiveFalse("66a40cc1-3602-4f58-b115-943f1f5754d7", PageRequest.of(index, GrispiUserImportService.PAGE_SIZE))
      println("${users.content.size} users for page index: $index")
    }
  }

  @GetMapping("/ticket-forms")
  fun ticketForms(): MutableList<ZendeskTicketForm>? {
    return zendeskTicketFormRepository.findAll()
  }

  @GetMapping("/ticket-status")
  fun ticketStatus(): MutableList<TicketUpdateObj> {
    val operationId = "66a40cc1-3602-4f58-b115-943f1f5754d7"
    val ticketCount = zendeskTicketRepository.countAllByOperationIdAndBrandId(operationId, GrispiTicketImportService.YUVANIKUR_BRAND_ID)

    println("user import process is started for ${ticketCount} users at: ${LocalDateTime.now()}")

    val ticketUpdateObjects: MutableList<TicketUpdateObj> = mutableListOf()
    val to = BigDecimal(ticketCount).divide(BigDecimal(GrispiUserImportService.PAGE_SIZE), RoundingMode.UP).toInt()
    for (index in 0 until to) {
      val tickets = zendeskTicketRepository.findAllByOperationIdAndBrandId(operationId,
        GrispiTicketImportService.YUVANIKUR_BRAND_ID,
        PageRequest.of(index, GrispiTicketImportService.PAGE_SIZE))

      tickets.map { TicketUpdateObj(zendeskMappingQueryRepository.findGrispiTicketKey(it.id), it.status.toString()) }.toCollection(ticketUpdateObjects)
    }

    return ticketUpdateObjects
  }

  data class TicketUpdateObj(val ticketKey: String, val status: String)

//  @GetMapping("/ticket-comments")
//  fun ticketComments(): List<Pair<Long?, List<CommentRequest>>> {
//    val ticketCount = zendeskTicketRepository.countAllByOperationIdAndBrandId(operationId, YUVANIKUR_BRAND_ID)
//
//    println("user import process is started for ${ticketCount} users at: ${LocalDateTime.now()}")
//
//    val to = BigDecimal(ticketCount).divide(BigDecimal(GrispiUserImportService.PAGE_SIZE), RoundingMode.UP).toInt()
//    for (index in 0 until to) {
//      val tickets = zendeskTicketRepository.findAllByOperationIdAndBrandId(operationId, YUVANIKUR_BRAND_ID, PageRequest.of(index, PAGE_SIZE))
//  }

}