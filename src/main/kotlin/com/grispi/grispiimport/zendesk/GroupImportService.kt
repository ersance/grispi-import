package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.common.ImportLogDao
import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiApiException
import com.grispi.grispiimport.grispi.Group
import jodd.json.JsonParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import org.springframework.web.context.WebApplicationContext

@Service
class GroupImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskApi: ZendeskApi,
    @Autowired val zendeskMappingDao: ZendeskMappingDao,
    @Autowired val importLogDao: ImportLogDao
) {

    companion object {
        const val RESOURCE_NAME = "group"
    }

    fun import(operationId: String, zendeskImportRequest: ZendeskImportRequest) {
        val groups = zendeskApi.getGroups(zendeskImportRequest.zendeskApiCredentials)
        val zendeskGroups = JsonParser().parse(groups.bodyRaw(), ZendeskGroups::class.java)

        zendeskMappingDao.infoLog(operationId, RESOURCE_NAME, "${zendeskGroups.groups.count()} groups found", null)
        println("ticket import process is started for ${zendeskGroups.groups} tickets")

        for (zendeskGroup in zendeskGroups.groups) {
            try {
                val createGroupResponse = grispiApi.createGroup(
                    zendeskGroup.toGrispiGroupRequest(),
                    zendeskImportRequest.grispiApiCredentials
                )
                val createdGroupId = JsonParser().parse(createGroupResponse.bodyRaw(), Long::class.java)
                zendeskMappingDao.addGroupMapping(operationId, zendeskGroup.id, createdGroupId)

                zendeskMappingDao.successLog(operationId, RESOURCE_NAME, "{${zendeskGroup.name}} created successfully", null)
            } catch (exception: GrispiApiException) {
                zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
                    "{${zendeskGroup.name} with id: ${zendeskGroup.id}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                    null)
            }
        }

        println("group import process is done")
    }

}
