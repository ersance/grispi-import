package com.grispi.grispiimport.zendesk.userfield;

import com.grispi.grispiimport.zendesk.ZendeskEntityRepository
import com.grispi.grispiimport.zendesk.ZendeskUserField
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository;

@Repository
interface ZendeskUserFieldRepository: MongoRepository<ZendeskUserField, String> {
    fun findAllByOperationId(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskUserField>
}
