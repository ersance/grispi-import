package com.grispi.grispiimport.grispi

import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.user.ZendeskUserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@Service
class GrispiUserImportService(
    private val grispiApi: GrispiApi,
    private val zendeskMappingRepository: ZendeskMappingRepository,
    private val zendeskMappingQueryRepository: ZendeskMappingQueryRepository,
    private val zendeskLogRepository: ZendeskLogRepository,
    private val zendeskUserRepository: ZendeskUserRepository
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
        val userCount = zendeskUserRepository.countAllByOperationIdAndActiveTrue(operationId)

        logger.info("user import process is started for ${userCount} users at: ${LocalDateTime.now()}")

        val combinedUsers: MutableList<CompletableFuture<ImportLog>> = mutableListOf()

        val to = BigDecimal(userCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt()
        for (index in 0 until to) {
            var users = zendeskUserRepository.findAllByOperationIdAndActiveTrue(operationId, PageRequest.of(index, PAGE_SIZE))

            logger.info("fetching {${users.pageable.pageNumber}}. page for {${users.content.count()}} users")

            for (user in users.content) {

                val grispiUserRequest = try {
                    user.toGrispiUserRequest(
                        zendeskMappingQueryRepository::findGrispiGroupMemberships,
                        zendeskMappingQueryRepository::findGrispiOrganizationId
                    )
                }
                catch (exception: RuntimeException) {
                    if (exception is GrispiReferenceNotFoundException) {
                        zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                            "{${user.name} with id: ${user.id}} couldn't be imported. ${exception.printMessage()}",
                            operationId))
                    }
                    else {
                        zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                            "{${user.name} with id: ${user.id}} couldn't be imported. ${exception.message}",
                            operationId))
                    }

                    continue
                }

                val userRequest = grispiApi
                    .createUser(grispiUserRequest, grispiApiCredentials)
                    .thenApply { userId ->
                        zendeskMappingRepository.save(ZendeskMapping(null, user.id, userId, RESOURCE_NAME, operationId))
                        zendeskLogRepository.save(ImportLog(null,
                            LogType.SUCCESS,
                            RESOURCE_NAME,
                            "{${user.name}} created successfully",
                            operationId))
                    }
                    .exceptionally { exception ->
                        when (exception.cause) {
                            is GrispiApiException -> {
                                val grispiApiException = exception.cause as GrispiApiException
                                zendeskLogRepository.save(
                                    ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                        "{${user.name} with id: ${user.id}} couldn't be imported. status code: ${grispiApiException.statusCode} message: ${grispiApiException.exceptionMessage}",
                                        operationId))
                            }
                            is GrispiUserConflictedException -> {
                                val conflictedException = exception.cause as GrispiUserConflictedException
                                zendeskMappingRepository.save(ZendeskMapping(null, user.id, conflictedException.conflictedUserId.toString(), RESOURCE_NAME, operationId))
                                zendeskLogRepository.save(
                                    ImportLog(null, LogType.WARNING, RESOURCE_NAME,
                                        "{${user.name} with id: ${user.id}} is already created. mapping user with id: {${conflictedException.conflictedUserId.toString()}}",
                                        operationId))
                            }
                            else -> {
                                zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                    "{${user.name} with id: ${user.id}} couldn't be imported. ${exception.message}",
                                    operationId))
                            }
                        }
                    }

                combinedUsers.add(userRequest)
            }
        }

        CompletableFuture.allOf(*combinedUsers.toTypedArray()).get(1, TimeUnit.DAYS)
    }

    fun importDeletedUsers(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        val deletedUserCount = zendeskUserRepository.countAllByOperationIdAndActiveFalse(operationId)

        logger.info("deleted user import process is started for ${deletedUserCount} users at: ${LocalDateTime.now()}")

        val combinedDeletedUsers: MutableList<CompletableFuture<ImportLog>> = mutableListOf()
        val to = BigDecimal(deletedUserCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt()
        for (index in 0 until to) {
            var users = zendeskUserRepository.findAllByOperationIdAndActiveFalse(operationId, PageRequest.of(index, PAGE_SIZE))
            logger.info("fetching $index. page")

            for (user in users.content) {

                val grispiUserRequest = try {
                    user.toGrispiUserRequest(
                        zendeskMappingQueryRepository::findGrispiGroupMemberships,
                        zendeskMappingQueryRepository::findGrispiOrganizationId
                    )
                }
                catch (exception: RuntimeException) {
                    if (exception is GrispiReferenceNotFoundException) {
                        zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                            "{${user.name} with id: ${user.id}} couldn't be imported. ${exception.printMessage()}",
                            operationId))
                    }
                    else {
                        zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                            "{${user.name} with id: ${user.id}} couldn't be imported. ${exception.message}",
                            operationId))
                    }

                    continue
                }

                val deletedUserRequest = grispiApi
                    .createDeletedUser(grispiUserRequest, grispiApiCredentials)
                    .thenApply { userId ->
                        zendeskMappingRepository.save(ZendeskMapping(null, user.id, userId, RESOURCE_NAME, operationId))
                        zendeskLogRepository.save(ImportLog(null,
                            LogType.SUCCESS,
                            DELETED_USER_NAME,
                            "{${user.name}} created successfully",
                            operationId))
                    }
                    .exceptionally { exception ->
                        when (exception.cause) {
                            is GrispiApiException -> {
                                val grispiApiException = exception.cause as GrispiApiException
                                zendeskLogRepository.save(
                                    ImportLog(null, LogType.ERROR, DELETED_USER_NAME,
                                        "{${user.name} with id: ${user.id}} couldn't be imported. status code: ${grispiApiException.statusCode} message: ${grispiApiException.exceptionMessage}",
                                        operationId))
                            }
                            is GrispiUserConflictedException -> {
                                val conflictedException = exception.cause as GrispiUserConflictedException
                                zendeskMappingRepository.save(ZendeskMapping(null, user.id, conflictedException.conflictedUserId.toString(), RESOURCE_NAME, operationId))
                                zendeskLogRepository.save(
                                    ImportLog(null, LogType.WARNING, DELETED_USER_NAME,
                                        "{${user.name} with id: ${user.id}} is already created. mapping user with id: {${conflictedException.conflictedUserId.toString()}}",
                                        operationId))
                            }
                            else -> {
                                zendeskLogRepository.save(ImportLog(null, LogType.ERROR, DELETED_USER_NAME,
                                    "{${user.name} with id: ${user.id}} couldn't be imported. ${exception.message}",
                                    operationId))
                            }
                        }
                    }

                combinedDeletedUsers.add(deletedUserRequest)
            }
        }

        CompletableFuture.allOf(*combinedDeletedUsers.toTypedArray()).get(1, TimeUnit.DAYS)
        logger.info("deleted user import process has ended for ${deletedUserCount} users at: ${LocalDateTime.now()}")
    }

}