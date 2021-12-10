package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiApiException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskApi: ZendeskApi,
    @Autowired val zendeskMappingDao: ZendeskMappingDao
) {

    companion object {
        const val RESOURCE_NAME = "user"
    }

    fun import(operationId: String, zendeskImportRequest: ZendeskImportRequest) {
        val zendeskUsers = zendeskApi.getUsers(zendeskImportRequest.zendeskApiCredentials)

        zendeskMappingDao.infoLog(operationId, RESOURCE_NAME, "${zendeskUsers.count()} users found", null)
        println("user import process is started for ${zendeskUsers.count()} users")

        for (zendeskUser in zendeskUsers) {
            try {
                val createUserResponse = grispiApi.createUser(
                    zendeskUser.toGrispiUserRequest(),
                    zendeskImportRequest.grispiApiCredentials
                )

                zendeskMappingDao.addUserMapping(operationId, zendeskUser.id, createUserResponse.bodyText().toLong())

                zendeskMappingDao.successLog(operationId, RESOURCE_NAME, "{${zendeskUser.name}} created successfully", null)
            } catch (exception: RuntimeException) {
                when (exception) {
                    is GrispiApiException -> {
                        zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
                            "{${zendeskUser.name} with id: ${zendeskUser.id}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                            null)
                    }
                    else -> {
                        zendeskMappingDao.errorLog(operationId, TicketImportService.RESOURCE_NAME,
                            "{${zendeskUser.name} with id: ${zendeskUser.id}} couldn't be imported. ${exception.message}",
                            null)
                    }
                }
            }
        }

        println("user import process is done")
    }

}
