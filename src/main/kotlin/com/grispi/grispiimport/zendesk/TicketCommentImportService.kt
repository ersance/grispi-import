package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiApiException
import jodd.json.JsonParser
import jodd.json.JsonSerializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TicketCommentImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskApi: ZendeskApi,
    @Autowired val zendeskMappingDao: ZendeskMappingDao) {

    companion object {
        const val RESOURCE_NAME = "comment"
    }

    fun import(operationId: String, zendeskImportRequest: ZendeskImportRequest) {

        val ticketsZendeskIds = zendeskMappingDao.getTicketsZendeskIds(operationId)
        println("comment import process is started for ${ticketsZendeskIds.count()} tickets")

        for (zendeskId in ticketsZendeskIds) {
            try {
                val ticketComments = zendeskApi.getTicketComments(zendeskId, zendeskImportRequest.zendeskApiCredentials)

                val ticketKey = zendeskMappingDao.getTicketKey(operationId, zendeskId)

                println("processing for ticket: ${ticketKey}")
                println("${ticketComments.count()} comments found.")

                val commentRequests = ticketComments.map {it.toCommentRequest(operationId, ticketKey, zendeskMappingDao::getUserId)}

                grispiApi.createComments(commentRequests, zendeskImportRequest.grispiApiCredentials)

                zendeskMappingDao.successLog(operationId,
                    RESOURCE_NAME,
                    "comments for ticket with id:{${zendeskId}} created successfully",
                    null)
            } catch (exception: RuntimeException) {
                when (exception) {
                    is GrispiApiException -> {
                        zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
                            "comments for ticket with id: {${zendeskId}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                            null)
                    }
                    else -> {
                        zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
                            "comments for ticket with id: {${zendeskId}} couldn't be imported. ${exception.message}",
                            null)
                    }
                }
            }
        }

        println("ticket comment import process is done")
    }

}