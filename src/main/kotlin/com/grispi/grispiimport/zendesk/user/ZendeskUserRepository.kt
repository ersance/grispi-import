package com.grispi.grispiimport.zendesk.user;

import com.grispi.grispiimport.zendesk.ZendeskEntityRepository
import com.grispi.grispiimport.zendesk.ZendeskUser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository;

@Repository
interface ZendeskUserRepository: MongoRepository<ZendeskUser, String> {
    fun findAllByOperationId(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskUser>
}
