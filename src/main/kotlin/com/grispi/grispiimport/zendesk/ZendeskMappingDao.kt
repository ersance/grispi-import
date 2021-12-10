package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.common.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.ListOperations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ZendeskMappingDao(
    @Autowired val zendeskMappingTemplate: RedisTemplate<String, Any>,
    @Autowired val logTemplate: RedisTemplate<String, ImportLog>
) {

    companion object {
        const val CUSTOM_FIELD = "cfield"
        const val USER = "user"
        const val ORGANIZATION = "org"
        const val GROUP = "group"
        const val TICKET = "ticket"
    }

    private val opsForHash: HashOperations<String, Long, String> = zendeskMappingTemplate.opsForHash()
    private val tenantHash: HashOperations<String, String, Any> = zendeskMappingTemplate.opsForHash()
    private val opsForList: ListOperations<String, ImportLog> = logTemplate.opsForList()

    fun errorLog(operationId: String, resourceName: String, message: String, externalId: Long?) {
        val logKey = generateLogKey(operationId, LogType.ERROR)
        opsForList.rightPush(logKey, ImportLog(LogType.ERROR, resourceName, message, externalId))
    }

    fun successLog(operationId: String, resourceName: String, message: String, externalId: Long?) {
        val logKey = generateLogKey(operationId, LogType.SUCCESS)
        opsForList.rightPush(logKey, ImportLog(LogType.SUCCESS, resourceName, message, externalId))
    }

    fun infoLog(operationId: String, resourceName: String, message: String, externalId: Long?) {
        val logKey = generateLogKey(operationId, LogType.INFO)
        opsForList.rightPush(logKey, ImportLog(LogType.INFO, resourceName, message, externalId))
    }

    fun getAllLogs(operationId: String): ImportLogContainer {
        return ImportLogContainer(
            opsForList.range(generateLogKey(operationId, LogType.SUCCESS), 0, Long.MAX_VALUE) as List<ImportLog>,
            opsForList.range(generateLogKey(operationId, LogType.ERROR), 0, Long.MAX_VALUE) as List<ImportLog>,
            opsForList.range(generateLogKey(operationId, LogType.INFO), 0, Long.MAX_VALUE) as List<ImportLog>,
        )
    }

    fun getAllLogsByTenant(tenantId: String): ImportLogContainer {
        val latestOperationId = findByTenantId(tenantId)
            .mapKeys { LocalDateTime.parse(it.key) }
            .toSortedMap { o1, o2 -> o1.compareTo(o2) }
            .entries.first().value

        return ImportLogContainer(
            opsForList.range(generateLogKey(latestOperationId.toString(), LogType.SUCCESS), 0, Long.MAX_VALUE) as List<ImportLog>,
            opsForList.range(generateLogKey(latestOperationId.toString(), LogType.ERROR), 0, Long.MAX_VALUE) as List<ImportLog>,
            opsForList.range(generateLogKey(latestOperationId.toString(), LogType.INFO), 0, Long.MAX_VALUE) as List<ImportLog>,
        )
    }

    fun addOrganizationMapping(operationId: String, zendeskId: Long, grispiId: Long) {
        val key = generateDataKey(operationId, ORGANIZATION)
        opsForHash.put(key, zendeskId, grispiId.toString())
    }

    fun getOrganizationId(operationId: String, zendeskId: Long): Long {
        val key = generateDataKey(operationId, ORGANIZATION)
        return opsForHash.get(key, zendeskId)?.toLong() ?: throw GrispiReferenceNotFoundException(zendeskId, "organization")
    }

    fun addCustomFieldMapping(operationId: String, zendeskId: Long, grispiId: Long) {
        val key = generateDataKey(operationId, CUSTOM_FIELD)
        opsForHash.put(key, zendeskId, grispiId.toString())
    }

    fun getCustomFieldId(operationId: String, zendeskId: Long): Long {
        val key = generateDataKey(operationId, CUSTOM_FIELD)
        return opsForHash.get(key, zendeskId)?.toLong() ?: throw GrispiReferenceNotFoundException(zendeskId, "custom field")
    }

    fun addUserMapping(operationId: String, zendeskId: Long, grispiId: Long) {
        val key = generateDataKey(operationId, USER)
        opsForHash.put(key, zendeskId, grispiId.toString())
    }

    fun getUserId(operationId: String, zendeskId: Long): Long {
        val key = generateDataKey(operationId, USER)
        return opsForHash.get(key, zendeskId)?.toLong() ?: throw GrispiReferenceNotFoundException(zendeskId, "user")
    }

    fun addGroupMapping(operationId: String, zendeskId: Long, grispiId: Long) {
        val key = generateDataKey(operationId, GROUP)
        opsForHash.put(key, zendeskId, grispiId.toString())
    }

    fun getGroupId(operationId: String, zendeskId: Long): Long {
        val key = generateDataKey(operationId, GROUP)
        return opsForHash.get(key, zendeskId)?.toLong() ?: throw GrispiReferenceNotFoundException(zendeskId, "user")
    }

    fun addTicketMapping(operationId: String, zendeskId: Long, grispiId: String) {
        val key = generateDataKey(operationId, TICKET)
        opsForHash.put(key, zendeskId, grispiId)
    }

    fun getTicketKey(operationId: String, zendeskId: Long): String {
        val key = generateDataKey(operationId, TICKET)
        return opsForHash.get(key, zendeskId) ?: throw GrispiReferenceNotFoundException(zendeskId, "ticket")
    }

    fun getTicketsZendeskIds(operationId: String): Set<Long> {
        val key = generateDataKey(operationId, TICKET)
        return opsForHash.keys(key)
    }

    private fun generateLogKey(operationId: String, logType: LogType): String {
        return "${operationId}_${logType.toString().lowercase()}_logs"
    }

    // TODO: 7.12.2021 change key
    private fun generateDataKey(operationId: String, resourceName: String): String {
        return "${operationId}_data_${resourceName}"
    }

    fun initializeTenant(operationId: String, tenantId: String) {
        tenantHash.put(tenantId, LocalDateTime.now().toString(), operationId)
    }

    fun findByTenantId(tenantId: String): Map<String, Any> {
        return tenantHash.entries(tenantId)
    }

}

class GrispiReferenceNotFoundException(val zendeskId: Long, val resourceName: String) : RuntimeException() {
    fun message(): String {
        return "requested ${resourceName} with zendesk reference id: ${zendeskId} couldn't be found"
    }
}

