package com.grispi.grispiimport.grispi

import com.grispi.grispiimport.utils.CalculateTimeSpent
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketCommentAggregationRepository
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketCommentRepository
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@Configuration
@Service
class GrispiTicketCommentImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskMappingQueryRepository: ZendeskMappingQueryRepository,
    @Autowired val zendeskLogRepository: ZendeskLogRepository,
    @Autowired val zendeskTicketCommentRepository: ZendeskTicketCommentRepository,
    @Autowired val zendeskTicketRepository: ZendeskTicketRepository,
    @Autowired val zendeskTicketCommentAggregationRepository: ZendeskTicketCommentAggregationRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val RESOURCE_NAME = "comment"
        const val PAGE_SIZE = 1000
    }

    @CalculateTimeSpent
    fun import(operationId: String, grispiApiCredentials: GrispiApiCredentials) {

        val commentedTickets = zendeskTicketCommentAggregationRepository.findCommentedTickets(operationId)

        logger.info("ticket comment import process is started for ${commentedTickets.count()} tickets at: ${LocalDateTime.now()}")

        val asyncCommentRequests: MutableList<CompletableFuture<ImportLog>> = mutableListOf()

        for (ticket in commentedTickets) {

            val asyncCommentRequest = grispiApi
                .createCommentsAsync(ticket.comments.map { it.toCommentRequest() }, grispiApiCredentials)
                .thenApply { response ->
                    zendeskLogRepository.save(
                        ImportLog(null, LogType.SUCCESS, RESOURCE_NAME,
                            "comments for ticket with key:{${response}} created successfully",
                            operationId)
                    )
                }
                .exceptionally { exception ->
                    when (exception.cause) {
                        is GrispiApiException -> {
                            val grispiApiException = exception.cause as GrispiApiException
                            zendeskLogRepository.save(
                                ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                    "comments for ticket with id: {${ticket.ticketKey}} couldn't be imported. status code: ${grispiApiException.statusCode} message: ${grispiApiException.exceptionMessage}",
                                    operationId))
                        }
                        else -> {
                            zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                "comments for ticket with  couldn't be imported. ${exception.message}", operationId))
                        }
                    }
                }

            logger.info("comments requested for ticket: ${ticket.ticketKey}")

            asyncCommentRequests.add(asyncCommentRequest)
        }
        CompletableFuture.allOf(*asyncCommentRequests.toTypedArray()).get(1, TimeUnit.DAYS)

        logger.info("ticket comment import process has ended for ${commentedTickets.count()} tickets at: ${LocalDateTime.now()}")
    }
}
