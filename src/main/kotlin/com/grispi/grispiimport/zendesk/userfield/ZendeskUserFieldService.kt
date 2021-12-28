package com.grispi.grispiimport.zendesk.userfield

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.zendesk.ZendeskApi
import com.grispi.grispiimport.zendesk.ZendeskApiCredentials
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ZendeskUserFieldService(
    private val zendeskApi: ZendeskApi,
    private val zendeskUserFieldRepository: ZendeskUserFieldRepository
) {

    companion object {
        const val RESOURCE_NAME = "user_field"
    }

    fun fetch(operationId: String, zendeskApiCredentials: ZendeskApiCredentials) {
        val zendeskUserFields = zendeskApi.getUserFields(zendeskApiCredentials)

        zendeskUserFields.forEach { it.operationId = operationId }

        zendeskUserFieldRepository.saveAll(zendeskUserFields)
    }

}
