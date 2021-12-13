package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.common.ImportLogDao
import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiApiException
import com.grispi.grispiimport.grispi.Group
import com.grispi.grispiimport.zendesk.ZendeskApi.Companion.PAGE_SIZE
import jodd.json.JsonParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class GroupImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskApi: ZendeskApi,
    @Autowired val zendeskMappingDao: ZendeskMappingDao
) {

    companion object {
        const val RESOURCE_NAME = "group"
    }

    fun import(operationId: String, zendeskImportRequest: ZendeskImportRequest) {

        val groupCount = zendeskApi.getGroupCount(zendeskImportRequest.zendeskApiCredentials)

        zendeskMappingDao.infoLog(operationId, RESOURCE_NAME, "${groupCount} groups found", null)
        println("group import process is started for ${groupCount} items at: ${LocalDateTime.now()}")

        for (index in 1..(BigDecimal(groupCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt())) {
            val zendeskGroups = zendeskApi.getGroups(zendeskImportRequest.zendeskApiCredentials, ZendeskPageParams(index, PAGE_SIZE))

            for (zendeskGroup in zendeskGroups) {
                try {
                    val createGroupResponse = grispiApi.createGroup(
                        zendeskGroup.toGrispiGroupRequest(),
                        zendeskImportRequest.grispiApiCredentials
                    )
                    val createdGroupId = JsonParser().parse(createGroupResponse.bodyRaw(), Long::class.java)
                    zendeskMappingDao.addGroupMapping(operationId, zendeskGroup.id, createdGroupId)

                    zendeskMappingDao.successLog(operationId,
                        RESOURCE_NAME,
                        "{${zendeskGroup.name}} created successfully",
                        null)
                } catch (exception: RuntimeException) {
                    when (exception) {
                        is GrispiApiException -> {
                            zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
                                "{${zendeskGroup.name} with id: ${zendeskGroup.id}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                                null)
                        }
                        else -> {
                            zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
                                "{${zendeskGroup.name} with id: ${zendeskGroup.id}} couldn't be imported. ${exception.message}",
                                null)
                        }
                    }
                }
            }
        }

        println("group import process is done")
    }

}
