package com.grispi.grispiimport.grispi

import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ticketfield.ZendeskTicketFieldRepository
import jodd.json.JsonParser
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GrispiTicketFieldImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskMappingRepository: ZendeskMappingRepository,
    @Autowired val zendeskLogRepository: ZendeskLogRepository,
    @Autowired val zendeskTicketFieldRepository: ZendeskTicketFieldRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val RESOURCE_NAME = "ticket_field"
        const val PAGE_SIZE = 1000
    }

    fun import(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        var ticketFields = zendeskTicketFieldRepository.findAllByOperationId(operationId, Pageable.ofSize(PAGE_SIZE))

        logger.info("ticket field import process is started for ${ticketFields.totalElements} tickets at: ${LocalDateTime.now()}")

        do {
            for (ticketField in ticketFields.content) {
                try {
                    val createTicketFieldResponse = grispiApi.createTicketField(ticketField.toGrispiTicketFieldRequest(), grispiApiCredentials)

                    zendeskMappingRepository.save(ZendeskMapping(null, ticketField.id, createTicketFieldResponse.bodyText(), RESOURCE_NAME, operationId))

                    zendeskLogRepository.save(ImportLog(null, LogType.SUCCESS, RESOURCE_NAME, "{${ticketField.id}} created successfully", operationId))
                } catch (exception: RuntimeException) {
                    when (exception) {
                        is GrispiApiException -> {
                            zendeskLogRepository.save(
                                ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                    "{${ticketField.title}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                                    operationId))
                        }
                        else -> {
                            zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                "{${ticketField.title} with id: ${ticketField.id}} couldn't be imported. ${exception.message}",
                                operationId))
                        }
                    }
                }
            }

            if (ticketFields.hasNext()) {
                ticketFields = zendeskTicketFieldRepository.findAllByOperationId(operationId, ticketFields.nextPageable())
            }
        } while (ticketFields.hasNext())

        // create zendesk id custom field
        try {
            grispiApi.createTicketField(GrispiTicketFieldRequest.Builder().buildZendeskIdCustomField(), grispiApiCredentials)

            zendeskLogRepository.save(ImportLog(null, LogType.SUCCESS, RESOURCE_NAME, "zendesk id custom field created successfully", operationId))
        } catch (exception: RuntimeException) {
            zendeskLogRepository.save(
                ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                    "zendesk id custom field couldn't be imported. message: ${exception.message}",
                    operationId))
        }

        // create zendesk brand id custom field
        try {
            grispiApi.createTicketField(GrispiTicketFieldRequest.Builder().buildZendeskBrandIdCustomField(), grispiApiCredentials)

            zendeskLogRepository.save(ImportLog(null, LogType.SUCCESS, RESOURCE_NAME, "zendesk brand id custom field created successfully", operationId))
        } catch (exception: RuntimeException) {
            zendeskLogRepository.save(
                ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                    "zendesk brand id custom field couldn't be imported. message: ${exception.message}",
                    operationId))
        }

        logger.info("ticket field import process has ended for ${ticketFields.totalElements} ticket fields at: ${LocalDateTime.now()}")
    }

}