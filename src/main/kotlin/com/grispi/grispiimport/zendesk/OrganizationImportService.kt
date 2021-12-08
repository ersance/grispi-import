package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.common.ImportLogDao
import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiApiException
import jodd.json.JsonParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import org.springframework.web.context.WebApplicationContext

@Service
class OrganizationImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskApi: ZendeskApi,
    @Autowired val zendeskMappingDao: ZendeskMappingDao,
    @Autowired val importLogDao: ImportLogDao
) {

    companion object {
        const val RESOURCE_NAME = "organization"
    }

    fun import(operationId: String, zendeskImportRequest: ZendeskImportRequest) {
        val organizations = zendeskApi.getOrganizations(zendeskImportRequest.zendeskApiCredentials)
        val zendeskOrganizations = JsonParser().parse(organizations.bodyRaw(), ZendeskOrganizations::class.java)

        zendeskMappingDao.infoLog(operationId, RESOURCE_NAME, "${zendeskOrganizations.organizations.count()} organizations found", null)
        println("organization import process is started for ${zendeskOrganizations.organizations} items")

        for (zendeskOrganization in zendeskOrganizations.organizations) {
            try {
                val createOrganizationResponse = grispiApi.createOrganization(
                    zendeskOrganization.toGrispiOrganizationRequest(),
                    zendeskImportRequest.grispiApiCredentials
                )
                val createdOrganizationId = JsonParser().parse(createOrganizationResponse.bodyRaw(), Long::class.java)
                zendeskMappingDao.addOrganizationMapping(operationId, zendeskOrganization.id, createdOrganizationId)

                zendeskMappingDao.successLog(operationId, RESOURCE_NAME, "{${zendeskOrganization.name}} created successfully", null)
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
        println("organization import process is done")
    }

}