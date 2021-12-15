package com.grispi.grispiimport.grispi

import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketCommentRepository
import jodd.json.JsonParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GrispiTicketCommentImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskMappingQueryRepository: ZendeskMappingQueryRepository,
    @Autowired val zendeskLogRepository: ZendeskLogRepository,
    @Autowired val zendeskTicketCommentRepository: ZendeskTicketCommentRepository,
) {

    companion object {
        const val RESOURCE_NAME = "comment"
        const val PAGE_SIZE = 1000
    }

    fun import(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        var ticketComments = zendeskTicketCommentRepository.findAllByOperationId(operationId)
        val groupedComments = ticketComments
            .groupBy { ZendeskComment::ticketId }
            .mapValues { it.value
                .map { it.toCommentRequest(zendeskMappingQueryRepository::findGrispiTicketKey, zendeskMappingQueryRepository::findGrispiUserId) }
            }

        println("ticket comment import process is started for ${groupedComments.count()} tickets at: ${LocalDateTime.now()}")
        for (commentRequests in groupedComments.entries) {
            try {
                grispiApi.createComments(commentRequests.value, grispiApiCredentials)

                zendeskLogRepository.save(ImportLog(null, LogType.SUCCESS, RESOURCE_NAME, "comments for ticket with id:{${commentRequests.key}} created successfully", operationId))
            } catch (exception: RuntimeException) {
                when (exception) {
                    is GrispiApiException -> {
                        zendeskLogRepository.save(
                            ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                "comments for ticket with id: {${commentRequests.key}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                                operationId))
                    }
                    else -> {
                        zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                            "comments for ticket with id: {${commentRequests.key}} couldn't be imported. ${exception.message}",
                            operationId))
                    }
                }
            }
        }
    }

}
