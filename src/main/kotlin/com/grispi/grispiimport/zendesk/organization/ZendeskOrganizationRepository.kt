package com.grispi.grispiimport.zendesk.organization;

import com.grispi.grispiimport.zendesk.ZendeskEntityRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository;

@Repository
interface ZendeskOrganizationRepository: MongoRepository<ZendeskOrganization, Long> {
    fun findAllByOperationId(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskOrganization>
}
