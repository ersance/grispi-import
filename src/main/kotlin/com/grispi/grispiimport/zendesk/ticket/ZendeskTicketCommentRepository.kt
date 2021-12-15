package com.grispi.grispiimport.zendesk.ticket

import com.grispi.grispiimport.zendesk.ZendeskComment
import com.grispi.grispiimport.zendesk.ZendeskEntityRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ZendeskTicketCommentRepository: MongoRepository<ZendeskComment, Long> {

    fun findAllByOperationId(@Param("operationId") operationId: String): List<ZendeskComment>

}
