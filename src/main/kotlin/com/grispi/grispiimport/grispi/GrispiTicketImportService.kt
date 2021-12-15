package com.grispi.grispiimport.grispi

import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

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
    }

    fun import(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        var tickets = zendeskTicketRepository.findAllByOperationId(operationId, Pageable.ofSize(PAGE_SIZE))

        println("ticket import process is started for ${tickets.totalElements} tickets at: ${LocalDateTime.now()}")

        do {
            println("fetching ${tickets.pageable.pageNumber}. page")
            for (ticket in tickets.content) {
                try {
                    val createTicketResponse = grispiApi.createTicket(
                            ticket.toTicketRequest(zendeskMappingQueryRepository::findGrispiUserId, zendeskMappingQueryRepository::findGrispiGroupId),
                            grispiApiCredentials
                        )

                    zendeskMappingRepository.save(ZendeskMapping(null, ticket.id, createTicketResponse.bodyText(), RESOURCE_NAME, operationId))

                    zendeskLogRepository.save(ImportLog(null, LogType.SUCCESS, RESOURCE_NAME, "{${ticket.subject}} created successfully", operationId))
                } catch (exception: RuntimeException) {
                    when (exception) {
                        is GrispiApiException -> {
                            zendeskLogRepository.save(
                                ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                    "{${ticket.subject} with id: ${ticket.id}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                                    operationId))
                        }
                        is GrispiReferenceNotFoundException -> {
                            zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                "{${ticket.subject} with id: ${ticket.id}} couldn't be imported. ${exception.message}",
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

            if (tickets.hasNext()) {
                tickets = zendeskTicketRepository.findAllByOperationId(operationId, tickets.nextPageable())
            }
        } while (tickets.hasNext())
    }

}
