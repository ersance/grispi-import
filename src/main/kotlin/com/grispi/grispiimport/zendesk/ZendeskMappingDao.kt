package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.common.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.data.annotation.Immutable
import org.springframework.data.redis.core.ListOperations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.stereotype.Service
import org.springframework.web.context.WebApplicationContext.SCOPE_REQUEST
import java.util.*

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

    private val opsForValue: ValueOperations<String, Any> = zendeskMappingTemplate.opsForValue()
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

    fun addOrganizationMapping(operationId: String, zendeskId: Long, grispiId: Long) {
        val key = generateDataKey(operationId, ORGANIZATION, zendeskId)
        opsForValue.append(key, grispiId.toString())
    }

    fun getOrganizationId(operationId: String, zendeskId: Long): Long {
        val key = generateDataKey(operationId, ORGANIZATION, zendeskId)
        return opsForValue.get(key)?.toString()?.toLong() ?: throw GrispiReferenceNotFoundException(zendeskId, "organization")
    }

    fun addCustomFieldMapping(operationId: String, zendeskId: Long, grispiId: Long) {
        val key = generateDataKey(operationId, CUSTOM_FIELD, zendeskId)
        opsForValue.append(key, grispiId.toString())
    }

    fun getCustomFieldId(operationId: String, zendeskId: Long): Long {
        val key = generateDataKey(operationId, CUSTOM_FIELD, zendeskId)
        return opsForValue.get(key)?.toString()?.toLong() ?: throw GrispiReferenceNotFoundException(zendeskId, "custom field")
    }

    fun addUserMapping(operationId: String, zendeskId: Long, grispiId: Long) {
        val key = generateDataKey(operationId, USER, zendeskId)
        opsForValue.append(key, grispiId.toString())
    }

    fun getUserId(operationId: String, zendeskId: Long): Long {
        val key = generateDataKey(operationId, USER, zendeskId)
        return opsForValue.get(key)?.toString()?.toLong() ?: throw GrispiReferenceNotFoundException(zendeskId, "user")
    }

    fun addGroupMapping(operationId: String, zendeskId: Long, grispiId: Long) {
        val key = generateDataKey(operationId, GROUP, zendeskId)
        opsForValue.append(key, grispiId.toString())
    }

    fun getGroupId(operationId: String, zendeskId: Long): Long {
        val key = generateDataKey(operationId, GROUP, zendeskId)
        return opsForValue.get(key)?.toString()?.toLong() ?: throw GrispiReferenceNotFoundException(zendeskId, "user")
    }

    fun addTicketMapping(operationId: String, zendeskId: Long, grispiId: String) {
        val key = generateDataKey(operationId, TICKET, zendeskId)
        opsForValue.append(key, grispiId.toString())
    }

    fun getTicketId(operationId: String, zendeskId: Long): Long {
        val key = generateDataKey(operationId, TICKET, zendeskId)
        return opsForValue.get(key)?.toString()?.toLong() ?: throw GrispiReferenceNotFoundException(zendeskId, "user")
    }

    private fun generateLogKey(operationId: String, logType: LogType): String {
        return "${operationId}_${logType.toString().lowercase()}_logs"
    }

    // TODO: 7.12.2021 change key
    private fun generateDataKey(operationId: String, resourceName: String, zendeskId: Long): String {
        return "${operationId}_data_${resourceName}_${zendeskId}"
    }

}

class GrispiReferenceNotFoundException(val zendeskId: Long, val resourceName: String) : RuntimeException() {
    fun message(): String {
        return "requested ${resourceName} with zendesk reference id: ${zendeskId} couldn't be found"
    }
}

