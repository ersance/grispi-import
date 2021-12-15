package com.grispi.grispiimport.grispi

import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketCommentRepository
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GrispiImportService(
    @Autowired val organizationService: GrispiOrganizationImportService,
    @Autowired val groupImportService: GrispiGroupImportService,
    @Autowired val ticketFieldService: GrispiTicketFieldImportService,
    @Autowired val userFieldService: GrispiUserFieldImportService,
    @Autowired val userService: GrispiUserImportService,
    @Autowired val grispiTicketImportService: GrispiTicketImportService,
    @Autowired val grispiTicketCommentImportService: GrispiTicketCommentImportService,
) {

    fun import(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        println("grispi imports started at ${LocalDateTime.now()}")
        organizationService.import(operationId, grispiApiCredentials)
        groupImportService.import(operationId, grispiApiCredentials)
        ticketFieldService.import(operationId, grispiApiCredentials)
        userFieldService.import(operationId, grispiApiCredentials)
        userService.import(operationId, grispiApiCredentials)
        grispiTicketImportService.import(operationId, grispiApiCredentials)
        grispiTicketCommentImportService.import(operationId, grispiApiCredentials)
    }

}