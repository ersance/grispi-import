package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

@RestController
class ZendeskImportController(
    @Autowired val zendeskImportService: ZendeskImportService,
    @Autowired val zendeskImportRepository: ZendeskImportRepository,
    @Autowired val zendeskTicketRepository: ZendeskTicketRepository,
) {

    @PostMapping("/import")
    fun importZendeskResources(@RequestBody zendeskImportRequest: ZendeskImportRequest): ZendeskImportResponse {

        val zendeskTenantImport = zendeskImportService.import(zendeskImportRequest)

        val fetchResourceCounts = zendeskImportService.fetchResourceCounts(zendeskImportRequest.zendeskApiCredentials)

        return ZendeskImportResponse(zendeskTenantImport, fetchResourceCounts)
    }

    @GetMapping("/commented_tickets")
    fun commentedTickets(pageable: Pageable): Page<ZendeskTicket> {
        return zendeskTicketRepository.findCommentedTickets(pageable)
    }

}

data class ZendeskImportResponse(val zendeskTenantImport: ZendeskTenantImport, val resourceCounts: Map<String, Int>)