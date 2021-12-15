package com.grispi.grispiimport.zendesk.organization

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiApiException
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ZendeskApi.Companion.PAGE_SIZE
import com.grispi.grispiimport.zendesk.group.ZendeskGroup
import jodd.json.JsonParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class OrganizationImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskApi: ZendeskApi,
    @Autowired val zendeskOrganizationRepository: ZendeskOrganizationRepository
) {

    companion object {
        const val RESOURCE_NAME = "organization"
    }

    fun import(operationId: String, zendeskApiCredentials: ZendeskApiCredentials) {
        val orgCount = zendeskApi.getOrganizationCount(zendeskApiCredentials)

        println("organization import process is started for ${orgCount} items at: ${LocalDateTime.now()}")

        for (index in 1..(BigDecimal(orgCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt())) {
            println("fetching ${index}. page")

            val organizations = zendeskApi.getOrganizations(zendeskApiCredentials, ZendeskPageParams(index, PAGE_SIZE))

            organizations.forEach { it.operationId = operationId }

            zendeskOrganizationRepository.saveAll(organizations)
        }
        println("organization import process is done")
    }

}