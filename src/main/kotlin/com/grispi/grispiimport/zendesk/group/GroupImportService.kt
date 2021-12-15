package com.grispi.grispiimport.zendesk.group

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ZendeskApi.Companion.PAGE_SIZE
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class GroupImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskApi: ZendeskApi,
    @Autowired val zendeskGroupRepository: ZendeskGroupRepository
) {

    companion object {
        const val RESOURCE_NAME = "group"
    }

    fun import(operationId: String, zendeskApiCredentials: ZendeskApiCredentials) {

        val groupCount = zendeskApi.getGroupCount(zendeskApiCredentials)

        println("group import process is started for ${groupCount} items at: ${LocalDateTime.now()}")

        for (index in 1..(BigDecimal(groupCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt())) {
            val zendeskGroups = zendeskApi.getGroups(zendeskApiCredentials, ZendeskPageParams(index, PAGE_SIZE))

            zendeskGroups.forEach { it.operationId = operationId }

            zendeskGroupRepository.saveAll(zendeskGroups)
        }

        println("group import process is done")
    }

}
