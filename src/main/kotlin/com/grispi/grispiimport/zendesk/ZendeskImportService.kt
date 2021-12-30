package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.common.NotFoundException
import com.grispi.grispiimport.grispi.GrispiImportService
import org.slf4j.MDC
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
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

    private fun createTenantImportOperation(zendeskImportRequest: ZendeskImportRequest, resourceCounts: Map<String, Long>): ZendeskTenantImport {
        val tenantImport = ZendeskTenantImport(
            zendeskImportRequest.grispiApiCredentials.tenantId,
            zendeskImportRequest.zendeskApiCredentials.subdomain,
            resourceCounts)
        return zendeskImportRepository.save(tenantImport)
    }

    fun checkStatus(operationId: String, zendeskApiCredentials: ZendeskApiCredentials): ZendeskImportStatusResponse {
        val import = zendeskImportRepository.findById(operationId)
            .orElseThrow { NotFoundException("zendesk tenant import not found by id: ${operationId} ") }

        val fetchedResourcesCounts = zendeskFetchService.fetchedResourcesCounts(operationId, zendeskApiCredentials)

        return ZendeskImportStatusResponse(import.grispiTenantId, import.zendeskTenantId, import.createdAt, fetchedResourcesCounts)
    }

}

interface ZendeskImportRepository: MongoRepository<ZendeskTenantImport, String> {
}

@Document
class ZendeskTenantImport(val grispiTenantId: String, val zendeskTenantId: String, val resources: Map<String, Long>?) {

    @Id
    var id: String = UUID.randomUUID().toString()

    var createdAt: Long = System.currentTimeMillis()

}
