package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiApiException
import com.grispi.grispiimport.zendesk.ZendeskApi.Companion.PAGE_SIZE
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

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
        val userCount = zendeskApi.getUserCount(zendeskImportRequest.zendeskApiCredentials)

        zendeskMappingDao.infoLog(operationId, RESOURCE_NAME, "${userCount} users found", null)
        println("user import process is started for ${userCount} items at: ${LocalDateTime.now()}")

        val combinedTickets: MutableList<CompletableFuture<Unit>> = mutableListOf()
        for (index in 1..(BigDecimal(userCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt())) {
            println("fetching ${index}. page for user")

            val thenApplyAsync = zendeskApi
                .getUsers(zendeskImportRequest.zendeskApiCredentials, ZendeskPageParams(index, PAGE_SIZE))
                .thenApplyAsync { users -> import(users, zendeskImportRequest, operationId) }

            combinedTickets.add(thenApplyAsync)
        }

        CompletableFuture.allOf(*combinedTickets.toTypedArray()).get(1, TimeUnit.DAYS)

        println("user import process is done")
    }

    private fun import(zendeskUsers: List<ZendeskUser>, zendeskImportRequest: ZendeskImportRequest, operationId: String) {
        for (zendeskUser in zendeskUsers) {
                try {
                    val createUserResponse = grispiApi.createUser(
                        zendeskUser.toGrispiUserRequest(),
                        zendeskImportRequest.grispiApiCredentials
                    )

                    zendeskMappingDao.addUserMapping(operationId,
                        zendeskUser.id,
                        createUserResponse.bodyText().toLong())

                    zendeskMappingDao.successLog(operationId,
                        RESOURCE_NAME,
                        "{${zendeskUser.name}} created successfully",
                        null)
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
    }

}
