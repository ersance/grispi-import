package com.grispi.grispiimport.grispi

import com.grispi.grispiimport.utils.CalculateTimeSpent
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketAggregationRepository
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@Service
class GrispiTicketImportService(
    private val grispiApi: GrispiApi,
    private val zendeskMappingRepository: ZendeskMappingRepository,
    private val zendeskMappingQueryRepository: ZendeskMappingQueryRepository,
    private val zendeskLogRepository: ZendeskLogRepository,
    private val zendeskTicketRepository: ZendeskTicketRepository,
    private val zendeskTicketAggregationRepository: ZendeskTicketAggregationRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val RESOURCE_NAME = "ticket"
        const val PAGE_SIZE = 1000
        const val YUVANIKUR_BRAND_ID = 360002498720
        const val DUGUNBUKETI_BRAND_ID = 360000629299
    }

    @CalculateTimeSpent
    fun import(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        val ticketAggregations = zendeskTicketAggregationRepository.findAllTickets(operationId)

        logger.info("ticket import process is started for ${ticketAggregations.count()} tickets at: ${LocalDateTime.now()}")

        val asyncTicketRequests: MutableList<CompletableFuture<ImportLog>> = mutableListOf()

        for (ticketAggr in ticketAggregations) {

            val toTicketRequest = try {
                ticketAggr.ticket.toTicketRequest(ticketAggr.mappings())
            }
            catch (exception: RuntimeException) {
                if (exception is GrispiReferenceNotFoundException) {
                    zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                        "{${ticketAggr.ticket.subject} with id: ${ticketAggr.ticket.id}} couldn't be imported. ${exception.printMessage()}",
                        operationId))
                }
                else {
                    zendeskLogRepository.save(ImportLog(null, LogType.ERROR,
                        GrispiUserImportService.RESOURCE_NAME,
                        "{${ticketAggr.ticket.subject} with id: ${ticketAggr.ticket.id}} couldn't be imported. ${exception.message}",
                        operationId))
                }

                continue
            }

            val ticketRequest = grispiApi
                .createTicketAsync(toTicketRequest, grispiApiCredentials)
                .thenApply { ticketKey ->
                    zendeskMappingRepository.save(ZendeskMapping(null, ticketAggr.ticket.id, ticketKey, RESOURCE_NAME, operationId))
                    zendeskLogRepository.save(ImportLog(null, LogType.SUCCESS, RESOURCE_NAME, "{${ticketAggr.ticket.subject}} created successfully", operationId))
                }
                .exceptionally { exception ->
                    when (exception.cause) {
                        is GrispiApiException -> {
                            val grispiApiException = exception.cause as GrispiApiException
                            zendeskLogRepository.save(
                                ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                    "{${ticketAggr.ticket.subject} with id: ${ticketAggr.ticket.id}} couldn't be imported. status code: ${grispiApiException.statusCode} message: ${grispiApiException.exceptionMessage}",
                                    operationId))
                        }
                        else -> {
                            zendeskLogRepository.save(ImportLog(null, LogType.ERROR,
                                GrispiUserImportService.RESOURCE_NAME,
                                "{${ticketAggr.ticket.subject} with id: ${ticketAggr.ticket.id}} couldn't be imported. ${exception.message}",
                                operationId))
                        }
                    }
                }

            asyncTicketRequests.add(ticketRequest)
        }
        CompletableFuture.allOf(*asyncTicketRequests.toTypedArray()).get(1, TimeUnit.DAYS)

        logger.info("ticket import process has ended for ${ticketAggregations.count()} tickets at: ${LocalDateTime.now()}")
    }

}
