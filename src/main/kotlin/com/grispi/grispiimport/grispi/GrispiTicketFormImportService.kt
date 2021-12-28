package com.grispi.grispiimport.grispi

import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ticketform.ZendeskTicketFormRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GrispiTicketFormImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskMappingQueryRepository: ZendeskMappingQueryRepository,
    @Autowired val zendeskLogRepository: ZendeskLogRepository,
    @Autowired val zendeskMappingRepository: ZendeskMappingRepository,
    @Autowired val zendeskTicketFormRepository: ZendeskTicketFormRepository,
) {

    companion object {
        const val RESOURCE_NAME = "ticket_form"
        const val PAGE_SIZE = 1000
    }

    fun import(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        var ticketForms = zendeskTicketFormRepository.findAllByOperationId(operationId, Pageable.ofSize(PAGE_SIZE))

        println("ticket form import process is started for ${ticketForms.totalElements} items at: ${LocalDateTime.now()}")

        do {
            println("fetching ${ticketForms.pageable.pageNumber}. page")
            for (ticketForm in ticketForms.content) {
                try {
                    val ticketFormResponse = grispiApi.createTicketForm(ticketForm.toGrispiTicketFormRequest(zendeskMappingQueryRepository::findGrispiTicketFieldKey), grispiApiCredentials)

                    zendeskMappingRepository.save(ZendeskMapping(null, ticketForm.id, ticketFormResponse.bodyText(), RESOURCE_NAME, operationId))

                    zendeskLogRepository.save(ImportLog(null, LogType.SUCCESS, RESOURCE_NAME, "{${ticketForm.id}} created successfully", operationId))
                } catch (exception: RuntimeException) {
                    when (exception) {
                        is GrispiApiException -> {
                            zendeskLogRepository.save(
                                ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                    "{${ticketForm.name}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                                    operationId))
                        }
                        else -> {
                            zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                "{${ticketForm.name} with id: ${ticketForm.id}} couldn't be imported. ${exception.message}",
                                operationId))
                        }
                    }
                }
            }

            if (ticketForms.hasNext()) {
                ticketForms = zendeskTicketFormRepository.findAllByOperationId(operationId, ticketForms.nextPageable())
            }
        } while (ticketForms.hasNext())
    }

}
