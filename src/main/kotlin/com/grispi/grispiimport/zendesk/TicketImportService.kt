package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.common.ImportLogDao
import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiApiException
import jodd.json.JsonParser
import jodd.json.ValueConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.time.Instant

@Service
class TicketImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskApi: ZendeskApi,
    @Autowired val zendeskMappingDao: ZendeskMappingDao,
    @Autowired val importLogDao: ImportLogDao,
    @Autowired val zendeskDateConverter: ZendeskDateConverter
) {

    companion object {
        const val RESOURCE_NAME = "ticket"
    }

    fun import(zendeskImportRequest: ZendeskImportRequest) {
        val tickets = zendeskApi.getTickets(zendeskImportRequest.zendeskApiCredentials)

        val zendeskTickets = JsonParser().withValueConverter("tickets.values.createdAt", zendeskDateConverter).parse(tickets.bodyText(), ZendeskTickets::class.java)

        importLogDao.infoLog(RESOURCE_NAME, "${zendeskTickets.tickets.count()} ticket found", null)

        for (zendeskTicket in zendeskTickets.tickets) {
            try {
                val createTicketResponse = grispiApi.createTicket(
                    zendeskTicket.toTicketRequest(zendeskMappingDao::getUserId, zendeskMappingDao::getGroupId),
                    zendeskImportRequest.grispiApiCredentials
                )

                importLogDao.successLog(RESOURCE_NAME, "{${zendeskTicket.subject}} created successfully", null)
            } catch (exception: RuntimeException) {
                when (exception) {
                    is GrispiApiException -> {
                        importLogDao.errorLog(RESOURCE_NAME,
                            "{${zendeskTicket.subject} with id: ${zendeskTicket.id}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                            null)
                    }
                    is GrispiReferenceNotFoundException -> {
                        importLogDao.errorLog(RESOURCE_NAME,
                            "{${zendeskTicket.subject} with id: ${zendeskTicket.id}} couldn't be imported. ${exception.message()}",
                            null)
                    }
                    else -> {
                        importLogDao.errorLog(RESOURCE_NAME,
                            "{${zendeskTicket.subject} with id: ${zendeskTicket.id}} couldn't be imported. ${exception.message}",
                            null)
                    }
                }
            }
        }
    }


}
