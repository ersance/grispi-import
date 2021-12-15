package com.grispi.grispiimport.grispi

import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.group.ZendeskGroupRepository
import jodd.json.JsonParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GrispiGroupImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskMappingRepository: ZendeskMappingRepository,
    @Autowired val zendeskLogRepository: ZendeskLogRepository,
    @Autowired val zendeskGroupRepository: ZendeskGroupRepository,
) {

    companion object {
        const val RESOURCE_NAME = "group"
        const val PAGE_SIZE = 1000
    }

    fun import(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        var groups = zendeskGroupRepository.findAllByOperationId(operationId, Pageable.ofSize(PAGE_SIZE))

        println("group import process is started for ${groups.totalElements} groups at: ${LocalDateTime.now()}")

        do {
            println("fetching ${groups.pageable.pageNumber}. page")
            for (group in groups.content) {
                try {
                    val createGroupResponse = grispiApi.createGroup(group.toGrispiGroupRequest(), grispiApiCredentials)

                    zendeskMappingRepository.save(ZendeskMapping(null, group.id, createGroupResponse.bodyText(), RESOURCE_NAME, operationId))

                    zendeskLogRepository.save(ImportLog(null, LogType.SUCCESS, RESOURCE_NAME, "{${group.name}} created successfully", operationId))
                } catch (exception: RuntimeException) {
                    when (exception) {
                        is GrispiApiException -> {
                            zendeskLogRepository.save(
                                ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                    "{${group.name} with id: ${group.id}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                                    operationId))
                        }
                        else -> {
                            zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                "{${group.name} with id: ${group.id}} couldn't be imported. ${exception.message}",
                                operationId))
                        }
                    }
                }
            }

            if (groups.hasNext()) {
                groups = zendeskGroupRepository.findAllByOperationId(operationId, groups.nextPageable())
            }
        } while (groups.hasNext())
    }

}
