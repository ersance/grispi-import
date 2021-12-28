package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.GrispiImportService
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
class ZendeskImportService(
    private val zendeskImportRepository: ZendeskImportRepository,
    private val zendeskFetchService: ZendeskFetchService,
    private val grispiImportService: GrispiImportService
) {

    fun import(zendeskImportRequest: ZendeskImportRequest): ZendeskTenantImport {

        val resourceCounts = zendeskFetchService.fetchResourceCounts(zendeskImportRequest.zendeskApiCredentials)

        val zendeskTenantImport = createTenantImportOperation(zendeskImportRequest, resourceCounts)

        zendeskImportRequest.zendeskApiCredentials.operationId = zendeskTenantImport.id

        CompletableFuture
            .supplyAsync { zendeskFetchService.fetchResources(zendeskTenantImport.id, zendeskImportRequest.zendeskApiCredentials) }
            .thenRun { grispiImportService.import(zendeskTenantImport.id, zendeskImportRequest.grispiApiCredentials) }

        return zendeskTenantImport
    }

    fun fetch(operationId: String?, zendeskImportRequest: ZendeskImportRequest) {

        val opId: String = operationId ?: createTenantImportOperation(zendeskImportRequest, mapOf()).id

        if (zendeskImportRepository.existsById(opId)) {
            CompletableFuture.supplyAsync { zendeskFetchService.fetchResources(opId, zendeskImportRequest.zendeskApiCredentials) }
        }
        else {
            throw RuntimeException("operation not found")
        }
    }

    fun import(operationId: String, zendeskImportRequest: ZendeskImportRequest) {
        if (zendeskImportRepository.existsById(operationId)) {
            grispiImportService.import(operationId, zendeskImportRequest.grispiApiCredentials)
        }
        else {
            throw RuntimeException("operation not found")
        }
    }

    private fun createTenantImportOperation(zendeskImportRequest: ZendeskImportRequest, resourceCounts: Map<String, Int>): ZendeskTenantImport {
        val tenantImport = ZendeskTenantImport(
            zendeskImportRequest.grispiApiCredentials.tenantId,
            zendeskImportRequest.zendeskApiCredentials.subdomain,
            resourceCounts)
        return zendeskImportRepository.save(tenantImport)
    }

}

interface ZendeskImportRepository: MongoRepository<ZendeskTenantImport, String> {
}

class ZendeskTenantImport(val grispiTenantId: String, val zendeskTenantId: String, val resources: Map<String, Int>?) {

    // TODO: 16.12.2021 operation id
    @Id
    val id: String = UUID.randomUUID().toString()

    val createdAt: Long = System.currentTimeMillis()

}
