package com.grispi.grispiimport.zendesk.user

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiApiException
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ZendeskApi.Companion.PAGE_SIZE
import com.grispi.grispiimport.zendesk.ticket.TicketImportService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.stream.Collectors

@Service
class UserImportService(
    @Autowired val grispiApi: GrispiApi,
    @Autowired val zendeskApi: ZendeskApi,
    @Autowired val zendeskUserRepository: ZendeskUserRepository
) {

    companion object {
        const val RESOURCE_NAME = "user"
    }

    fun import(operationId: String, zendeskApiCredentials: ZendeskApiCredentials) {
        val userCount = zendeskApi.getUserCount(zendeskApiCredentials)

        println("user import process is started for ${userCount} items at: ${LocalDateTime.now()}")

        for (index in 1..(BigDecimal(userCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt())) {
            println("fetching ${index}. page for users")

            zendeskApi
                .getUsers(zendeskApiCredentials, ZendeskPageParams(index, PAGE_SIZE))
                .thenApply { users -> users.stream().peek { it.operationId = operationId }.collect(Collectors.toList()) }
                .thenApply { users -> zendeskUserRepository.saveAll(users) }
        }

        println("user import process is done")
    }

}
