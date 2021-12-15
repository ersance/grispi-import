package com.grispi.grispiimport.zendesk.ticketfield

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.zendesk.ZendeskApi
import com.grispi.grispiimport.zendesk.ZendeskApiCredentials
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.stream.Collectors

@Service
class TicketFieldImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskApi: ZendeskApi,
    @Autowired val zendeskTicketFieldRepository: ZendeskTicketFieldRepository
) {

    companion object {
        const val RESOURCE_NAME = "ticket_field"
    }

    fun import(operationId: String, zendeskApiCredentials: ZendeskApiCredentials) {
        val zendeskTicketFields = zendeskApi.getTicketFields(zendeskApiCredentials)

        val filteredTicketFields = zendeskTicketFields.stream()
            .filter { ticketField -> !ZendeskTicketField.SYSTEM_FIELDS.contains(ticketField.type) }
            .peek { it.operationId = operationId }
            .collect(Collectors.toList())

        println("ticket field import process is started for ${filteredTicketFields?.count()} items at: ${LocalDateTime.now()}")

        zendeskTicketFieldRepository.saveAll(filteredTicketFields)

        println("ticket fields import process is done")
    }

}
