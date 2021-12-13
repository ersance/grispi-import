package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.common.ImportLogDao
import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiApiException
import com.grispi.grispiimport.grispi.GrispiTicketFieldRequest
import com.grispi.grispiimport.grispi.Group
import jodd.json.JsonParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.stream.Collectors

@Service
class TicketFieldImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskApi: ZendeskApi,
    @Autowired val zendeskMappingDao: ZendeskMappingDao,
    @Autowired val importLogDao: ImportLogDao
) {

    companion object {
        const val RESOURCE_NAME = "ticket field"
    }

    fun import(operationId: String, zendeskImportRequest: ZendeskImportRequest) {
        val zendeskTicketFields = zendeskApi.getTicketFields(zendeskImportRequest.zendeskApiCredentials)

        val filteredTickets = zendeskTicketFields.stream()
            .filter { ticketField -> !ZendeskTicketField.SYSTEM_FIELDS.contains(ticketField.type) }
            ?.collect(Collectors.toList())

        zendeskMappingDao.infoLog(operationId, RESOURCE_NAME, "${filteredTickets?.count()} ticket fields found", null)
        println("ticket field import process is started for ${filteredTickets?.count()} items at: ${LocalDateTime.now()}")

        if (filteredTickets != null) {
            for (zendeskTicketField in filteredTickets) {
                try {
                    val createCustomFieldResponse = grispiApi.createCustomField(
                        zendeskTicketField.toGrispiTicketFieldRequest(),
                        zendeskImportRequest.grispiApiCredentials
                    )

                    zendeskMappingDao.addCustomFieldMapping(operationId, zendeskTicketField.id, createCustomFieldResponse.bodyText().toLong())

                    zendeskMappingDao.successLog(operationId, RESOURCE_NAME, "{${zendeskTicketField.title}} created successfully", null)
                } catch (exception: RuntimeException) {
                    when (exception) {
                        is GrispiApiException -> {
                            zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
                                "{${zendeskTicketField.title}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                                null)
                        }
                        else -> {
                            zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
                                "{${zendeskTicketField.title} couldn't be imported. ${exception.message}",
                                null)
                        }
                    }
                }
            }

            // create zendesk id custom field
            try {
                val createCustomFieldResponse = grispiApi.createCustomField(
                    GrispiTicketFieldRequest.Builder().buildZendeskIdCustomField(),
                    zendeskImportRequest.grispiApiCredentials
                )

                zendeskMappingDao.successLog(operationId, RESOURCE_NAME, "zendesk id custom field created successfully", null)
            } catch (exception: GrispiApiException) {
                zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
                    "zendesk id custom field couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                    null)
            }

            // create zendesk brand id custom field
            try {
                val createCustomFieldResponse = grispiApi.createCustomField(
                    GrispiTicketFieldRequest.Builder().buildZendeskBrandIdCustomField(),
                    zendeskImportRequest.grispiApiCredentials
                )

                zendeskMappingDao.successLog(operationId, RESOURCE_NAME, "zendesk id custom field created successfully", null)
            } catch (exception: GrispiApiException) {
                zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
                    "zendesk id custom field couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                    null)
            }
        }

        println("ticket fields import process is done")
    }

}
