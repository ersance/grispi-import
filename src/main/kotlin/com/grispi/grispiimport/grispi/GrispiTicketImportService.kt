package com.grispi.grispiimport.grispi

import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@Service
class GrispiTicketImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskMappingRepository: ZendeskMappingRepository,
    @Autowired val zendeskMappingQueryRepository: ZendeskMappingQueryRepository,
    @Autowired val zendeskLogRepository: ZendeskLogRepository,
    @Autowired val zendeskTicketRepository: ZendeskTicketRepository
) {

    companion object {
        const val RESOURCE_NAME = "ticket"
        const val PAGE_SIZE = 1000
        const val YUVANIKUR_BRAND_ID = 360002498720
    }

    fun import(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        val ticketCount = zendeskTicketRepository.countAllByOperationIdAndBrandId(operationId, YUVANIKUR_BRAND_ID)

        println("user import process is started for ${ticketCount} users at: ${LocalDateTime.now()}")

        val to = BigDecimal(ticketCount).divide(BigDecimal(GrispiUserImportService.PAGE_SIZE), RoundingMode.UP).toInt()
        for (index in 0 until to) {
            val tickets = zendeskTicketRepository.findAllByOperationIdAndBrandId(operationId, YUVANIKUR_BRAND_ID, PageRequest.of(index, PAGE_SIZE))

            println("fetching {${tickets.pageable.pageNumber}}. page for {${tickets.content.count()}} tickets")

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
    }

//    fun import(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
//        var tickets = zendeskTicketRepository.findAllByOperationIdAndBrandId(operationId, YUVANIKUR_BRAND_ID, Pageable.ofSize(PAGE_SIZE))
//
//        println("grispi ticket import process is started for ${tickets.totalElements} tickets at: ${LocalDateTime.now()}")
//
//        do {
//            println("fetching ${tickets.pageable.pageNumber}. page")
//            for (ticket in tickets.content) {
//                try {
//                    val ticketKey = grispiApi.createTicket(ticket.toTicketRequest(
//                        zendeskMappingQueryRepository::findGrispiUserId,
//                        zendeskMappingQueryRepository::findGrispiGroupId,
//                        zendeskMappingQueryRepository::findGrispiTicketFormId
//                    ), grispiApiCredentials)
//
//                    zendeskMappingRepository.save(ZendeskMapping(null, ticket.id, ticketKey, RESOURCE_NAME, operationId))
//
//                    zendeskLogRepository.save(ImportLog(null, LogType.SUCCESS, RESOURCE_NAME, "{${ticket.subject}} created successfully", operationId))
//                } catch (exception: RuntimeException) {
//                    when (exception) {
//                        is GrispiApiException -> {
//                            zendeskLogRepository.save(
//                                ImportLog(null, LogType.ERROR, RESOURCE_NAME,
//                                    "{${ticket.subject} with id: ${ticket.id}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
//                                    operationId))
//                        }
//                        is GrispiReferenceNotFoundException -> {
//                            zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
//                                "{${ticket.subject} with id: ${ticket.id}} couldn't be imported. ${exception.printMessage()}",
//                                operationId))
//                        }
//                        else -> {
//                            zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
//                                "{${ticket.subject} with id: ${ticket.id}} couldn't be imported. ${exception.message}",
//                                operationId))
//                        }
//                    }
//                }
//            }
//
//            if (tickets.hasNext()) {
//                tickets = zendeskTicketRepository.findAllByOperationIdAndBrandId(operationId, YUVANIKUR_BRAND_ID, tickets.nextPageable())
//            }
//        } while (tickets.hasNext())
//    }

}
