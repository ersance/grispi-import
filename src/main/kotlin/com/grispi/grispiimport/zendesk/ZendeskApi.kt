package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiApiCredentials
import jodd.http.HttpRequest
import jodd.http.HttpResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service

@Service
class ZendeskApi {

    companion object {
        const val HOST: String = "zendesk.com/api/v2"

        const val CUSTOM_FIELD_ENDPOINT: String = "/ticket_fields"
        const val ORGANIZATIONS_ENDPOINT: String = "/organizations"
        const val GROUPS_ENDPOINT: String = "/groups"
        const val USERS_ENDPOINT: String = "/users"
        const val TICKETS_ENDPOINT: String = "/tickets"
    }

    fun getTicketFields(apiCredentials: ZendeskApiCredentials): HttpResponse {
        return get(CUSTOM_FIELD_ENDPOINT, apiCredentials)
    }

    fun getUsers(apiCredentials: ZendeskApiCredentials): HttpResponse {
        return get(USERS_ENDPOINT, apiCredentials)
    }

    fun getTickets(apiCredentials: ZendeskApiCredentials): HttpResponse {
        return get(TICKETS_ENDPOINT, apiCredentials)
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