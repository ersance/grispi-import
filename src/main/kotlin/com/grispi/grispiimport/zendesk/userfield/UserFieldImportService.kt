package com.grispi.grispiimport.zendesk.userfield

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.zendesk.ZendeskApi
import com.grispi.grispiimport.zendesk.ZendeskApiCredentials
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserFieldImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskApi: ZendeskApi,
    @Autowired val zendeskUserFieldRepository: ZendeskUserFieldRepository
) {

    companion object {
        const val RESOURCE_NAME = "user_field"
    }

    fun import(operationId: String, zendeskApiCredentials: ZendeskApiCredentials) {
        val zendeskUserFields = zendeskApi.getUserFields(zendeskApiCredentials)

        zendeskUserFields.forEach { it.operationId = operationId }

        zendeskUserFieldRepository.saveAll(zendeskUserFields)

//        zendeskMappingDao.infoLog(operationId, RESOURCE_NAME, "${zendeskUserFields.count()} user fields found", null)
//        println("user field import process is started for ${zendeskUserFields.count()} items at: ${LocalDateTime.now()}")

//        for (zendeskUserField in zendeskUserFields) {
//            try {
//                grispiApi.createUserField(zendeskUserField.toGrispiUserField(), zendeskImportRequest.grispiApiCredentials)
//
//                zendeskMappingDao.successLog(operationId, RESOURCE_NAME,
//                    "{${zendeskUserField.title}} created successfully", null)
//            } catch (exception: RuntimeException) {
//                when (exception) {
//                    is GrispiApiException -> {
//                        zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
//                            "{${zendeskUserField.title}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
//                            null)
//                    }
//                    else -> {
//                        zendeskMappingDao.errorLog(operationId, RESOURCE_NAME,
//                            "{${zendeskUserField.title} couldn't be imported. ${exception.message}",
//                            null)
//                    }
//                }
//            }
//        }

    }

}
