package com.grispi.grispiimport.zendesk.ticketfield;

import com.grispi.grispiimport.zendesk.ZendeskEntityRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository;

@Repository
interface ZendeskTicketFieldRepository: MongoRepository<ZendeskTicketField, String> {
    fun findAllByOperationId(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskTicketField>
}
