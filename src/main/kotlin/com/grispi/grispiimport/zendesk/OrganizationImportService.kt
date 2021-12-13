package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiApiException
import com.grispi.grispiimport.zendesk.ZendeskApi.Companion.PAGE_SIZE
import jodd.json.JsonParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class OrganizationImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskApi: ZendeskApi,
    @Autowired val zendeskMappingDao: ZendeskMappingDao
) {

    companion object {
        const val RESOURCE_NAME = "organization"
    }

    fun import(operationId: String, zendeskImportRequest: ZendeskImportRequest) {
        val orgCount = zendeskApi.getOrganizationCount(zendeskImportRequest.zendeskApiCredentials)

        zendeskMappingDao.infoLog(operationId, RESOURCE_NAME, "${orgCount} organizations found", null)
        println("organization import process is started for ${orgCount} items at: ${LocalDateTime.now()}")

        for (index in 1..(BigDecimal(orgCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt())) {
            println("fetching ${index}. page")

            val organizations = zendeskApi.getOrganizations(zendeskImportRequest.zendeskApiCredentials, ZendeskPageParams(index, PAGE_SIZE))

            for (zendeskOrganization in organizations) {
                try {
                    val createOrganizationResponse = grispiApi.createOrganization(
                        zendeskOrganization.toGrispiOrganizationRequest(),
                        zendeskImportRequest.grispiApiCredentials
                    )
                    val createdOrganizationId =
                        JsonParser().parse(createOrganizationResponse.bodyRaw(), Long::class.java)
                    zendeskMappingDao.addOrganizationMapping(operationId, zendeskOrganization.id, createdOrganizationId)

                    zendeskMappingDao.successLog(operationId,
                        RESOURCE_NAME,
                        "{${zendeskOrganization.name}} created successfully",
                        null)
                } catch (exception: RuntimeException) {
                    when (exception) {
                        is GrispiApiException -> {
                            zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
                                "{${zendeskOrganization.name} with id: ${zendeskOrganization.id}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                                null)
                        }
                        else -> {
                            zendeskMappingDao.errorLog(operationId, TicketImportService.RESOURCE_NAME,
                                "{${zendeskOrganization.name} with id: ${zendeskOrganization.id}} couldn't be imported. ${exception.message}",
                                null)
                        }
                    }
                }
            }
        }
        println("organization import process is done")
    }

}