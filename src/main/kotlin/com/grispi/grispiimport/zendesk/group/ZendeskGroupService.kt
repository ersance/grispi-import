package com.grispi.grispiimport.zendesk.group

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.utils.CalculateTimeSpent
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ZendeskApi.Companion.PAGE_SIZE
import com.grispi.grispiimport.zendesk.organization.ResourceCount
import com.grispi.grispiimport.zendesk.organization.ZendeskOrganizationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@Service
class ZendeskGroupService(
    private val zendeskApi: ZendeskApi,
    private val zendeskGroupRepository: ZendeskGroupRepository
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
