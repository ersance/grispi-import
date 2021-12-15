package com.grispi.grispiimport.zendesk.group;

import com.grispi.grispiimport.zendesk.ZendeskEntityRepository
import com.grispi.grispiimport.zendesk.group.ZendeskGroup
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository;

@Repository
interface ZendeskGroupRepository: MongoRepository<ZendeskGroup, Long> {
    fun findAllByOperationId(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskGroup>
}
