package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.*
import com.grispi.grispiimport.zendesk.group.ZendeskGroupMembership
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import java.util.stream.Collectors

@Repository
interface ZendeskMappingRepository: MongoRepository<ZendeskMapping, String>

@Repository
class ZendeskMappingQueryRepository(
    private val mongoTemplate: MongoTemplate,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun findGrispiUserId(zendeskId: Long): String {
        val query = Query()
        query.addCriteria(Criteria.where("zendeskId").`is`(zendeskId).and("resourceName").`is`(GrispiUserImportService.RESOURCE_NAME))
        val zendeskMapping = mongoTemplate.findOne(query, ZendeskMapping::class.java)
        if (zendeskMapping != null) {
            return zendeskMapping.grispiId
        }
        else {
            logger.warn("grispi user not found: $zendeskId")
            throw GrispiReferenceNotFoundException(zendeskId, GrispiUserImportService.RESOURCE_NAME)
        }
    }

    fun findGrispiOrganizationId(zendeskId: Long): String? {
        val query = Query()
        query.addCriteria(Criteria.where("zendeskId").`is`(zendeskId).and("resourceName").`is`(GrispiOrganizationImportService.RESOURCE_NAME))
        val zendeskMapping = mongoTemplate.findOne(query, ZendeskMapping::class.java)
        return zendeskMapping?.grispiId
    }

    fun findGrispiGroupId(zendeskId: Long): String? {
        val query = Query()
        query.addCriteria(Criteria.where("zendeskId").`is`(zendeskId).and("resourceName").`is`(GrispiGroupImportService.RESOURCE_NAME))
        val zendeskMapping = mongoTemplate.findOne(query, ZendeskMapping::class.java)
        return zendeskMapping?.grispiId
    }

    // retrieves zendesk group membership ids for a given zendesk user id
    fun findGrispiGroupMemberships(zendeskUserId: Long): Set<String> {
        val memberships = mongoTemplate.find(Query().addCriteria(Criteria.where("userId").`is`(zendeskUserId)), ZendeskGroupMembership::class.java)

        val query = Query()
        query.addCriteria(Criteria.where("zendeskId").`in`(memberships.map { it.groupId }).and("resourceName").`is`(GrispiGroupImportService.RESOURCE_NAME))
        val zendeskMapping = mongoTemplate.find(query, ZendeskMapping::class.java)
        return zendeskMapping.stream().map { it.grispiId }.collect(Collectors.toSet())
    }

    fun findGrispiTicketKey(zendeskId: Long): String {
        val query = Query()
        query.addCriteria(Criteria.where("zendeskId").`is`(zendeskId).and("resourceName").`is`(GrispiTicketImportService.RESOURCE_NAME))
        val zendeskMapping = mongoTemplate.findOne(query, ZendeskMapping::class.java)
        if (zendeskMapping != null) {
            return zendeskMapping.grispiId
        }
        else {
            logger.warn("grispi ticket not found: $zendeskId")
            throw GrispiReferenceNotFoundException(zendeskId, GrispiTicketImportService.RESOURCE_NAME)
        }
    }

    fun findGrispiTicketFieldKey(zendeskId: Long): String? {
        val query = Query()
        query.addCriteria(Criteria.where("zendeskId").`is`(zendeskId).and("resourceName").`is`(GrispiTicketFieldImportService.RESOURCE_NAME))
        val zendeskMapping = mongoTemplate.findOne(query, ZendeskMapping::class.java)
        return zendeskMapping?.grispiId
    }

    fun findAllGrispiTicketFieldKeys(zendeskIds: Set<Long>): Set<String> {
        val query = Query()
        query.addCriteria(Criteria.where("zendeskId").`in`(zendeskIds).and("resourceName").`is`(GrispiTicketFieldImportService.RESOURCE_NAME))
        val ticketFieldKeys = mongoTemplate.find(query, ZendeskMapping::class.java, "zendeskMapping")
        return ticketFieldKeys.map { it.grispiId }.toSet()
    }

    fun findGrispiTicketFormId(zendeskId: Long): String {
        val query = Query()
        query.addCriteria(Criteria.where("zendeskId").`is`(zendeskId).and("resourceName").`is`(GrispiTicketFormImportService.RESOURCE_NAME))
        val zendeskMapping = mongoTemplate.findOne(query, ZendeskMapping::class.java)
        if (zendeskMapping != null) {
            return zendeskMapping.grispiId
        }
        else {
            logger.warn("grispi ticket form not found: $zendeskId")
            throw GrispiReferenceNotFoundException(zendeskId, GrispiTicketFormImportService.RESOURCE_NAME)
        }
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
    ERROR, INFO, SUCCESS, WARNING
}

class GrispiReferenceNotFoundException(val zendeskId: Long, val resourceName: String) : RuntimeException() {
    fun printMessage(): String {
        return "requested ${resourceName} with zendesk reference id: ${zendeskId} couldn't be found"
    }
}