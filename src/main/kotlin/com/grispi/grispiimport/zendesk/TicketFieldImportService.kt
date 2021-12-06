package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.common.ImportLogDao
import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiApiException
import com.grispi.grispiimport.grispi.Group
import jodd.json.JsonParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
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

    fun import(zendeskImportRequest: ZendeskImportRequest) {
        val ticketFields = zendeskApi.getTicketFields(zendeskImportRequest.zendeskApiCredentials)
        val zendeskTicketFields = JsonParser().parse(ticketFields.bodyRaw(), ZendeskTicketFields::class.java)

        val filteredTickets = zendeskTicketFields.ticketFields.stream()
            .filter { ticketField -> !ZendeskTicketField.SYSTEM_FIELDS.contains(ticketField.type) }
            ?.collect(Collectors.toList())

        importLogDao.infoLog(RESOURCE_NAME, "${filteredTickets?.count()} ticket fields found", null)

        if (filteredTickets != null) {
            for (zendeskTicketField in filteredTickets) {
                try {
                    val createCustomFieldResponse = grispiApi.createCustomField(
                        zendeskTicketField.toGrispiTicketFieldRequest(),
                        zendeskImportRequest.grispiApiCredentials
                    )
                    val createdCustomFieldId = JsonParser().parse(createCustomFieldResponse.bodyRaw(), Long::class.java)
                    zendeskMappingDao.addCustomFieldMapping(zendeskTicketField.id, createdCustomFieldId)

                    importLogDao.successLog(RESOURCE_NAME, "{${zendeskTicketField.title}} created successfully", null)
                } catch (exception: GrispiApiException) {
                    importLogDao.errorLog(RESOURCE_NAME,
                        "{${zendeskTicketField.title}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                        null)
                }
            }
        }
    }

}
