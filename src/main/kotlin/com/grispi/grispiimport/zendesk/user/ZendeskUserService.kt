package com.grispi.grispiimport.zendesk.user

import com.grispi.grispiimport.utils.CalculateTimeSpent
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ZendeskApi.Companion.PAGE_SIZE
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

// TODO: 25.12.2021 handle api errors
@Service
class ZendeskUserService(
    private val zendeskApi: ZendeskApi,
    private val zendeskUserRepository: ZendeskUserRepository,
    private val apiLimitWatcher: ApiLimitWatcher
) {

    companion object {
        const val RESOURCE_NAME = "user"
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
            println("fetching ${index}. page for users")

            if (apiLimitWatcher.isApiUnavailable(operationId)) {
                val retryAfterFor = apiLimitWatcher.getRetryAfterFor(operationId)
                println("sleeping user thread for ${retryAfterFor} page ${index}")
                CompletableFuture.supplyAsync(
                    { fetchUsers(operationId, zendeskApiCredentials, index) },
                    CompletableFuture.delayedExecutor(retryAfterFor, TimeUnit.SECONDS)
                )

                break;
            }

            zendeskApi
                .getUsers(zendeskApiCredentials, ZendeskPageParams(index, PAGE_SIZE), this::save)
                .thenApply { users -> save(users, operationId) }
                .thenRun { println("users imported for page: ${index}") }
        }
    }

    private fun fetchDeletedUsers(operationId: String, zendeskApiCredentials: ZendeskApiCredentials, startingFrom: Int? = 1) {
        val deletedUserCount = zendeskApi.getDeletedUserCount(zendeskApiCredentials)

        val to = BigDecimal(deletedUserCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt()
        for (index in (startingFrom)!!.rangeTo(to)) {
            println("fetching ${index}. page for users")

            if (apiLimitWatcher.isApiUnavailable(operationId)) {
                val retryAfterFor = apiLimitWatcher.getRetryAfterFor(operationId)
                println("sleeping user thread for ${retryAfterFor} page ${index}")
                CompletableFuture.supplyAsync(
                    { fetchDeletedUsers(operationId, zendeskApiCredentials, index) },
                    CompletableFuture.delayedExecutor(retryAfterFor, TimeUnit.SECONDS)
                )

                break;
            }

            zendeskApi
                .getDeletedUsers(zendeskApiCredentials, ZendeskPageParams(index, PAGE_SIZE), this::save)
                .thenApply { users -> save(users, operationId) }
                .thenRun { println("deleted users imported for page: ${index}") }
        }
    }

    private fun save(users: List<ZendeskUser>, operationId: String): List<ZendeskUser> {
        users.forEach { it.operationId = operationId }

        return zendeskUserRepository.saveAll(users)
    }
}
