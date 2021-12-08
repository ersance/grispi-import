package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.common.ImportLogDao
import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiApiException
import jodd.json.JsonParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskApi: ZendeskApi,
    @Autowired val zendeskMappingDao: ZendeskMappingDao,
    @Autowired val importLogDao: ImportLogDao
) {

    companion object {
        const val RESOURCE_NAME = "user"
    }

    fun import(operationId: String, zendeskImportRequest: ZendeskImportRequest) {
        val users = zendeskApi.getUsers(zendeskImportRequest.zendeskApiCredentials)
        val zendeskUsers = JsonParser().parse(users.bodyRaw(), ZendeskUsers::class.java)

        zendeskMappingDao.infoLog(operationId, RESOURCE_NAME, "${zendeskUsers.users.count()} users found", null)
        println("user import process is started for ${zendeskUsers.users.count()} users")

        for (zendeskUser in zendeskUsers.users) {
            try {
                val createUserResponse = grispiApi.createUser(
                    zendeskUser.toGrispiUserRequest(),
                    zendeskImportRequest.grispiApiCredentials
                )
                val createdUserId = JsonParser().parse(createUserResponse.bodyRaw(), Long::class.java)
                zendeskMappingDao.addUserMapping(operationId, zendeskUser.id, createdUserId)

                zendeskMappingDao.successLog(operationId, RESOURCE_NAME, "{${zendeskUser.name}} created successfully", null)
            } catch (exception: GrispiApiException) {
                zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
                    "{${zendeskUser.name} with id: ${zendeskUser.id}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                    null)
            }
        }

        println("user import process is done")
    }

}
