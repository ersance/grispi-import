package com.grispi.grispiimport.grispi

import com.grispi.grispiimport.utils.CalculateTimeSpent
import com.grispi.grispiimport.zendesk.*
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
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskMappingRepository: ZendeskMappingRepository,
    @Autowired val zendeskMappingQueryRepository: ZendeskMappingQueryRepository,
    @Autowired val zendeskLogRepository: ZendeskLogRepository,
    @Autowired val zendeskTicketRepository: ZendeskTicketRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val RESOURCE_NAME = "ticket"
        const val PAGE_SIZE = 1000
        const val YUVANIKUR_BRAND_ID = 360002498720
        const val DUGUNBUKETI_BRAND_ID = 360000629299
    }

    fun importBrand(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        val ticketCount = zendeskTicketRepository.countAllByOperationIdAndBrandId(operationId, YUVANIKUR_BRAND_ID)

        logger.info("ticket import process is started for ${ticketCount} tickets at: ${LocalDateTime.now()}")

        val to = BigDecimal(ticketCount).divide(BigDecimal(GrispiUserImportService.PAGE_SIZE), RoundingMode.UP).toInt()
        for (index in 0 until to) {
            val tickets = zendeskTicketRepository.findAllByOperationIdAndBrandId(operationId, YUVANIKUR_BRAND_ID, PageRequest.of(index, PAGE_SIZE))

            logger.info("fetching {${tickets.pageable.pageNumber}}. page for {${tickets.content.count()}} tickets")

            for (ticket in tickets.content) {
                try {
                    val ticketKey = grispiApi.createTicket(ticket.toTicketRequest(
                        zendeskMappingQueryRepository::findGrispiUserId,
                        zendeskMappingQueryRepository::findGrispiGroupId,
                        zendeskMappingQueryRepository::findGrispiTicketFormId
                    ), grispiApiCredentials)

                    zendeskMappingRepository.save(ZendeskMapping(null, ticket.id, ticketKey, RESOURCE_NAME, operationId))

                    zendeskLogRepository.save(ImportLog(null, LogType.SUCCESS, RESOURCE_NAME, "{${ticket.subject}} created successfully", operationId))
                }
                catch (exception: RuntimeException) {
                    when (exception) {
                        is GrispiApiException -> {
                            zendeskLogRepository.save(
                                ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                    "{${ticket.subject} with id: ${ticket.id}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                                    operationId))
                        }
                        is GrispiReferenceNotFoundException -> {
                            zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                "{${ticket.subject} with id: ${ticket.id}} couldn't be imported. ${exception.printMessage()}",
                                operationId))
                        }
                        else -> {
                            zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                "{${ticket.subject} with id: ${ticket.id}} couldn't be imported. ${exception.message}",
                                operationId))
                        }
                    }
                }
            }

        }

        logger.info("ticket import process has ended for ${ticketCount} tickets at: ${LocalDateTime.now()}")
    }

    @CalculateTimeSpent
    fun import(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        val ticketCount = zendeskTicketRepository.countAllByOperationId(operationId)

        logger.info("ticket import process is started for ${ticketCount} tickets at: ${LocalDateTime.now()}")

        val ticketRequests: MutableList<CompletableFuture<ImportLog>> = mutableListOf()

        val to = BigDecimal(ticketCount).divide(BigDecimal(GrispiUserImportService.PAGE_SIZE), RoundingMode.UP).toInt()
        for (index in 0 until to) {
            val tickets = zendeskTicketRepository.findAllByOperationId(operationId, PageRequest.of(index, PAGE_SIZE))

            logger.info("fetching {${tickets.pageable.pageNumber}}. page for {${tickets.content.count()}} tickets")

            for (ticket in tickets.content) {

                val toTicketRequest = try {
                    ticket.toTicketRequest(
                        zendeskMappingQueryRepository::findGrispiUserId,
                        zendeskMappingQueryRepository::findGrispiGroupId,
                        zendeskMappingQueryRepository::findGrispiTicketFormId)
                }
                catch (exception: RuntimeException) {
                    if (exception is GrispiReferenceNotFoundException) {
                        zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                            "{${ticket.subject} with id: ${ticket.id}} couldn't be imported. ${exception.printMessage()}",
                            operationId))
                    }
                    else {
                        zendeskLogRepository.save(ImportLog(null, LogType.ERROR,
                            GrispiUserImportService.RESOURCE_NAME,
                            "{${ticket.subject} with id: ${ticket.id}} couldn't be imported. ${exception.message}",
                            operationId))
                    }

                    continue
                }

                val ticketRequest = grispiApi
                    .createTicketAsync(toTicketRequest, grispiApiCredentials)
                    .thenApply { ticketKey ->
                        zendeskMappingRepository.save(ZendeskMapping(null, ticket.id, ticketKey, RESOURCE_NAME, operationId))
                        zendeskLogRepository.save(ImportLog(null, LogType.SUCCESS, RESOURCE_NAME, "{${ticket.subject}} created successfully", operationId))
                    }
                    .exceptionally { exception ->
                        when (exception.cause) {
                            is GrispiApiException -> {
                                val grispiApiException = exception.cause as GrispiApiException
                                zendeskLogRepository.save(
                                    ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                        "{${ticket.subject} with id: ${ticket.id}} couldn't be imported. status code: ${grispiApiException.statusCode} message: ${grispiApiException.exceptionMessage}",
                                        operationId))
                            }
                            else -> {
                                zendeskLogRepository.save(ImportLog(null, LogType.ERROR,
                                    GrispiUserImportService.RESOURCE_NAME,
                                    "{${ticket.subject} with id: ${ticket.id}} couldn't be imported. ${exception.message}",
                                    operationId))
                            }
                        }
                    }

                ticketRequests.add(ticketRequest)
            }
        }
        CompletableFuture.allOf(*ticketRequests.toTypedArray()).get(1, TimeUnit.DAYS)

        logger.info("ticket import process has ended for ${ticketCount} tickets at: ${LocalDateTime.now()}")
    }

}
