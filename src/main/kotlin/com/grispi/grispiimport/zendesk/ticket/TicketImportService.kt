package com.grispi.grispiimport.zendesk.ticket

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiApiException
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ZendeskApi.Companion.PAGE_SIZE
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

@Service
class TicketImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskApi: ZendeskApi,
    @Autowired val zendeskTicketRepository: ZendeskTicketRepository
) {

    companion object {
        const val RESOURCE_NAME = "ticket"
    }

    fun import(operationId: String, zendeskApiCredentials: ZendeskApiCredentials) {
        val ticketCount = zendeskApi.getTicketCount(zendeskApiCredentials)

//        zendeskMappingDao.infoLog(operationId, RESOURCE_NAME, "${ticketCount} tickets found", null)
        println("ticket import process is started for ${ticketCount} tickets at: ${LocalDateTime.now()}")

        val combinedTickets: MutableList<CompletableFuture<List<ZendeskTicket>>> = mutableListOf()
        for (index in 1..(BigDecimal(ticketCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt())) {
            println("fetching ${index}. page")

            val page = zendeskApi
                .getTickets(zendeskApiCredentials, ZendeskPageParams(index, PAGE_SIZE))
                .thenApply { tickets -> tickets.stream().peek { it.operationId = operationId }.collect(Collectors.toList()) }
                .thenApply { users -> zendeskTicketRepository.saveAll(users) }

            combinedTickets.add(page)
        }

        CompletableFuture.allOf(*combinedTickets.toTypedArray()).get(1, TimeUnit.DAYS)

        println("ticket import process is done")
    }

//    private fun import(zendeskTickets: List<ZendeskTicket>, zendeskImportRequest: ZendeskImportRequest, operationId: String) {
//        for (zendeskTicket in zendeskTickets) {
//            try {
//                val createTicketResponse = grispiApi.createTicket(
//                    zendeskTicket.toTicketRequest(operationId, zendeskMappingDao::getUserId, zendeskMappingDao::getGroupId),
//                    zendeskImportRequest.grispiApiCredentials
//                )
//
//                zendeskMappingDao.addTicketMapping(operationId, zendeskTicket.id, createTicketResponse.bodyText())
//
//                zendeskMappingDao.successLog(operationId, RESOURCE_NAME,
//                    "{${zendeskTicket.subject}} created successfully",
//                    null)
//            } catch (exception: RuntimeException) {
//                when (exception) {
//                    is GrispiApiException -> {
//                        zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
//                            "{${zendeskTicket.subject} with id: ${zendeskTicket.id}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
//                            null)
//                    }
//                    is GrispiReferenceNotFoundException -> {
//                        zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
//                            "{${zendeskTicket.subject} with id: ${zendeskTicket.id}} couldn't be imported. ${exception.message()}",
//                            null)
//                    }
//                    else -> {
//                        zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
//                            "{${zendeskTicket.subject} with id: ${zendeskTicket.id}} couldn't be imported. ${exception.message}",
//                            null)
//                    }
//                }
//            }
//        }
//    }

}