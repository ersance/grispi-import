package com.grispi.grispiimport.zendesk.ticket

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.zendesk.ZendeskApi
import com.grispi.grispiimport.zendesk.ZendeskApiCredentials
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class TicketCommentImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskApi: ZendeskApi,
    @Autowired val ticketRepository: ZendeskTicketRepository,
    @Autowired val ticketCommentRepository: ZendeskTicketCommentRepository
) {

    companion object {
        const val RESOURCE_NAME = "comment"
        const val PAGE_SIZE = 1000
    }

    fun import(operationId: String, zendeskApiCredentials: ZendeskApiCredentials) {

        var commentedTickets = ticketRepository.findCommentedTickets(Pageable.ofSize(PAGE_SIZE))

        println("comment import process is started for ${commentedTickets.totalElements} tickets at: ${LocalDateTime.now()}")

        do {
            println("fetching ${commentedTickets.pageable.pageNumber}. page")
            for (zendeskTicket in commentedTickets.content) {
                // TODO: 14.12.2021 check if pageable
                val ticketComments = zendeskApi.getTicketComments(zendeskTicket.id, zendeskApiCredentials)
                ticketComments.forEach {
                    it.operationId = operationId
                    it.ticketId = zendeskTicket.id
                }

                ticketCommentRepository.saveAll(ticketComments)
            }

            if (commentedTickets.hasNext()) {
                commentedTickets = ticketRepository.findCommentedTickets(commentedTickets.nextPageable())
            }
        } while (commentedTickets.hasNext())

//        val ticketsZendeskIds = zendeskMappingDao.getTicketsZendeskIds(operationId)
//        println("comment import process is started for ${ticketsZendeskIds.count()} tickets at: ${LocalDateTime.now()}")

//        for (zendeskId in ticketsZendeskIds) {
//            try {
//                val ticketComments = zendeskApi.getTicketComments(zendeskId, zendeskImportRequest.zendeskApiCredentials)
//
//                val ticketKey = zendeskMappingDao.getTicketKey(operationId, zendeskId)
//
//                println("${ticketComments.count()} comments found.")
//
//                val commentRequests = ticketComments.map {it.toCommentRequest(operationId, ticketKey, zendeskMappingDao::getUserId)}
//
//                grispiApi.createComments(commentRequests, zendeskImportRequest.grispiApiCredentials)
//
//                zendeskMappingDao.successLog(operationId,
//                    RESOURCE_NAME,
//                    "comments for ticket with id:{${zendeskId}} created successfully",
//                    null)
//            } catch (exception: RuntimeException) {
//                when (exception) {
//                    is GrispiApiException -> {
//                        zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
//                            "comments for ticket with id: {${zendeskId}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
//                            null)
//                    }
//                    else -> {
//                        zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
//                            "comments for ticket with id: {${zendeskId}} couldn't be imported. ${exception.message}",
//                            null)
//                    }
//                }
//            }
//        }

        println("ticket comment import process is done at: ${LocalDateTime.now()}")
    }

}