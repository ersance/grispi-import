package com.grispi.grispiimport.zendesk.ticketform

import com.grispi.grispiimport.zendesk.ZendeskApi
import com.grispi.grispiimport.zendesk.ZendeskApiCredentials
import com.grispi.grispiimport.zendesk.ticketfield.ZendeskTicketField
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service
class ZendeskTicketFormService(
    private val zendeskApi: ZendeskApi,
    private val zendeskTicketFormRepository: ZendeskTicketFormRepository
) {

    companion object {
        const val RESOURCE_NAME = "ticket_form"
        const val PAGE_SIZE = 1000
    }

    fun fetch(operationId: String, zendeskApiCredentials: ZendeskApiCredentials) {
        val zendeskTicketForms = zendeskApi.getTicketForms(zendeskApiCredentials)

        zendeskTicketForms.forEach { it.operationId = operationId }

        zendeskTicketFormRepository.saveAll(zendeskTicketForms)
    }

}

@Repository
interface ZendeskTicketFormRepository: MongoRepository<ZendeskTicketForm, Long> {
    fun findAllByOperationId(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskTicketForm>
}
