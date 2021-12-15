package com.grispi.grispiimport

import com.grispi.grispiimport.grispi.*
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.group.ZendeskGroupRepository
import com.grispi.grispiimport.zendesk.organization.ZendeskOrganization
import com.grispi.grispiimport.zendesk.organization.ZendeskOrganizationRepository
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketCommentRepository
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketRepository
import com.grispi.grispiimport.zendesk.ticketfield.ZendeskTicketFieldRepository
import com.grispi.grispiimport.zendesk.user.ZendeskUserRepository
import com.grispi.grispiimport.zendesk.userfield.ZendeskUserFieldRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.*


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
  @Autowired val zendeskTicketCommentRepository: ZendeskTicketCommentRepository
) {

  companion object {
    val apiCredentials =
      ZendeskApiCredentials("dugunbuketi", "bilgehan@dugunbuketi.com", "BApUrJvH7zpa8obvOFRE3LCuRUuVbMRrSHTRrnop")
  }

  @GetMapping("/getir")
  fun getir(): List<Page<out ZendeskEntity>> {
    val opId = "4a7cdcb3-9da0-4473-93cd-cab1049b2435"
    val organizations = zendeskOrganizationRepository.findAllByOperationId(opId, Pageable.unpaged())
    val groups = zendeskGroupRepository.findAllByOperationId(opId, Pageable.unpaged())
    val tf = zendeskTicketFieldRepository.findAllByOperationId(opId, Pageable.unpaged())
    val uf = zendeskUserFieldRepository.findAllByOperationId(opId, Pageable.unpaged())
    val zu = zendeskUserRepository.findAllByOperationId(opId, Pageable.unpaged())
    val zt = zendeskTicketRepository.findAllByOperationId(opId, Pageable.unpaged())
//    val ztc = zendeskTicketCommentRepository.findAllByOperationId(opId)

    return listOf(organizations, groups, tf, uf, zu, zt)
  }


}