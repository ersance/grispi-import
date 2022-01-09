package com.grispi.grispiimport.zendesk.ticketfield

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiGroupImportService
import com.grispi.grispiimport.utils.CalculateTimeSpent
import com.grispi.grispiimport.zendesk.ZendeskApi
import com.grispi.grispiimport.zendesk.ZendeskApiCredentials
import com.grispi.grispiimport.zendesk.ZendeskMapping
import com.grispi.grispiimport.zendesk.ZendeskMappingRepository
import com.grispi.grispiimport.zendesk.organization.ResourceCount
import com.grispi.grispiimport.zendesk.organization.ZendeskOrganizationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors

@Service
class ZendeskTicketFieldService(
    private val zendeskApi: ZendeskApi,
    private val zendeskMappingRepository: ZendeskMappingRepository,
    private val zendeskTicketFieldRepository: ZendeskTicketFieldRepository
) {

    companion object {
        const val RESOURCE_NAME = "ticket_field"
        const val DISCARDED_FIELD_KEY = "discarded_field"

        /**
         * group is unnecessary for Grispi
         */
        val FIELDS_TO_EXCLUDE: Set<String> = setOf("group")
    }

    @CalculateTimeSpent
    fun fetch(operationId: String, zendeskApiCredentials: ZendeskApiCredentials) {
        val zendeskTicketFields = zendeskApi.getTicketFields(zendeskApiCredentials)

        val filteredTicketFields = zendeskTicketFields.stream()
            .filter { ticketField -> !ZendeskTicketField.SYSTEM_FIELDS.contains(ticketField.type) && !FIELDS_TO_EXCLUDE.contains(ticketField.type) }
            .peek { it.operationId = operationId }
            .collect(Collectors.toList())

        zendeskTicketFields.stream()
            .filter { ticketField -> ZendeskTicketField.SYSTEM_FIELDS.contains(ticketField.type) }
            .forEach { ticketField ->
                zendeskMappingRepository.save(ZendeskMapping(null, ticketField.id, "ts.${ticketField.type}", RESOURCE_NAME, operationId))
            }

        zendeskTicketFields.stream()
            .filter { ticketField -> FIELDS_TO_EXCLUDE.contains(ticketField.type) }
            .forEach { ticketField ->
                zendeskMappingRepository.save(ZendeskMapping(null, ticketField.id, DISCARDED_FIELD_KEY, RESOURCE_NAME, operationId))
            }

        zendeskTicketFieldRepository.saveAll(filteredTicketFields)
    }

    fun fetchedTicketFieldsCount(operationId: String): Long {
        return zendeskTicketFieldRepository.countAllByOperationId(operationId)
    }

    fun counts(operationId: String, zendeskApiCredentials: ZendeskApiCredentials): ResourceCount {
        return CompletableFuture
            .supplyAsync { zendeskApi.getTicketFieldCount(zendeskApiCredentials) }
            .thenCombine(
                CompletableFuture.supplyAsync { fetchedTicketFieldsCount(operationId) },
                { zCount, fCount -> ResourceCount(RESOURCE_NAME, zCount, fCount) })
            .get()
    }

}

@Repository
interface ZendeskTicketFieldRepository: MongoRepository<ZendeskTicketField, String> {
    fun findAllByOperationId(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskTicketField>
    fun countAllByOperationId(@Param("operationId") operationId: String): Long
}

