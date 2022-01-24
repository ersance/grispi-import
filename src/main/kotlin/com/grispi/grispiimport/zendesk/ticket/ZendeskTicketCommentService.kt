package com.grispi.grispiimport.zendesk.ticket

import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.organization.ResourceCount
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@Service
class ZendeskTicketCommentService(
    private val zendeskApi: ZendeskApi,
    private val ticketRepository: ZendeskTicketRepository,
    private val ticketCommentRepository: ZendeskTicketCommentRepository,
    private val apiLimitWatcher: ApiLimitWatcher,
    private val commentMapRepository: CommentMapRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val RESOURCE_NAME = "comment"
        const val PAGE_SIZE = 1000
    }

    fun fetch(operationId: String, zendeskApiCredentials: ZendeskApiCredentials, startingFrom: Int? = 0) {
        val commentedTicketsCount = ticketRepository.findCommentedTicketCount(operationId)

        logger.info("ticket comment fetch process is started for ${commentedTicketsCount} tickets at: ${LocalDateTime.now()}")

        val combinedCommentRequests: MutableList<CompletableFuture<Void>> = mutableListOf()

        val to = BigDecimal(commentedTicketsCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt()
        for (index in (startingFrom)!!.rangeTo(to)) {
            val commentedTickets = ticketRepository.findCommentedTickets(operationId, PageRequest.of(index, PAGE_SIZE))

            for (ticket in commentedTickets) {
                if (apiLimitWatcher.isApiUnavailable(operationId)) {
                    commentMapRepository.save(CommentMap(ticket.id, apiAvailable = false))
                    val retryAfterFor = apiLimitWatcher.getRetryAfterFor(operationId)
                    logger.info("sleeping user thread for ${retryAfterFor} page ${index}")
                    CompletableFuture.supplyAsync(
                        { fetch(operationId, zendeskApiCredentials, index) },
                        CompletableFuture.delayedExecutor(retryAfterFor, TimeUnit.SECONDS)
                    )

                    break;
                }

                commentMapRepository.save(CommentMap(ticket.id, callingZendesk = true))
                val getTicketComments = zendeskApi
                    .getTicketComments(ticket.id, zendeskApiCredentials, this::save)
                    .thenApply { comments -> save(comments, operationId, ticket.id) }
                    .thenRun {
                        MDC.put("operationId", operationId)
                        logger.info("comments fetched for page: ${index}")
                    }

                combinedCommentRequests.add(getTicketComments)
            }

            logger.info("ticket comments fetched for page: ${index}")
        }

        CompletableFuture.allOf(*combinedCommentRequests.toTypedArray()).get(1, TimeUnit.DAYS)

        logger.info("ticket comments import process is done")
    }

    fun save(comments: List<ZendeskComment>, operationId: String, ticketId: Long): List<ZendeskComment> {

        if (comments.isEmpty()) {
            logger.info("no comment found. ")
            return emptyList()
        }
        else {
            logger.info("${comments.count()} comments fetched")
        }

        comments.forEach {
            it.operationId = operationId
            it.ticketId = ticketId
        }

        return ticketCommentRepository.saveAll(comments)
    }

    fun fetchedCommentsCount(operationId: String): Long {
        return ticketCommentRepository.countAllByOperationId(operationId)
    }

    fun counts(operationId: String): ResourceCount {
        return CompletableFuture
            .supplyAsync { ticketRepository.calculateCommentCountByOperationId(operationId).uniqueMappedResult?.total() }
            .thenCombine(
                CompletableFuture.supplyAsync { fetchedCommentsCount(operationId) },
                { zCount, fCount -> ResourceCount(RESOURCE_NAME, zCount, fCount) })
            .get()
    }

}

@Repository
interface ZendeskTicketCommentRepository: MongoRepository<ZendeskComment, Long> {

    fun findAllByOperationIdAndTicketIdIsIn(@Param("operationId") operationId: String, @Param("ticketIds") ticketIds: List<Long>): List<ZendeskComment>

    fun countAllByOperationId(operationId: String): Long

}

