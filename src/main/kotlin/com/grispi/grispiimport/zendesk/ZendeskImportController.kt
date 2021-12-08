package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.common.ImportLogContainer
import com.grispi.grispiimport.common.ImportLogDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class ZendeskImportController(
    @Autowired val organizationImportService: OrganizationImportService,
    @Autowired val groupImportService: GroupImportService,
    @Autowired val ticketFieldImportService: TicketFieldImportService,
    @Autowired val userImportService: UserImportService,
    @Autowired val ticketImportService: TicketImportService,
    @Autowired val importLogDao: ImportLogDao,
    @Autowired val zendeskMappingDao: ZendeskMappingDao
) {

    @PostMapping("/import")
    fun importZendeskResources(@RequestBody zendeskImportRequest: ZendeskImportRequest): ZendeskImportResponse {

        val operationId = UUID.randomUUID().toString()

        organizationImportService.import(operationId, zendeskImportRequest)

        groupImportService.import(operationId, zendeskImportRequest)

        ticketFieldImportService.import(operationId, zendeskImportRequest)

        userImportService.import(operationId, zendeskImportRequest)

        ticketImportService.import(operationId, zendeskImportRequest)

        // ticket comments

        return ZendeskImportResponse(operationId, zendeskMappingDao.getAllLogs(operationId))
    }

}

data class ZendeskImportResponse(val operationId: String, val logs: ImportLogContainer)