package com.grispi.grispiimport.zendesk.organization

import com.grispi.grispiimport.utils.CalculateTimeSpent
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ZendeskApi.Companion.PAGE_SIZE
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class ZendeskOrganizationService(
    private val zendeskApi: ZendeskApi,
    private val zendeskOrganizationRepository: ZendeskOrganizationRepository
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

}