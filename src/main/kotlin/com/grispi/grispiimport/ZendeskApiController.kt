package com.grispi.grispiimport

import ch.qos.logback.classic.ClassicConstants.FINALIZE_SESSION_MARKER
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
import com.grispi.grispiimport.zendesk.user.ZendeskUserService
import com.grispi.grispiimport.zendesk.userfield.ZendeskUserFieldRepository
import jodd.http.HttpRequest
import jodd.json.JsonParser
import org.slf4j.LoggerFactory
import org.slf4j.MDC
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
  @Autowired val grispiUserService: GrispiUserImportService,
  @Autowired val zendeskUserRepository: ZendeskUserRepository,
  @Autowired val zendeskTicketRepository: ZendeskTicketRepository,
  @Autowired val zendeskTicketCommentRepository: ZendeskTicketCommentRepository,
  @Autowired val zendeskTicketFormRepository: ZendeskTicketFormRepository,
  @Autowired val zendeskMappingQueryRepository: ZendeskMappingQueryRepository,
  @Autowired val grispiTicketCommentImportService: GrispiTicketCommentImportService
) {

  private val logger = LoggerFactory.getLogger(javaClass)

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

  @GetMapping("/logla")
  fun loglaKocum() {
    val tenant = "tenant"

    logger.info("daha bi sey yok. bunlarin app.log'ta olmasi lazim")

    MDC.put("tenantId", tenant)
    logger.info("import biiznillah...")
    Thread.sleep(2000)
    logger.info("devamke..")
    Thread.sleep(1000)
    logger.info("yeter.")
    logger.info(FINALIZE_SESSION_MARKER, "semiallaaaahulimelhamide. tenant");
  }

  @GetMapping("/ticket-forms")
  fun ticketForms(): TicketRequest {

    val findGrispiGroupMemberships = zendeskMappingQueryRepository.findGrispiGroupMemberships(362415139339)

    val toTicketRequest = zendeskTicketRepository.findById(116390).get()
      .toTicketRequest(
        zendeskMappingQueryRepository::findGrispiUserId,
        zendeskMappingQueryRepository::findGrispiGroupId,
        zendeskMappingQueryRepository::findGrispiTicketFormId)
    return toTicketRequest
  }

  @GetMapping("/ticket-status")
  fun ticketStatus(): MutableList<TicketUpdateObj> {
    val operationId = "66a40cc1-3602-4f58-b115-943f1f5754d7"
    val ticketCount = zendeskTicketRepository.countAllByOperationIdAndBrandId(operationId, GrispiTicketImportService.YUVANIKUR_BRAND_ID)

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

}