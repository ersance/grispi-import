package com.grispi.grispiimport.grispi

import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.user.ZendeskUserAggregationRepository
import com.grispi.grispiimport.zendesk.user.ZendeskUserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@Service
class GrispiUserImportService(
    private val grispiApi: GrispiApi,
    private val zendeskMappingRepository: ZendeskMappingRepository,
    private val zendeskMappingQueryRepository: ZendeskMappingQueryRepository,
    private val zendeskLogRepository: ZendeskLogRepository,
    private val zendeskUserRepository: ZendeskUserRepository,
    private val zendeskUserAggregationRepository: ZendeskUserAggregationRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val RESOURCE_NAME = "user"
        const val DELETED_USER_NAME = "deleted_user"
        const val PAGE_SIZE = 1000
    }

    fun import(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        importUsers(operationId, grispiApiCredentials)
        importDeletedUsers(operationId, grispiApiCredentials)
    }

    fun importUsers(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        val activeUsersAggregated = zendeskUserAggregationRepository.findActiveUsers(operationId)

        logger.info("user import process is started for ${activeUsersAggregated.count()} users at: ${LocalDateTime.now()}")

        val asyncUserRequests: MutableList<CompletableFuture<ImportLog>> = mutableListOf()

        for (userAggr in activeUsersAggregated) {
            val grispiUserRequest = try {
                userAggr.user.toGrispiUserRequest(userAggr.grispiGroupIds, userAggr.grispiOrganizationId)
            }
            catch (exception: RuntimeException) {
                if (exception is GrispiReferenceNotFoundException) {
                    zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                        "{${userAggr.user.name} with id: ${userAggr.user.id}} couldn't be imported. ${exception.printMessage()}",
                        operationId))
                }
                else {
                    zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                        "{${userAggr.user.name} with id: ${userAggr.user.id}} couldn't be imported. ${exception.message}",
                        operationId))
                }

                continue
            }

            val userRequest = grispiApi
                .createUser(grispiUserRequest, grispiApiCredentials)
                .thenApply { userId ->
                    zendeskMappingRepository.save(ZendeskMapping(null, userAggr.user.id, userId, RESOURCE_NAME, operationId))
                    zendeskLogRepository.save(ImportLog(null,
                        LogType.SUCCESS,
                        RESOURCE_NAME,
                        "{${userAggr.user.name}} created successfully",
                        operationId))
                }
                .exceptionally { exception ->
                    when (exception.cause) {
                        is GrispiApiException -> {
                            val grispiApiException = exception.cause as GrispiApiException
                            zendeskLogRepository.save(
                                ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                    "{${userAggr.user.name} with id: ${userAggr.user.id}} couldn't be imported. status code: ${grispiApiException.statusCode} message: ${grispiApiException.exceptionMessage}",
                                    operationId))
                        }
                        is GrispiUserConflictedException -> {
                            val conflictedException = exception.cause as GrispiUserConflictedException
                            zendeskMappingRepository.save(ZendeskMapping(null, userAggr.user.id, conflictedException.conflictedUserId.toString(), RESOURCE_NAME, operationId))
                            zendeskLogRepository.save(
                                ImportLog(null, LogType.WARNING, RESOURCE_NAME,
                                    "{${userAggr.user.name} with id: ${userAggr.user.id}} is already created. mapping user with id: {${conflictedException.conflictedUserId.toString()}}",
                                    operationId))
                        }
                        else -> {
                            zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                "{${userAggr.user.name} with id: ${userAggr.user.id}} couldn't be imported. ${exception.message}",
                                operationId))
                        }
                    }
                }

            asyncUserRequests.add(userRequest)
        }

        CompletableFuture.allOf(*asyncUserRequests.toTypedArray()).get(1, TimeUnit.DAYS)

        logger.info("user import process has ended for ${activeUsersAggregated.count()} users at: ${LocalDateTime.now()}")
    }

    fun importDeletedUsers(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        val deletedUsersAggregated = zendeskUserAggregationRepository.findDeletedUsers(operationId)

        logger.info("deleted user import process is started for ${deletedUsersAggregated.count()} users at: ${LocalDateTime.now()}")

        val asyncUserRequests: MutableList<CompletableFuture<ImportLog>> = mutableListOf()

        for (userAggr in deletedUsersAggregated) {
            val grispiUserRequest = try {
                userAggr.user.toGrispiUserRequest(userAggr.grispiGroupIds, userAggr.grispiOrganizationId)
            }
            catch (exception: RuntimeException) {
                if (exception is GrispiReferenceNotFoundException) {
                    zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                        "{${userAggr.user.name} with id: ${userAggr.user.id}} couldn't be imported. ${exception.printMessage()}",
                        operationId))
                }
                else {
                    zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                        "{${userAggr.user.name} with id: ${userAggr.user.id}} couldn't be imported. ${exception.message}",
                        operationId))
                }

                continue
            }

            val deletedUserRequest = grispiApi
                .createDeletedUser(grispiUserRequest, grispiApiCredentials)
                .thenApply { userId ->
                    zendeskMappingRepository.save(ZendeskMapping(null, userAggr.user.id, userId, RESOURCE_NAME, operationId))
                    zendeskLogRepository.save(ImportLog(null,
                        LogType.SUCCESS,
                        DELETED_USER_NAME,
                        "{${userAggr.user.name}} created successfully",
                        operationId))
                }
                .exceptionally { exception ->
                    when (exception.cause) {
                        is GrispiApiException -> {
                            val grispiApiException = exception.cause as GrispiApiException
                            zendeskLogRepository.save(
                                ImportLog(null, LogType.ERROR, DELETED_USER_NAME,
                                    "{${userAggr.user.name} with id: ${userAggr.user.id}} couldn't be imported. status code: ${grispiApiException.statusCode} message: ${grispiApiException.exceptionMessage}",
                                    operationId))
                        }
                        is GrispiUserConflictedException -> {
                            val conflictedException = exception.cause as GrispiUserConflictedException
                            zendeskMappingRepository.save(ZendeskMapping(null, userAggr.user.id, conflictedException.conflictedUserId.toString(), RESOURCE_NAME, operationId))
                            zendeskLogRepository.save(
                                ImportLog(null, LogType.WARNING, DELETED_USER_NAME,
                                    "{${userAggr.user.name} with id: ${userAggr.user.id}} is already created. mapping user with id: {${conflictedException.conflictedUserId.toString()}}",
                                    operationId))
                        }
                        else -> {
                            zendeskLogRepository.save(ImportLog(null, LogType.ERROR, DELETED_USER_NAME,
                                "{${userAggr.user.name} with id: ${userAggr.user.id}} couldn't be imported. ${exception.message}",
                                operationId))
                        }
                    }
                }

            asyncUserRequests.add(deletedUserRequest)
        }

        CompletableFuture.allOf(*asyncUserRequests.toTypedArray()).get(1, TimeUnit.DAYS)

        logger.info("deleted user import process has ended for ${deletedUsersAggregated.count()} users at: ${LocalDateTime.now()}")
    }

}