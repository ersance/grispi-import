package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.common.ImportLogContainer
import com.grispi.grispiimport.common.ImportLogDao
import com.grispi.grispiimport.grispi.GrispiUserFieldRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class ZendeskImportController(
    @Autowired val organizationImportService: OrganizationImportService,
    @Autowired val groupImportService: GroupImportService,
    @Autowired val ticketFieldImportService: TicketFieldImportService,
    @Autowired val userFieldImportService: UserFieldImportService,
    @Autowired val userImportService: UserImportService,
    @Autowired val ticketImportService: TicketImportService,
    @Autowired val ticketCommentImportService: TicketCommentImportService,
    @Autowired val importLogDao: ImportLogDao,
    @Autowired val zendeskMappingDao: ZendeskMappingDao
) {

    @PostMapping("/import")
    fun importZendeskResources(@RequestBody zendeskImportRequest: ZendeskImportRequest): ZendeskImportResponse {

        val operationId = "op_id"

//        zendeskMappingDao.initializeTenant(operationId, zendeskImportRequest.grispiApiCredentials.tenantId)
//
//        organizationImportService.import(operationId, zendeskImportRequest)
//
//        groupImportService.import(operationId, zendeskImportRequest)
//
//        ticketFieldImportService.import(operationId, zendeskImportRequest)
//
//        userFieldImportService.import(operationId, zendeskImportRequest)
//
//        userImportService.import(operationId, zendeskImportRequest)

//        ticketImportService.import(operationId, zendeskImportRequest)

//        ticketCommentImportService.import(operationId, zendeskImportRequest)

        return ZendeskImportResponse(operationId, zendeskMappingDao.getAllLogs(operationId))
    }

    @GetMapping("/{tenantId}/imports")
    fun tenantImports(@PathVariable tenantId: String): Map<String, Any> {
        return zendeskMappingDao.findByTenantId(tenantId)
    }

    @GetMapping("/{tenantId}/import-logs")
    fun importLogs(@PathVariable tenantId: String): ImportLogContainer {
        return zendeskMappingDao.getAllLogsByTenant(tenantId)
    }

    @GetMapping("/import-logs")
    fun importLogsByOperationId(@RequestParam("operation-id") operationId: String): ImportLogContainer {
        return zendeskMappingDao.getAllLogs(operationId)
    }

}

data class ZendeskImportResponse(val operationId: String, val logs: ImportLogContainer)