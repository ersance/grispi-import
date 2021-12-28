package com.grispi.grispiimport.grispi

import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketCommentRepository
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.stream.Collectors

@Service
class GrispiTicketCommentImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskMappingQueryRepository: ZendeskMappingQueryRepository,
    @Autowired val zendeskLogRepository: ZendeskLogRepository,
    @Autowired val zendeskTicketCommentRepository: ZendeskTicketCommentRepository,
    @Autowired val zendeskTicketRepository: ZendeskTicketRepository,
) {

    companion object {
        const val RESOURCE_NAME = "comment"
        const val PAGE_SIZE = 1000
    }

    fun import(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        val commentedTicketIdsForBrand = zendeskTicketRepository.findCommentedTicketIdsForBrand(operationId, GrispiTicketImportService.YUVANIKUR_BRAND_ID)

        val groupedComments = zendeskTicketCommentRepository
            .findAllByOperationIdAndTicketIdIsIn(operationId, commentedTicketIdsForBrand.stream().map { it.id }.toList())
            .groupBy { it.ticketId }

        println("missing ids: ${commentedTicketIdsForBrand.stream().filter { !groupedComments.keys.contains(it.id) }.toList()}")

        println("ticket comment import process is started for ${groupedComments.count()} tickets at: ${LocalDateTime.now()}")
        for (commentRequest in groupedComments.entries) {
            println("comments for ${commentRequest.key} ")
            try {
                val comments = commentRequest.value.stream()
                    .map { it.toCommentRequest(zendeskMappingQueryRepository::findGrispiTicketKey, zendeskMappingQueryRepository::findGrispiUserId) }
                    .toList()

                grispiApi.createComments(comments, grispiApiCredentials)

                zendeskLogRepository.save(ImportLog(null, LogType.SUCCESS, RESOURCE_NAME, "comments for ticket with id:{${commentRequest.key}} created successfully", operationId))
            } catch (exception: RuntimeException) {
                when (exception) {
                    is GrispiApiException -> {
                        zendeskLogRepository.save(
                            ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                "comments for ticket with id: {${commentRequest.key}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                                operationId))
                    }
                    else -> {
                        zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                            "comments for ticket with id: {${commentRequest.key}} couldn't be imported. ${exception.message}",
                            operationId))
                    }
                }
            }
        }
    }

}
