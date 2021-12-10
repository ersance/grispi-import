package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiApiCredentials
import jodd.http.HttpRequest
import jodd.http.HttpResponse
import jodd.http.HttpStatus
import jodd.json.JsonParser
import jodd.json.meta.JSON
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

// TODO: 9.12.2021 handle pagination
// TODO: 9.12.2021 handle exceptions (throw)
@Service
class ZendeskApi(@Autowired val zendeskDateConverter: ZendeskDateConverter) {

    companion object {
        const val HOST: String = "zendesk.com/api/v2"

        const val CUSTOM_FIELD_ENDPOINT: String = "/ticket_fields"
        const val USER_FIELD_ENDPOINT: String = "/user_fields"
        const val ORGANIZATIONS_ENDPOINT: String = "/organizations"
        const val GROUPS_ENDPOINT: String = "/groups"
        const val USERS_ENDPOINT: String = "/users"
        const val TICKETS_ENDPOINT: String = "/tickets"
        const val TICKET_COUNT_ENDPOINT: String = "${TICKETS_ENDPOINT}/count"
        const val TICKET_COMMENTS_ENDPOINT: String = "/comments"
    }

    fun getTicketFields(apiCredentials: ZendeskApiCredentials): List<ZendeskTicketField> {
        val response = get(CUSTOM_FIELD_ENDPOINT, apiCredentials)

        return JsonParser()
            .parse(response.bodyText(), ZendeskTicketFields::class.java)
            .ticketFields
    }

    fun getUserFields(apiCredentials: ZendeskApiCredentials): List<ZendeskUserField> {
        val response = get(USER_FIELD_ENDPOINT, apiCredentials)

        return JsonParser()
            .parse(response.bodyText(), ZendeskUserFields::class.java)
            .userFields
    }

    fun getUsers(apiCredentials: ZendeskApiCredentials): List<ZendeskUser> {
        val response = get(USERS_ENDPOINT, apiCredentials)

        return JsonParser()
            .parse(response.bodyText(), ZendeskUsers::class.java)
            .users
    }

    // TODO: 7.12.2021 handle zendesk api exception
    fun getTickets(apiCredentials: ZendeskApiCredentials, zendeskPageParams: ZendeskPageParams): CompletableFuture<List<ZendeskTicket>> {
        return HttpRequest
            .get("https://${apiCredentials.subdomain}.${HOST}${TICKETS_ENDPOINT}")
            .query("page", zendeskPageParams.page).query("per_page", zendeskPageParams.perPage)
            .basicAuthentication("${apiCredentials.email}/token", apiCredentials.token)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .sendAsync()
            .thenApply { response ->
                if (!response.statusCode().equals(HttpStatus.HTTP_OK)) println("${response.statusCode()}____${response.bodyText()}")

                JsonParser()
                    .withValueConverter("tickets.values.createdAt", zendeskDateConverter)
                    .parse(response.bodyText(), ZendeskTickets::class.java)
                    .tickets
            }
    }

    fun getTicketCount(apiCredentials: ZendeskApiCredentials): Int {
        val response = get(TICKET_COUNT_ENDPOINT, apiCredentials)
        return JsonParser().parseAsJsonObject(response.bodyText())
            .getJsonObject("count")
            .getValue("value")
    }

    fun getTicketComments(ticketsZendeskId: Long, apiCredentials: ZendeskApiCredentials): List<ZendeskComment> {
        val response = HttpRequest
            .get("https://${apiCredentials.subdomain}.${HOST}${TICKETS_ENDPOINT}/${ticketsZendeskId}${TICKET_COMMENTS_ENDPOINT}")
            .basicAuthentication("${apiCredentials.email}/token", apiCredentials.token)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .send()

        return JsonParser()
            .withValueConverter("comments.values.createdAt", zendeskDateConverter)
            .parse(response.bodyText(), ZendeskComments::class.java)
            .comments
    }

    fun getOrganizations(apiCredentials: ZendeskApiCredentials): List<ZendeskOrganization> {
        val response = get(ORGANIZATIONS_ENDPOINT, apiCredentials)

        return JsonParser()
            .parse(response.bodyText(), ZendeskOrganizations::class.java)
            .organizations
    }

    fun getGroups(apiCredentials: ZendeskApiCredentials): List<ZendeskGroup> {
        val response = get(GROUPS_ENDPOINT, apiCredentials)

        return JsonParser()
            .parse(response.bodyText(), ZendeskGroups::class.java)
            .groups
    }

    private fun get(endpoint: String, apiCredentials: ZendeskApiCredentials): HttpResponse {
        return HttpRequest
            .get("https://${apiCredentials.subdomain}.${HOST}${endpoint}")
            .basicAuthentication("${apiCredentials.email}/token", apiCredentials.token)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .send()
    }

}
class ZendeskApiException(
    val statusCode: Int? = null,
    val exceptionMessage: String? = null
) : RuntimeException()

data class ZendeskPageParams(val page: Int = 0, val perPage: Int = 100)
