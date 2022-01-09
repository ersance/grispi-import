package com.grispi.grispiimport.grispi

import jodd.http.HttpRequest
import jodd.http.HttpResponse
import jodd.http.HttpStatus
import jodd.json.JsonException
import jodd.json.JsonParser
import jodd.json.JsonSerializer
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.lang.IllegalStateException
import java.util.concurrent.CompletableFuture

@Service
class GrispiApi {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val HOST: String = "http://grispi.com:8080"
        const val TENANT_ID_HEADER_NAME: String = "tenantId"

        const val ORGANIZATIONS_ENDPOINT: String = "/import/organizations"
        const val GROUPS_ENDPOINT: String = "/import/groups"
        const val CUSTOM_FIELD_ENDPOINT: String = "/import/custom-fields"
        const val USER_FIELD_ENDPOINT: String = "/import/user-fields"
        const val USER_ENDPOINT: String = "/import/users"
        const val DELETED_USER_ENDPOINT: String = "/import/deleted-users"
        const val TICKET_ENDPOINT: String = "/import/tickets"
        const val TICKET_COMMENT_ENDPOINT: String = "/comments"
        const val TICKET_FORM_ENDPOINT: String = "/import/ticket-forms"
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

    fun createTicketField(grispiTicketFieldRequest: GrispiTicketFieldRequest, apiCredentials: GrispiApiCredentials): HttpResponse {
        val httpResponse = post(CUSTOM_FIELD_ENDPOINT, grispiTicketFieldRequest.toJson(), apiCredentials)

        if (!httpResponse.statusCode().equals(HttpStatus.HTTP_CREATED)) {
            throw GrispiApiException(httpResponse.statusCode(), httpResponse.bodyText())
        }

        return httpResponse
    }

    fun createTicketForm(grispiTicketFormRequest: GrispiTicketFormRequest, apiCredentials: GrispiApiCredentials): HttpResponse {
        val httpResponse = post(TICKET_FORM_ENDPOINT, grispiTicketFormRequest.toJson(), apiCredentials)

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

    fun createUser(userRequest: UserRequest, apiCredentials: GrispiApiCredentials): CompletableFuture<String> {
        return postAsync(USER_ENDPOINT, userRequest.toJson(), apiCredentials)
            .thenApply { response ->
                if (response.statusCode().equals(HttpStatus.HTTP_CREATED)) {
                    return@thenApply response.bodyText()
                }
                else if (response.statusCode().equals(HttpStatus.HTTP_CONFLICT)) {
                    try {
                        val errorMessageInBody = JsonParser().parseAsJsonObject(response.bodyText()).getValue("message") as String

                        throw GrispiUserConflictedException(errorMessageInBody)
                    }
                    catch (ex: JsonException) {
                        logger.info(response.bodyText())
                        throw GrispiUserConflictedException(response.bodyText())
                    }
                }
                else {
                    throw GrispiApiException(response.statusCode(), response.bodyText())
                }
            }
    }

    fun createDeletedUser(deletedUserRequest: DeletedUserRequest, apiCredentials: GrispiApiCredentials): CompletableFuture<String> {
        return postAsync(DELETED_USER_ENDPOINT, deletedUserRequest.toJson(), apiCredentials)
            .thenApply { response ->
                if (response.statusCode().equals(HttpStatus.HTTP_CREATED)) {
                    return@thenApply response.bodyText()
                }
                else if (response.statusCode().equals(HttpStatus.HTTP_CONFLICT)) {
                    try {
                        val errorMessageInBody = JsonParser().parseAsJsonObject(response.bodyText()).getValue("message", String)

                        throw GrispiUserConflictedException(errorMessageInBody.toString())
                    }
                    catch (ex: JsonException) {
                        logger.info(response.bodyText())
                        throw GrispiUserConflictedException(response.bodyText())
                    }
                }
                else {
                    throw GrispiApiException(response.statusCode(), response.bodyText())
                }
            }
    }

    fun createTicketAsync(ticketRequest: TicketRequest, apiCredentials: GrispiApiCredentials): CompletableFuture<String> {
        return postAsync(TICKET_ENDPOINT, ticketRequest.toJson(), apiCredentials)
            .thenApply { response ->
                if (response.statusCode().equals(HttpStatus.HTTP_CREATED)) {
                    return@thenApply response.bodyText()
                }
                else {
                    throw GrispiApiException(response.statusCode(), response.bodyText())
                }
            }
    }

    fun createTicket(ticketRequest: TicketRequest, apiCredentials: GrispiApiCredentials): String {
        val httpResponse = post(TICKET_ENDPOINT, ticketRequest.toJson(), apiCredentials)

        if (!httpResponse.statusCode().equals(HttpStatus.HTTP_CREATED)) {
            throw GrispiApiException(httpResponse.statusCode(), httpResponse.bodyText())
        }

        return httpResponse.bodyText()
    }

    fun createCommentsAsync(commentRequests: List<CommentRequest>?, apiCredentials: GrispiApiCredentials): CompletableFuture<String> {
        val commentRequestJson = JsonSerializer().deep(true).serialize(commentRequests)
        return postAsync("${TICKET_ENDPOINT}/${commentRequests?.first()?.ticketKey}${TICKET_COMMENT_ENDPOINT}", commentRequestJson, apiCredentials)
            .thenApply { response ->
                if (response.statusCode().equals(HttpStatus.HTTP_CREATED)) {
                    return@thenApply response.bodyText()
                }
                else {
                    throw GrispiApiException(response.statusCode(), response.bodyText())
                }
            }
    }

    fun createComments(commentRequests: List<CommentRequest>?, apiCredentials: GrispiApiCredentials): String {
        val commentRequestJson = JsonSerializer().deep(true).serialize(commentRequests)

        val httpResponse = post("${TICKET_ENDPOINT}/${commentRequests?.first()?.ticketKey}${TICKET_COMMENT_ENDPOINT}", commentRequestJson, apiCredentials)

        if (!httpResponse.statusCode().equals(HttpStatus.HTTP_CREATED)) {
            throw GrispiApiException(httpResponse.statusCode(), httpResponse.bodyText())
        }

        return httpResponse.bodyText()
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

    private fun postAsync(endpoint: String, requestBody: String, apiCredentials: GrispiApiCredentials): CompletableFuture<HttpResponse> {
        return HttpRequest
            .post("${HOST}${endpoint}")
            .bodyText(requestBody, "")
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
//            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${apiCredentials.token}")
            .header(TENANT_ID_HEADER_NAME, apiCredentials.tenantId)
            .timeout(10000)
            .sendAsync()
    }

}

class GrispiApiException(
    val statusCode: Int? = null,
    val exceptionMessage: String? = null
) : RuntimeException()

class GrispiUserConflictedException(val conflictedUserId: String? = null) : RuntimeException()
