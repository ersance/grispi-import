package com.grispi.grispiimport.grispi

import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.user.ZendeskUserRepository
import jodd.json.JsonParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GrispiUserImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskMappingRepository: ZendeskMappingRepository,
    @Autowired val zendeskLogRepository: ZendeskLogRepository,
    @Autowired val zendeskUserRepository: ZendeskUserRepository
) {

    companion object {
        const val RESOURCE_NAME = "user"
        const val PAGE_SIZE = 1000
    }

    fun import(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        var users = zendeskUserRepository.findAllByOperationId(operationId, Pageable.ofSize(PAGE_SIZE))

        println("user import process is started for ${users.totalElements} users at: ${LocalDateTime.now()}")

        do {
            println("fetching ${users.pageable.pageNumber}. page")
            for (user in users.content) {
                try {
                    val createUserResponse = grispiApi.createUser(user.toGrispiUserRequest(), grispiApiCredentials)

                    zendeskMappingRepository.save(ZendeskMapping(null, user.id, createUserResponse.bodyText(), RESOURCE_NAME, operationId))

                    zendeskLogRepository.save(ImportLog(null, LogType.SUCCESS, RESOURCE_NAME, "{${user.name}} created successfully", operationId))
                } catch (exception: RuntimeException) {
                    when (exception) {
                        is GrispiApiException -> {
                            zendeskLogRepository.save(
                                ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                    "{${user.name} with id: ${user.id}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                                    operationId))
                        }
                        // TODO: 15.12.2021 handle conflicted users
                        else -> {
                            zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                "{${user.name} with id: ${user.id}} couldn't be imported. ${exception.message}",
                                operationId))
                        }
                    }
                }
            }

            if (users.hasNext()) {
                users = zendeskUserRepository.findAllByOperationId(operationId, users.nextPageable())
            }
        } while (users.hasNext())
    }

}
