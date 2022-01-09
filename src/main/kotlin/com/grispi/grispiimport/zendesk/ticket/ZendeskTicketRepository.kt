package com.grispi.grispiimport.zendesk.ticket

import com.grispi.grispiimport.zendesk.ZendeskTicket
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.aggregation.AggregationResults
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ZendeskTicketRepository: MongoRepository<ZendeskTicket, Long> {

    fun findAllByOperationId(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskTicket>

    fun countAllByOperationId(@Param("operationId") operationId: String): Long

//    TODO: @Aggregation("{'\$match': {'operationId': ?0, commentCount: {'\$gt': 1}}}, {'\$group': {_id: null, total: {'\$sum:' \$commentCount}}}")
    @Query(value = "{'operationId': ?0, 'commentCount': {'\$gt': 1}}")
    fun findCommentedTickets(operationId: String, pageable: Pageable): Page<ZendeskTicket>

    @Query(value = "{'operationId': ?0, 'commentCount': {'\$gt': 1}}")
    fun findCommentedTicketIds(operationId: String): Set<ZendeskTicket>

    @Query(value = "{'commentCount': {'\$gt': 1}, 'operationId': ?0}", count = true)
    fun findCommentedTicketCount(@Param("operationId") operationId: String): Long

    @Aggregation("{'\$match': {commentCount: {'\$gt': 1} }, 'operationId': ?0}, {'\$group': {_id: null, total: {'\$sum:' \$commentCount}}}")
    fun calculateCommentCountByOperationId(@Param("operationId") operationId: String): AggregationResults<SumValue>

    /**
     * fixme: querying with brand id is just a workaround solution and must be removed after {yuvanikur} brand is completely fetched and imported
     */
    fun findAllByOperationIdAndBrandId(@Param("operationId") operationId: String, @Param("brandId") brandId: Long, pageable: Pageable): Page<ZendeskTicket>
    fun countAllByOperationIdAndBrandId(@Param("operationId") operationId: String, @Param("brandId") brandId: Long): Int

    @Query(value = "{'operationId': ?0, 'brandId': ?1, 'commentCount': {'\$gt': 1}}")
    fun findCommentedTicketIdsForBrand(operationId: String, brandId: Long): Set<ZendeskTicket>

}

interface SumValue {
    fun total(): Long
}
