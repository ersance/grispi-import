package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.zendesk.organization.ResourceCount
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketRepository
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@RestController
class ZendeskImportController(
    @Autowired val zendeskImportService: ZendeskImportService,
    @Autowired val zendeskImportRepository: ZendeskImportRepository,
    @Autowired val zendeskTicketRepository: ZendeskTicketRepository,
) {

    @PostMapping("/import")
    fun importZendeskResources(@RequestBody zendeskImportRequest: ZendeskImportRequest): ZendeskImportResponse {

        MDC.put("tenantId", zendeskImportRequest.grispiApiCredentials.tenantId)

        val zendeskTenantImport = zendeskImportService.import(zendeskImportRequest)

        return ZendeskImportResponse(zendeskTenantImport)
    }

    @PostMapping("/zendesk-import", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun blog(@RequestBody zendeskImportRequest: ZendeskImportRequest): ZendeskTenantImport {

        MDC.put("tenantId", zendeskImportRequest.grispiApiCredentials.tenantId)

        return zendeskImportService.import(zendeskImportRequest)
    }

    @GetMapping("/import/{operationId}/status")
    fun importStatus(@PathVariable operationId: String, @RequestBody zendeskImportRequest: ZendeskImportRequest): ResponseEntity<ZendeskImportStatusResponse> {
        return ResponseEntity.ok(zendeskImportService.checkStatus(operationId, zendeskImportRequest.zendeskApiCredentials))
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
data class ZendeskImportStatusResponse(val grispiTenantId: String, val zendeskTenantId: String, val createdAt: Long, val resources: List<ResourceCount>)