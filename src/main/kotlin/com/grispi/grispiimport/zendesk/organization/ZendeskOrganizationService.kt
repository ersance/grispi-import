package com.grispi.grispiimport.zendesk.organization

import com.grispi.grispiimport.utils.CalculateTimeSpent
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ZendeskApi.Companion.PAGE_SIZE
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
class ZendeskOrganizationService(
    private val zendeskApi: ZendeskApi,
    private val zendeskOrganizationRepository: ZendeskOrganizationRepository,
) {

    companion object {
        const val RESOURCE_NAME = "organization"
    }

    @CalculateTimeSpent
    fun fetch(operationId: String, zendeskApiCredentials: ZendeskApiCredentials) {
        val orgCount = zendeskApi.getOrganizationCount(zendeskApiCredentials)

        for (index in 1..(BigDecimal(orgCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt())) {
            val organizations = zendeskApi.getOrganizations(zendeskApiCredentials, ZendeskPageParams(index, PAGE_SIZE))

            organizations.forEach { it.operationId = operationId }

            zendeskOrganizationRepository.saveAll(organizations)
        }
    }

    fun fetchedOrganizationsCount(operationId: String): Long {
        return zendeskOrganizationRepository.countAllByOperationId(operationId)
    }

    fun counts(operationId: String, zendeskApiCredentials: ZendeskApiCredentials): ResourceCount {
        return CompletableFuture
            .supplyAsync { zendeskApi.getOrganizationCount(zendeskApiCredentials) }
            .thenCombine(
                CompletableFuture.supplyAsync { fetchedOrganizationsCount(operationId) },
                { zCount, fCount -> ResourceCount(RESOURCE_NAME, zCount, fCount) })
            .get()
    }

}

data class ResourceCount(val resource: String, val expected: Long?, val fetched: Long?)

@Repository
interface ZendeskOrganizationRepository: MongoRepository<ZendeskOrganization, Long> {
    fun findAllByOperationId(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskOrganization>
    fun countAllByOperationId(@Param("operationId") operationId: String): Long
}