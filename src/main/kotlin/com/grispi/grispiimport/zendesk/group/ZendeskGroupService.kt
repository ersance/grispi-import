package com.grispi.grispiimport.zendesk.group

import com.grispi.grispiimport.utils.CalculateTimeSpent
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ZendeskApi.Companion.PAGE_SIZE
import com.grispi.grispiimport.zendesk.organization.ResourceCount
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.CompletableFuture

@Service
class ZendeskGroupService(
    private val zendeskApi: ZendeskApi,
    private val zendeskGroupRepository: ZendeskGroupRepository,
    private val zendeskGroupMembershipRepository: ZendeskGroupMembershipRepository
) {

    companion object {
        const val RESOURCE_NAME = "group"
    }

    @CalculateTimeSpent
    fun fetch(operationId: String, zendeskApiCredentials: ZendeskApiCredentials) {

        val groupCount = zendeskApi.getGroupCount(zendeskApiCredentials)

        for (index in 1..(BigDecimal(groupCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt())) {
            val zendeskGroups = zendeskApi.getGroups(zendeskApiCredentials, ZendeskPageParams(index, PAGE_SIZE))

            zendeskGroups.forEach { it.operationId = operationId }

            zendeskGroupRepository.saveAll(zendeskGroups)
        }
    }

    fun fetchGroupMemberships(operationId: String, zendeskApiCredentials: ZendeskApiCredentials) {

        val groupMembershipCount = zendeskApi.getGroupMembershipCount(zendeskApiCredentials)

        for (index in 1..(BigDecimal(groupMembershipCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt())) {
            val zendeskGroupMemberships = zendeskApi.getGroupMemberships(zendeskApiCredentials, ZendeskPageParams(index, PAGE_SIZE))

            zendeskGroupMemberships.forEach { it.operationId = operationId }

            zendeskGroupMembershipRepository.saveAll(zendeskGroupMemberships)
        }
    }

    fun fetchedGroupsCount(operationId: String): Long {
        return zendeskGroupRepository.countAllByOperationId(operationId)
    }

    fun counts(operationId: String, zendeskApiCredentials: ZendeskApiCredentials): ResourceCount {
        return CompletableFuture
            .supplyAsync { zendeskApi.getGroupCount(zendeskApiCredentials) }
            .thenCombine(
                CompletableFuture.supplyAsync { fetchedGroupsCount(operationId) },
                { zCount, fCount -> ResourceCount(RESOURCE_NAME, zCount, fCount) })
            .get()
    }

}

@Repository
interface ZendeskGroupRepository: MongoRepository<ZendeskGroup, Long> {
    fun findAllByOperationId(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskGroup>
    fun countAllByOperationId(@Param("operationId") operationId: String): Long
}

@Repository
interface ZendeskGroupMembershipRepository: MongoRepository<ZendeskGroupMembership, Long> {
    fun findAllByOperationId(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskGroupMembership>
    fun countAllByOperationId(@Param("operationId") operationId: String): Long
}
