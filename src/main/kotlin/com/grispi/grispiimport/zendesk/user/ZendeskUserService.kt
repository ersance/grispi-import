package com.grispi.grispiimport.zendesk.user

import com.grispi.grispiimport.utils.CalculateTimeSpent
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ZendeskApi.Companion.PAGE_SIZE
import com.grispi.grispiimport.zendesk.organization.ResourceCount
import com.grispi.grispiimport.zendesk.organization.ZendeskOrganizationService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.math.log

// TODO: 25.12.2021 handle api errors
@Service
class ZendeskUserService(
    private val zendeskApi: ZendeskApi,
    private val zendeskUserRepository: ZendeskUserRepository,
    private val apiLimitWatcher: ApiLimitWatcher
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val RESOURCE_NAME = "user"
        const val DELETED_RESOURCE_NAME = "deleted_user"
    }

    @CalculateTimeSpent
    fun fetch(operationId: String, zendeskApiCredentials: ZendeskApiCredentials, startingFrom: Int? = 1) {
        fetchDeletedUsers(operationId, zendeskApiCredentials)
        fetchUsers(operationId, zendeskApiCredentials)
    }

    private fun fetchUsers(operationId: String, zendeskApiCredentials: ZendeskApiCredentials, startingFrom: Int? = 1) {
        val userCount = zendeskApi.getUserCount(zendeskApiCredentials)

        val to = BigDecimal(userCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt()
        for (index in (startingFrom)!!.rangeTo(to)) {
            logger.info("fetching ${index}. page for users")

            if (apiLimitWatcher.isApiUnavailable(operationId)) {
                val retryAfterFor = apiLimitWatcher.getRetryAfterFor(operationId)
                logger.info("sleeping user thread for ${retryAfterFor} page ${index}")
                CompletableFuture.supplyAsync(
                    { fetchUsers(operationId, zendeskApiCredentials, index) },
                    CompletableFuture.delayedExecutor(retryAfterFor, TimeUnit.SECONDS)
                )

                break;
            }

            zendeskApi
                .getUsers(zendeskApiCredentials, ZendeskPageParams(index, PAGE_SIZE), this::save)
                .thenApply { users -> save(users, operationId) }
                .thenRun { logger.info("users imported for page: ${index}") }
        }
    }

    private fun fetchDeletedUsers(operationId: String, zendeskApiCredentials: ZendeskApiCredentials, startingFrom: Int? = 1) {
        val deletedUserCount = zendeskApi.getDeletedUserCount(zendeskApiCredentials)

        val to = BigDecimal(deletedUserCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt()
        for (index in (startingFrom)!!.rangeTo(to)) {
            logger.info("fetching ${index}. page for users")

            if (apiLimitWatcher.isApiUnavailable(operationId)) {
                val retryAfterFor = apiLimitWatcher.getRetryAfterFor(operationId)
                logger.info("sleeping user thread for ${retryAfterFor} page ${index}")
                CompletableFuture.supplyAsync(
                    { fetchDeletedUsers(operationId, zendeskApiCredentials, index) },
                    CompletableFuture.delayedExecutor(retryAfterFor, TimeUnit.SECONDS)
                )

                break;
            }

            zendeskApi
                .getDeletedUsers(zendeskApiCredentials, ZendeskPageParams(index, PAGE_SIZE), this::save)
                .thenApply { users -> save(users, operationId) }
                .thenRun { logger.info("deleted users imported for page: ${index}") }
        }
    }

    private fun save(users: List<ZendeskUser>, operationId: String): List<ZendeskUser> {
        users.forEach { it.operationId = operationId }

        return zendeskUserRepository.saveAll(users)
    }

    fun fetchedDeletedUsersCount(operationId: String): Long {
        return zendeskUserRepository.countAllByOperationIdAndActiveFalse(operationId)
    }

    fun fetchedUsersCount(operationId: String): Long {
        return zendeskUserRepository.countAllByOperationIdAndActiveTrue(operationId)
    }

    fun deletedUsersCount(operationId: String, zendeskApiCredentials: ZendeskApiCredentials): ResourceCount {
        return CompletableFuture
            .supplyAsync { zendeskApi.getDeletedUserCount(zendeskApiCredentials) }
            .thenCombine(
                CompletableFuture.supplyAsync { fetchedDeletedUsersCount(operationId) },
                { zCount, fCount -> ResourceCount(DELETED_RESOURCE_NAME, zCount, fCount) })
            .get()
    }

    fun usersCount(operationId: String, zendeskApiCredentials: ZendeskApiCredentials): ResourceCount {
        return CompletableFuture
            .supplyAsync { zendeskApi.getUserCount(zendeskApiCredentials) }
            .thenCombine(
                CompletableFuture.supplyAsync { fetchedUsersCount(operationId) },
                { zCount, fCount -> ResourceCount(RESOURCE_NAME, zCount, fCount) })
            .get()
    }
}

@Repository
interface ZendeskUserRepository: MongoRepository<ZendeskUser, Long> {
    fun findAllByOperationIdAndActiveTrue(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskUser>
    fun findAllByOperationIdAndActiveFalse(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskUser>
    fun countAllByOperationIdAndActiveTrue(@Param("operationId") operationId: String): Long
    fun countAllByOperationIdAndActiveFalse(@Param("operationId") operationId: String): Long
}

