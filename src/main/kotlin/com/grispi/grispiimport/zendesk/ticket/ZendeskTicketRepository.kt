package com.grispi.grispiimport.zendesk.ticket

import com.grispi.grispiimport.zendesk.ZendeskComment
import com.grispi.grispiimport.zendesk.ZendeskTicket
import com.grispi.grispiimport.zendesk.ZendeskUser
import org.bson.Document
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.aggregation.AggregationOptions
import org.springframework.data.mongodb.core.aggregation.AggregationResults
import org.springframework.data.mongodb.core.aggregation.Fields
import org.springframework.data.mongodb.core.query.Criteria
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

@Repository
class ZendeskTicketAggregationRepository(
    private val mongoTemplate: MongoTemplate
) {

    fun findAllTickets(operationId: String): MutableList<ZendeskTicketExt> {

        val projectionOperation = project()
            .and("_id").`as`("ticketId")
            .and("\$\$ROOT").`as`("ticket")
            .and("\$assignee.grispiId").arrayElementAt(0).`as`("grispiAssigneeId")
            .and("\$submitter.grispiId").arrayElementAt(0).`as`("grispiSubmitterId")
            .and("\$requester.grispiId").arrayElementAt(0).`as`("grispiRequesterId")
            .and("\$group.grispiId").arrayElementAt(0).`as`("grispiGroupId")
            .and("\$ticketForm.grispiId").arrayElementAt(0).`as`("grispiTicketFormId")
            .and("\$followers.grispiId").`as`("grispiFollowerIds")
            .and("\$emailCcs.grispiId").`as`("grispiEmailCcIds")

        val ticketAggregation = newAggregation(
            match(Criteria.where("operationId").`is`(operationId)),
            lookup("zendeskMapping", "assigneeId", "zendeskId", "assignee"),
            lookup("zendeskMapping", "submitterId", "zendeskId", "submitter"),
            lookup("zendeskMapping", "requesterId", "zendeskId", "requester"),
            lookup("zendeskMapping", "followerIds", "zendeskId", "followers"),
            lookup("zendeskMapping", "emailCcIds", "zendeskId", "emailCcs"),
            lookup("zendeskMapping", "groupId", "zendeskId", "group"),
            lookup("zendeskMapping", "ticketFormId", "zendeskId", "ticketForm"),
            projectionOperation
        ).withOptions(AggregationOptions.builder().allowDiskUse(true).build())

        val aggregate = mongoTemplate.aggregate(ticketAggregation, ZendeskTicket::class.java, ZendeskTicketExt::class.java)

        return aggregate.mappedResults
    }
}

data class ZendeskTicketExt(
    val ticketId: Long,
    val ticket: ZendeskTicket,
    val grispiAssigneeId: Long?,
    val grispiSubmitterId: Long?,
    val grispiRequesterId: Long?,
    val grispiGroupId: Long?,
    val grispiTicketFormId: Long?,
    val grispiFollowerIds: Set<Long>,
    val grispiEmailCcIds: Set<Long>,
) {

    fun mappings(): GrispiMappings {
        return GrispiMappings(grispiAssigneeId, grispiSubmitterId, grispiRequesterId, grispiGroupId, grispiTicketFormId, grispiFollowerIds, grispiEmailCcIds)
    }

}

data class GrispiMappings(
    val grispiAssigneeId: Long?,
    val grispiSubmitterId: Long?,
    val grispiRequesterId: Long?,
    val grispiGroupId: Long?,
    val grispiTicketFormId: Long?,
    val grispiFollowerIds: Set<Long> = emptySet(),
    val grispiEmailCcIds: Set<Long> = emptySet()
)