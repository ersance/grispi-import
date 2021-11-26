package com.grispi.grispiimport.grispi

import jodd.http.HttpRequest
import jodd.http.HttpResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.lang.IllegalStateException

@Service
class GrispiApi {

    companion object {
        const val HOST: String = "http://grispi.com:8080"
        const val TENANT_ID_HEADER_NAME: String = "tenantId"

        const val CUSTOM_FIELD_ENDPOINT: String = "/fields"
        const val ADMIN_ENDPOINT: String = "/admins"
        const val END_USER_ENDPOINT: String = "/users"
        const val AGENT_ENDPOINT: String = "/agents"
        const val TICKET_ENDPOINT: String = "/tickets"
    }

    fun createCustomField(ticketField: GrispiTicketField, apiCredentials: GrispiApiCredentials): HttpResponse {

        return post(CUSTOM_FIELD_ENDPOINT, ticketField.toJson(), apiCredentials)
    }

    fun createUser(userRequest: UserRequest, apiCredentials: GrispiApiCredentials): HttpResponse {

        val path = when(userRequest.role) {
            Role.END_USER.toString() -> END_USER_ENDPOINT
            Role.AGENT.toString() -> AGENT_ENDPOINT
            Role.ADMIN.toString() -> ADMIN_ENDPOINT
            else -> throw IllegalStateException("'${userRequest.role}' is invalid")
        }

        return post(path, userRequest.toJson(), apiCredentials)
    }

    fun createTicket(ticketRequest: TicketRequest, apiCredentials: GrispiApiCredentials): HttpResponse {
        return post(TICKET_ENDPOINT, ticketRequest.toJson(), apiCredentials)
    }

    private fun post(endpoint: String, requestBody: String, apiCredentials: GrispiApiCredentials): HttpResponse {
        return HttpRequest
            .post("${HOST}${endpoint}")
            .body(requestBody)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${apiCredentials.token}")
            .header(TENANT_ID_HEADER_NAME, apiCredentials.tenantId)
            .send()
    }

}