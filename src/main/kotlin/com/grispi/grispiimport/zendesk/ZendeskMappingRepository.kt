package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.GrispiGroupImportService
import com.grispi.grispiimport.grispi.GrispiTicketImportService
import com.grispi.grispiimport.grispi.GrispiUserImportService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ZendeskMappingRepository: MongoRepository<ZendeskMapping, String>

@Repository
class ZendeskMappingQueryRepository(
    @Autowired val mongoTemplate: MongoTemplate,
) {

    fun findGrispiUserId(zendeskId: Long): String {
        val query = org.springframework.data.mongodb.core.query.Query()
        query.addCriteria(Criteria.where("zendeskId").`is`(zendeskId).and("resourceName").`is`(GrispiUserImportService.RESOURCE_NAME))
        val zendeskMapping = mongoTemplate.findOne(query, ZendeskMapping::class.java)
        return zendeskMapping?.grispiId ?: throw GrispiReferenceNotFoundException(zendeskId, GrispiUserImportService.RESOURCE_NAME)
    }

    fun findGrispiGroupId(zendeskId: Long): String? {
        val query = org.springframework.data.mongodb.core.query.Query()
        query.addCriteria(Criteria.where("zendeskId").`is`(zendeskId).and("resourceName").`is`(GrispiGroupImportService.RESOURCE_NAME))
        val zendeskMapping = mongoTemplate.findOne(query, ZendeskMapping::class.java)
        return zendeskMapping?.grispiId
    }

    fun findGrispiTicketKey(zendeskId: Long): String {
        val query = org.springframework.data.mongodb.core.query.Query()
        query.addCriteria(Criteria.where("zendeskId").`is`(zendeskId).and("resourceName").`is`(GrispiTicketImportService.RESOURCE_NAME))
        val zendeskMapping = mongoTemplate.findOne(query, ZendeskMapping::class.java)
        return zendeskMapping?.grispiId ?: throw GrispiReferenceNotFoundException(zendeskId, GrispiTicketImportService.RESOURCE_NAME)
    }

}

@Repository
interface ZendeskLogRepository: MongoRepository<ImportLog, String> {
}

@Document
data class ZendeskMapping(
    @Id
    val id: String? = null,
    val zendeskId: Long,
    val grispiId: String,
    val resourceName: String,
    val operationId: String,
)

@Document
data class ImportLog(
    @Id
    val id: String? = null,
    val type: LogType,
    val resourceName: String,
    val message: String,
    val operationId: String,
) {
    val createdAt: Long = System.currentTimeMillis()
}

enum class LogType {
    ERROR, INFO, SUCCESS
}

class GrispiReferenceNotFoundException(val zendeskId: Long, val resourceName: String) : RuntimeException() {
    fun message(): String {
        return "requested ${resourceName} with zendesk reference id: ${zendeskId} couldn't be found"
    }
}