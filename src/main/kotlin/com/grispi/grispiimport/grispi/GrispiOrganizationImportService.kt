package com.grispi.grispiimport.grispi

import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.organization.ZendeskOrganizationRepository
import jodd.json.JsonParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GrispiOrganizationImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskMappingRepository: ZendeskMappingRepository,
    @Autowired val zendeskLogRepository: ZendeskLogRepository,
    @Autowired val zendeskOrganizationRepository: ZendeskOrganizationRepository
) {

    companion object {
        const val RESOURCE_NAME = "organization"
        const val PAGE_SIZE = 1
    }

    fun import(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        var organizations = zendeskOrganizationRepository.findAllByOperationId(operationId, Pageable.ofSize(PAGE_SIZE))

        println("organization import process is started for ${organizations.totalElements} tickets at: ${LocalDateTime.now()}")

        do {
            println("fetching ${organizations.pageable.pageNumber}. page")
            for (organization in organizations.content) {
                try {
                    val createOrganizationResponse = grispiApi.createOrganization(organization.toGrispiOrganizationRequest(), grispiApiCredentials)

                    zendeskMappingRepository.save(ZendeskMapping(null, organization.id, createOrganizationResponse.bodyText(), RESOURCE_NAME, operationId))

                    zendeskLogRepository.save(ImportLog(null, LogType.SUCCESS, RESOURCE_NAME, "{${organization.name}} created successfully", operationId))
                } catch (exception: RuntimeException) {
                    when (exception) {
                        is GrispiApiException -> {
                            zendeskLogRepository.save(
                                ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                    "{${organization.name} with id: ${organization.id}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                                    operationId))
                        }
                        else -> {
                            zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                "{${organization.name} with id: ${organization.id}} couldn't be imported. ${exception.message}",
                                operationId))
                        }
                    }
                }
            }

            if (organizations.hasNext()) {
                organizations = zendeskOrganizationRepository.findAllByOperationId(operationId, organizations.nextPageable())
            }
        } while (organizations.hasNext())
    }

}