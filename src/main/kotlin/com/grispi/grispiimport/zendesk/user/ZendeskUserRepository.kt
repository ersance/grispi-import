package com.grispi.grispiimport.zendesk.user;

import com.grispi.grispiimport.zendesk.ZendeskEntityRepository
import com.grispi.grispiimport.zendesk.ZendeskUser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository;

@Repository
interface ZendeskUserRepository: MongoRepository<ZendeskUser, Long> {
    fun findAllByOperationIdAndActiveTrue(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskUser>
    fun findAllByOperationIdAndActiveFalse(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskUser>
    fun countAllByOperationIdAndActiveTrue(@Param("operationId") operationId: String): Int
    fun countAllByOperationIdAndActiveFalse(@Param("operationId") operationId: String): Int
}
