package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.common.ImportLogContainer
import com.grispi.grispiimport.common.ImportLogDao
import com.grispi.grispiimport.grispi.GrispiApi
import jodd.json.JsonSerializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ZendeskImportController(
    @Autowired val organizationImportService: OrganizationImportService,
    @Autowired val groupImportService: GroupImportService,
    @Autowired val ticketFieldImportService: TicketFieldImportService,
    @Autowired val userImportService: UserImportService,
    @Autowired val ticketImportService: TicketImportService,
    @Autowired val importLogDao: ImportLogDao
) {

    @PostMapping("/import")
    fun importZendeskResources(@RequestBody zendeskImportRequest: ZendeskImportRequest): ImportLogContainer {

        // organization
        organizationImportService.import(zendeskImportRequest)

        // group
        groupImportService.import(zendeskImportRequest)

        // custom fields
        ticketFieldImportService.import(zendeskImportRequest)

        // users
        userImportService.import(zendeskImportRequest)

        // tickets with comments
        ticketImportService.import(zendeskImportRequest)

        return importLogDao.getAllLogs()
    }

}