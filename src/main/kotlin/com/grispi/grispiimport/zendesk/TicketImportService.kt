package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiApiException
import jodd.json.JsonParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.CompletableFuture

@Service
class TicketImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskApi: ZendeskApi,
    @Autowired val zendeskMappingDao: ZendeskMappingDao,
) {

    companion object {
        const val RESOURCE_NAME = "ticket"

        const val PAGE_SIZE = 5
    }

    fun import(operationId: String, zendeskImportRequest: ZendeskImportRequest) {

        val ticketCount = zendeskApi.getTicketCount(zendeskImportRequest.zendeskApiCredentials)
        zendeskMappingDao.infoLog(operationId, RESOURCE_NAME, "${ticketCount} tickets found", null)
        println("ticket import process is started for ${ticketCount} tickets")

        val combinedTickets: MutableList<CompletableFuture<Unit>> = mutableListOf()
        for (index in 1..(BigDecimal(ticketCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt())) {
            println("fetching ${index}. page")

            val thenApplyAsync = zendeskApi
                .getTickets(zendeskImportRequest.zendeskApiCredentials, ZendeskPageParams(index, PAGE_SIZE))
                .thenApplyAsync { tickets -> import(tickets, zendeskImportRequest, operationId) }

            combinedTickets.add(thenApplyAsync)
        }

        CompletableFuture.allOf(*combinedTickets.toTypedArray()).get()

        println("ticket import process is done")
    }

    private fun import(zendeskTickets: List<ZendeskTicket>, zendeskImportRequest: ZendeskImportRequest, operationId: String) {
        for (zendeskTicket in zendeskTickets) {
            println(zendeskTicket.toTicketRequest(operationId, zendeskMappingDao::getUserId, zendeskMappingDao::getGroupId).toString())
            try {
                val createTicketResponse = grispiApi.createTicket(
                    zendeskTicket.toTicketRequest(operationId, zendeskMappingDao::getUserId, zendeskMappingDao::getGroupId),
                    zendeskImportRequest.grispiApiCredentials
                )

                zendeskMappingDao.addTicketMapping(operationId, zendeskTicket.id, createTicketResponse.bodyText())

                zendeskMappingDao.successLog(operationId, RESOURCE_NAME,
                    "{${zendeskTicket.subject}} created successfully",
                    null)
                        } catch (exception: RuntimeException) {
                            when (exception) {
                                is GrispiApiException -> {
                                    zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
                                        "{${zendeskTicket.subject} with id: ${zendeskTicket.id}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                                        null)
                                }
                                is GrispiReferenceNotFoundException -> {
                                    zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
                                        "{${zendeskTicket.subject} with id: ${zendeskTicket.id}} couldn't be imported. ${exception.message()}",
                                        null)
                                }
                                else -> {
                                    zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
                                        "{${zendeskTicket.subject} with id: ${zendeskTicket.id}} couldn't be imported. ${exception.message}",
                                        null)
                                }
                            }
                        }
        }
    }

}