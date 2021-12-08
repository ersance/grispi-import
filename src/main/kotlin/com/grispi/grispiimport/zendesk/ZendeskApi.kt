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

@Service
class ZendeskApi(@Autowired val zendeskDateConverter: ZendeskDateConverter) {

    companion object {
        const val HOST: String = "zendesk.com/api/v2"

        const val CUSTOM_FIELD_ENDPOINT: String = "/ticket_fields"
        const val ORGANIZATIONS_ENDPOINT: String = "/organizations"
        const val GROUPS_ENDPOINT: String = "/groups"
        const val USERS_ENDPOINT: String = "/users"
        const val TICKETS_ENDPOINT: String = "/tickets"
        const val TICKET_COUNT_ENDPOINT: String = "${TICKETS_ENDPOINT}/count"
    }

    fun getTicketFields(apiCredentials: ZendeskApiCredentials): HttpResponse {
        return get(CUSTOM_FIELD_ENDPOINT, apiCredentials)
    }

    fun getUsers(apiCredentials: ZendeskApiCredentials): HttpResponse {
        return get(USERS_ENDPOINT, apiCredentials)
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

    fun getOrganizations(apiCredentials: ZendeskApiCredentials): HttpResponse {
        return get(ORGANIZATIONS_ENDPOINT, apiCredentials)
    }

    fun getGroups(apiCredentials: ZendeskApiCredentials): HttpResponse {
        return get(GROUPS_ENDPOINT, apiCredentials)
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
