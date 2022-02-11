package com.grispi.grispiimport.zendesk.userfield

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.zendesk.ZendeskApi
import com.grispi.grispiimport.zendesk.ZendeskApiCredentials
import com.grispi.grispiimport.zendesk.ZendeskUserField
import com.grispi.grispiimport.zendesk.organization.ResourceCount
import com.grispi.grispiimport.zendesk.organization.ZendeskOrganizationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

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

    fun fetchedUserFieldsCount(operationId: String): Long {
        return zendeskUserFieldRepository.countAllByOperationId(operationId)
    }

    fun counts(operationId: String, zendeskApiCredentials: ZendeskApiCredentials): ResourceCount {
        return CompletableFuture
            .supplyAsync { zendeskApi.getUserFieldCount(zendeskApiCredentials) }
            .thenCombine(
                CompletableFuture.supplyAsync { fetchedUserFieldsCount(operationId) },
                { zCount, fCount -> ResourceCount(RESOURCE_NAME, zCount, fCount) })
            .get()
    }

}

@Repository
interface ZendeskUserFieldRepository: MongoRepository<ZendeskUserField, String> {
    fun findAllByOperationId(@Param("operationId") operationId: String, pageable: Pageable): Page<ZendeskUserField>
    fun countAllByOperationId(@Param("operationId") operationId: String): Long
}