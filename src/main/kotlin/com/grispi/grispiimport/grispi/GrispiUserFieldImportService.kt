package com.grispi.grispiimport.grispi

import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.userfield.ZendeskUserFieldRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GrispiUserFieldImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskMappingRepository: ZendeskMappingRepository,
    @Autowired val zendeskLogRepository: ZendeskLogRepository,
    @Autowired val zendeskUserFieldRepository: ZendeskUserFieldRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val RESOURCE_NAME = "user_field"
        const val PAGE_SIZE = 1000
    }

    fun import(operationId: String, grispiApiCredentials: GrispiApiCredentials) {
        var userFields = zendeskUserFieldRepository.findAllByOperationId(operationId, Pageable.ofSize(PAGE_SIZE))

        logger.info("user field import process is started for ${userFields.totalElements} fields at: ${LocalDateTime.now()}")

        do {
            for (userField in userFields.content) {
                try {
                    val createUserFieldResponse = grispiApi.createUserField(userField.toGrispiUserField(), grispiApiCredentials)

                    zendeskMappingRepository.save(ZendeskMapping(null, userField.id, createUserFieldResponse.bodyText(), RESOURCE_NAME, operationId))

                    zendeskLogRepository.save(ImportLog(null, LogType.SUCCESS, RESOURCE_NAME, "{${userField.title}} created successfully", operationId))
                } catch (exception: RuntimeException) {
                    when (exception) {
                        is GrispiApiException -> {
                            zendeskLogRepository.save(
                                ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                    "{${userField.title} with id: ${userField.id}} couldn't be imported. status code: ${exception.statusCode} message: ${exception.exceptionMessage}",
                                    operationId))
                        }
                        else -> {
                            zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                                "{${userField.title} with id: ${userField.id}} couldn't be imported. ${exception.message}",
                                operationId))
                        }
                    }
                }
            }

            if (userFields.hasNext()) {
                userFields = zendeskUserFieldRepository.findAllByOperationId(operationId, userFields.nextPageable())
            }
        } while (userFields.hasNext())


        // create phone number user field
        try {
            grispiApi.createUserField(GrispiUserFieldRequest.Builder().buildPhoneNumberField(), grispiApiCredentials)

            zendeskLogRepository.save(ImportLog(null, LogType.SUCCESS, RESOURCE_NAME, "zendesk phone number user field created successfully", operationId))
        } catch (exception: RuntimeException) {
            zendeskLogRepository.save(
                ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                    "zendesk phone number user field couldn't be imported. message: ${exception.message}", operationId))
        }

        // create external id user field
        try {
            grispiApi.createUserField(GrispiUserFieldRequest.Builder().buildExternalId(), grispiApiCredentials)

            zendeskLogRepository.save(ImportLog(null, LogType.SUCCESS, RESOURCE_NAME, "zendesk external id user field created successfully", operationId))
        } catch (exception: RuntimeException) {
            zendeskLogRepository.save(ImportLog(null, LogType.ERROR, RESOURCE_NAME,
                    "zendesk external id user field couldn't be imported. message: ${exception.message}", operationId))
        }

        logger.info("user field import process has ended for ${userFields.totalElements} user field at: ${LocalDateTime.now()}")
    }

}
