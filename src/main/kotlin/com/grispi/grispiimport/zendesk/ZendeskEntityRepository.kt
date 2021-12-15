package com.grispi.grispiimport.zendesk

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param

@NoRepositoryBean
interface ZendeskEntityRepository<T, ID>: MongoRepository<T, ID> {
    fun findAllByOperationId(@Param("operationId") operationId: String, pageable: Pageable): Page<T>
}