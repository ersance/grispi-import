package com.grispi.grispiimport.grispi

import jodd.http.HttpRequest
import jodd.http.HttpResponse
import jodd.http.HttpStatus
import jodd.json.JsonSerializer
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.lang.IllegalStateException

@Service
class GrispiApi {

    companion object {
        const val HOST: String = "http://grispi.com:8080"
        const val TENANT_ID_HEADER_NAME: String = "tenantId"

        const val ORGANIZATIONS_ENDPOINT: String = "/import/organizations"
        const val GROUPS_ENDPOINT: String = "/import/groups"
        const val CUSTOM_FIELD_ENDPOINT: String = "/import/custom-fields"
        const val USER_FIELD_ENDPOINT: String = "/import/user-fields"
        const val USER_ENDPOINT: String = "/import/users"
        const val TICKET_ENDPOINT: String = "/import/tickets"
        const val TICKET_COMMENT_ENDPOINT: String = "/comments"
    }

    fun createOrganization(grispiOrganizationRequest: GrispiOrganizationRequest, grispiApiCredentials: GrispiApiCredentials): HttpResponse {
        val httpResponse = post(ORGANIZATIONS_ENDPOINT, grispiOrganizationRequest.toJson(), grispiApiCredentials)

        if (!httpResponse.statusCode().equals(HttpStatus.HTTP_CREATED)) {
            throw GrispiApiException(httpResponse.statusCode(), httpResponse.bodyText())
        }

        return httpResponse
    }

    fun createGroup(grispiGroupRequest: GrispiGroupRequest, grispiApiCredentials: GrispiApiCredentials): HttpResponse {
        val httpResponse = post(GROUPS_ENDPOINT, grispiGroupRequest.toJson(), grispiApiCredentials)

        if (!httpResponse.statusCode().equals(HttpStatus.HTTP_CREATED)) {
            throw GrispiApiException(httpResponse.statusCode(), httpResponse.bodyText())
        }

        return httpResponse
    }

    fun createCustomField(grispiTicketFieldRequest: GrispiTicketFieldRequest, apiCredentials: GrispiApiCredentials): HttpResponse {
        val httpResponse = post(CUSTOM_FIELD_ENDPOINT, grispiTicketFieldRequest.toJson(), apiCredentials)

        if (!httpResponse.statusCode().equals(HttpStatus.HTTP_CREATED)) {
            throw GrispiApiException(httpResponse.statusCode(), httpResponse.bodyText())
        }

        return httpResponse
    }

    fun createUserField(grispiUserFieldRequest: GrispiUserFieldRequest, apiCredentials: GrispiApiCredentials): HttpResponse {
        val httpResponse = post(USER_FIELD_ENDPOINT, grispiUserFieldRequest.toJson(), apiCredentials)

        if (!httpResponse.statusCode().equals(HttpStatus.HTTP_CREATED)) {
            throw GrispiApiException(httpResponse.statusCode(), httpResponse.bodyText())
        }

        return httpResponse
    }

    fun createUser(userRequest: UserRequest, apiCredentials: GrispiApiCredentials): HttpResponse {
        val httpResponse = post(USER_ENDPOINT, userRequest.toJson(), apiCredentials)

        if (!httpResponse.statusCode().equals(HttpStatus.HTTP_CREATED)) {
            throw GrispiApiException(httpResponse.statusCode(), httpResponse.bodyText())
        }

        return httpResponse
    }

    fun createTicket(ticketRequest: TicketRequest, apiCredentials: GrispiApiCredentials): HttpResponse {
        val httpResponse = post(TICKET_ENDPOINT, ticketRequest.toJson(), apiCredentials)

        if (!httpResponse.statusCode().equals(HttpStatus.HTTP_CREATED)) {
            throw GrispiApiException(httpResponse.statusCode(), httpResponse.bodyText())
        }

        return httpResponse
    }

    fun createComments(commentRequests: List<CommentRequest>?, apiCredentials: GrispiApiCredentials): HttpResponse {
        val commentRequestJson = JsonSerializer().deep(true).serialize(commentRequests)

        val httpResponse = post("${TICKET_ENDPOINT}/${commentRequests?.first()?.ticketKey}${TICKET_COMMENT_ENDPOINT}", commentRequestJson, apiCredentials)

        if (!httpResponse.statusCode().equals(HttpStatus.HTTP_CREATED)) {
            throw GrispiApiException(httpResponse.statusCode(), httpResponse.bodyText())
        }

        return httpResponse
    }

    private fun post(endpoint: String, requestBody: String, apiCredentials: GrispiApiCredentials): HttpResponse {
        return HttpRequest
            .post("${HOST}${endpoint}")
            .bodyText(requestBody, "")
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
//            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${apiCredentials.token}")
            .header(TENANT_ID_HEADER_NAME, apiCredentials.tenantId)
            .timeout(10000)
            .send()
    }

}

class GrispiApiException(
    val statusCode: Int? = null,
    val exceptionMessage: String? = null
) : RuntimeException()
