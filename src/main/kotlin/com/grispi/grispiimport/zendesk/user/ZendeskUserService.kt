package com.grispi.grispiimport.zendesk.user

import com.grispi.grispiimport.utils.CalculateTimeSpent
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ZendeskApi.Companion.PAGE_SIZE
import com.grispi.grispiimport.zendesk.organization.ResourceCount
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
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

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val RESOURCE_NAME = "user"
        const val DELETED_RESOURCE_NAME = "deleted_user"
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
                .thenRun {
                    MDC.put("operationId", operationId)
                    logger.info("users fetched for page: ${index}")
                }
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
                .thenRun {
                    MDC.put("operationId", operationId)
                    logger.info("deleted users imported for page: ${index}")
                }
        }
    }

    private fun save(users: List<ZendeskUser>, operationId: String): List<ZendeskUser> {
        users.forEach { it.operationId = operationId }

        return zendeskUserRepository.saveAll(users)
    }
}

@Repository
interface ZendeskUserRepository: MongoRepository<ZendeskUser, Long> {

    fun findAllByOperationIdAndActiveTrue(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskUser>

//    @Aggregation("")
//    fun findAllByOperationIdAndActiveTrue(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskUser>

    fun findAllByOperationIdAndActiveFalse(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskUser>

    fun countAllByOperationIdAndActiveTrue(@Param("operationId") operationId: String): Long

    fun countAllByOperationIdAndActiveFalse(@Param("operationId") operationId: String): Long
}

@Component
class ZendeskUserAggregationRepository(
    private val mongoTemplate: MongoTemplate
) {

    fun findActiveUsers(operationId: String): MutableList<ZendeskUserAggr> {
        return findUsers(operationId, true)
    }

    fun findDeletedUsers(operationId: String): MutableList<ZendeskUserAggr> {
        return findUsers(operationId, false)
    }

    private fun findUsers(operationId: String, active: Boolean): MutableList<ZendeskUserAggr> {
        val userAggregation = newAggregation(
//            match(Criteria.where("operationId").`is`(operationId).and("active").`is`(active).and("_id").`is`(1905241556334)),
            match(Criteria.where("operationId").`is`(operationId).and("active").`is`(active)),
            lookup("zendeskMapping", "organizationId", "zendeskId", "organization"),
            lookup("zendeskGroupMembership", "_id", "userId", "groupMembership"),
            unwind("groupMembership", true),
            lookup("zendeskMapping", "groupMembership.groupId", "zendeskId", "grispiGroupIds"),
            unwind("grispiGroupIds", true),
            group("_id")
                .first("\$\$ROOT").`as`("user")
                .first("\$organization.grispiId").`as`("grispiOrganizationId")
                .push("\$grispiGroupIds.grispiId").`as`("grispiGroupIds"),
            project("user", "grispiGroupIds")
                .and("\$grispiOrganizationId").arrayElementAt(0).`as`("grispiOrganizationId")
        )

        val aggregate = mongoTemplate.aggregate(userAggregation, ZendeskUser::class.java, ZendeskUserAggr::class.java)

        return aggregate.mappedResults;
    }

}

data class ZendeskUserAggr(
    val user: ZendeskUser,
    val grispiOrganizationId: Long? = null,
    val grispiGroupIds: Set<Long>? = null
)