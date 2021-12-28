package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class ZendeskImportController(
    @Autowired val zendeskImportService: ZendeskImportService,
    @Autowired val zendeskTicketRepository: ZendeskTicketRepository,
) {

    @PostMapping("/import")
    fun importZendeskResources(@RequestBody zendeskImportRequest: ZendeskImportRequest): ZendeskImportResponse {

        val zendeskTenantImport = zendeskImportService.import(zendeskImportRequest)

        return ZendeskImportResponse(zendeskTenantImport)
    }

    @PostMapping("/fetch")
    fun fetchById(@RequestBody zendeskImportRequest: ZendeskImportRequest, @RequestParam("operationId") operationId: String?): ResponseEntity<Void> {
        try {
            zendeskImportService.fetch(operationId, zendeskImportRequest)
        }
        catch (exception: RuntimeException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
        }

        return ResponseEntity.ok().build()
    }

    @PostMapping("/import/{operationId}")
    fun importById(@RequestBody zendeskImportRequest: ZendeskImportRequest, @PathVariable operationId: String): ResponseEntity<Void> {

        try {
            zendeskImportService.import(operationId, zendeskImportRequest)
        }
        catch (exception: RuntimeException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
        }

        return ResponseEntity.ok().build()
    }

}

data class ZendeskImportResponse(val zendeskTenantImport: ZendeskTenantImport)