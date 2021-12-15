package com.grispi.grispiimport.zendesk.ticket

import com.grispi.grispiimport.zendesk.ZendeskEntityRepository
import com.grispi.grispiimport.zendesk.ZendeskTicket
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ZendeskTicketRepository: MongoRepository<ZendeskTicket, Long> {

    fun findAllByOperationId(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskTicket>

    @Query(value = "{'commentCount' : { '\$gt': 1} }", fields="{ 'key' : 1}", count = true)
    fun findCommentedTickets(pageable: Pageable): Page<ZendeskTicket>

}
