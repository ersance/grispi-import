package com.grispi.grispiimport.common

import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceConstructor
import org.springframework.data.redis.core.RedisHash
import java.util.*

class ImportLog(val type: LogType, val resourceName: String, val message: String, val externalId: Long? = null) {

    @Id
    val id: String = UUID.randomUUID().toString()

    val createdAt: Long = System.currentTimeMillis()
}

enum class LogType {
    ERROR, INFO, SUCCESS
}

// resourceName: (user, organization, customField, group, ticket)
// externalId: (zendeskId, freshDeskId)
//@RedisHash("importErrorLog")
//data class ImportErrorLog(val resourceName: String, val message: String, val externalId: Long? = null): ImportLog(LogType.ERROR) {
//
//}
//
//@RedisHash("importSuccessLog")
//data class ImportSuccessLog(val resourceName: String?, val message: String, val externalId: Long? = null): ImportLog(LogType.SUCCESS) {
//    @Id
//    val id: String = UUID.randomUUID().toString()
//}
//
//@RedisHash("importInfoLog")
//data class ImportInfoLog(val resourceName: String?, val message: String, val externalId: Long? = null): ImportLog(LogType.INFO) {
//    @Id
//    val id: String = UUID.randomUUID().toString()
//}
