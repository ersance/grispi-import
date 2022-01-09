package com.grispi.grispiimport.grispi

import com.grispi.grispiimport.utils.CalculateTimeSpent
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketCommentRepository
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val RESOURCE_NAME = "comment"
        const val PAGE_SIZE = 1000
    }

    @CalculateTimeSpent
    fun import(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        val commentedTicketIds = zendeskTicketRepository.findCommentedTicketIds(operationId)

        val groupedComments = zendeskTicketCommentRepository
            .findAllByOperationIdAndTicketIdIsIn(operationId, commentedTicketIds.stream().map { it.id }.toList())
            .groupBy { it.ticketId }

        val commentRequests: MutableList<CompletableFuture<ImportLog>> = mutableListOf()

        logger.info("ticket comment import process is started for ${groupedComments.count()} tickets at: ${LocalDateTime.now()}")
        for (ticket in groupedComments.entries) {

            val comments = ticket.value.stream()
                .map { it.toCommentRequest(zendeskMappingQueryRepository::findGrispiTicketKey, zendeskMappingQueryRepository::findGrispiUserId) }
                .toList()

            val commentRequest = grispiApi
                .createCommentsAsync(comments, grispiApiCredentials)
                .thenApply { response ->
                    zendeskLogRepository.save(
                        ImportLog(null, LogType.SUCCESS, RESOURCE_NAME,
                            "comments for ticket with id:{${ticket.key}} created successfully",
                            operationId)
                    )
                }
                .exceptionally { exception ->
                    when (exception.cause) {
                        is GrispiApiException -> {
                            val grispiApiException = exception.cause as GrispiApiException
                            zendeskLogRepository.save(
                                ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                    "comments for ticket with id: {${ticket.key}} couldn't be imported. status code: ${grispiApiException.statusCode} message: ${grispiApiException.exceptionMessage}",
                                    operationId))
                        }
                        is GrispiReferenceNotFoundException -> {
                            val grispiReferenceNotFoundException = exception.cause as GrispiReferenceNotFoundException
                            zendeskLogRepository.save(ImportLog(null, LogType.ERROR,
                                GrispiTicketImportService.RESOURCE_NAME,
                                "{${ticket.key}} comments couldn't be imported. ${grispiReferenceNotFoundException.printMessage()}",
                                operationId))
                        }
                        else -> {
                            zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                "comments for ticket with id: {${ticket.key}} couldn't be imported. ${exception.message}",
                                operationId))
                        }
                    }
                }

            commentRequests.add(commentRequest)
        }

        CompletableFuture.allOf(*commentRequests.toTypedArray()).get(1, TimeUnit.DAYS)
        logger.info("ticket comment import process has ended for ${groupedComments.count()} tickets at: ${LocalDateTime.now()}")
    }

    fun importForBrand(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        val commentedTicketIdsForBrand = zendeskTicketRepository.findCommentedTicketIdsForBrand(operationId, GrispiTicketImportService.YUVANIKUR_BRAND_ID)

        val tickets = zendeskTicketCommentRepository
            .findAllByOperationIdAndTicketIdIsIn(operationId, commentedTicketIdsForBrand.stream().map { it.id }.toList())
            .groupBy { it.ticketId }

        logger.info("missing ids: ${commentedTicketIdsForBrand.stream().filter { !tickets.keys.contains(it.id) }.toList()}")

        logger.info("ticket comment import process is started for ${tickets.count()} tickets at: ${LocalDateTime.now()}")
        for (ticket in tickets.entries) {
            try {
                val comments = ticket.value.stream()
                    .map { it.toCommentRequest(zendeskMappingQueryRepository::findGrispiTicketKey, zendeskMappingQueryRepository::findGrispiUserId) }
                    .toList()

                grispiApi.createComments(comments, grispiApiCredentials)

                zendeskLogRepository.save(ImportLog(null, LogType.SUCCESS, RESOURCE_NAME, "comments for ticket with id:{${ticket.key}} created successfully", operationId))
            } catch (exception: RuntimeException) {
                when (exception) {
                    is GrispiApiException -> {
                        zendeskLogRepository.save(
                            ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                "comments for ticket with id: {${ticket.key}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                                operationId))
                    }
                    else -> {
                        zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                            "comments for ticket with id: {${ticket.key}} couldn't be imported. ${exception.message}",
                            operationId))
                    }
                }
            }
        }
        logger.info("ticket comment import process has ended for ${tickets.count()} tickets at: ${LocalDateTime.now()}")
    }

}
