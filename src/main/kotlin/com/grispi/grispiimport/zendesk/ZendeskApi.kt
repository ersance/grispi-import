package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.zendesk.group.ZendeskGroup
import com.grispi.grispiimport.zendesk.group.ZendeskGroupMembership
import com.grispi.grispiimport.zendesk.group.ZendeskGroupMemberships
import com.grispi.grispiimport.zendesk.group.ZendeskGroups
import com.grispi.grispiimport.zendesk.organization.ZendeskOrganization
import com.grispi.grispiimport.zendesk.organization.ZendeskOrganizations
import com.grispi.grispiimport.zendesk.ticketfield.ZendeskTicketField
import com.grispi.grispiimport.zendesk.ticketfield.ZendeskTicketFields
import com.grispi.grispiimport.zendesk.ticketform.ZendeskTicketForm
import com.grispi.grispiimport.zendesk.ticketform.ZendeskTicketForms
import jodd.http.HttpRequest
import jodd.http.HttpResponse
import jodd.json.JsonParser
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.reflect.KFunction3

@Service
class ZendeskApi(
    private val zendeskDateConverter: ZendeskDateConverter,
    private val apiLimitWatcher: ApiLimitWatcher
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val HOST: String = "zendesk.com/api/v2"
        const val PAGE_SIZE = 100 // 100 is default size on zendesk

        const val CUSTOM_FIELD_ENDPOINT: String = "/ticket_fields"
        const val USER_FIELD_ENDPOINT: String = "/user_fields"
        const val ORGANIZATIONS_ENDPOINT: String = "/organizations"
        const val GROUPS_ENDPOINT: String = "/groups"
        const val GROUP_MEMBERSHIP_ENDPOINT: String = "/group_memberships"
        const val USERS_ENDPOINT: String = "/users"
        const val DELETED_USERS_ENDPOINT: String = "/deleted_users"
        const val TICKETS_ENDPOINT: String = "/tickets"
        const val TICKET_COMMENTS_ENDPOINT: String = "/comments"
        const val TICKET_FORM_ENDPOINT: String = "/ticket_forms"

        val scheduledExecutorService: ScheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors())
    }

    // ORGANIZATIONS
    fun getOrganizationCount(apiCredentials: ZendeskApiCredentials): Long {
        return getResourceCount("${ORGANIZATIONS_ENDPOINT}/count", apiCredentials)
    }

    fun getOrganizations(apiCredentials: ZendeskApiCredentials, zendeskPageParams: ZendeskPageParams): List<ZendeskOrganization> {
        val response = HttpRequest
            .get("https://${apiCredentials.subdomain}.${HOST}${ORGANIZATIONS_ENDPOINT}")
            .query("page", zendeskPageParams.page).query("per_page", zendeskPageParams.perPage)
            .basicAuthentication("${apiCredentials.email}/token", apiCredentials.token)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .send()

        return JsonParser()
            .parse(response.bodyText(), ZendeskOrganizations::class.java)
            .organizations
    }

    // GROUPS
    fun getGroups(apiCredentials: ZendeskApiCredentials, zendeskPageParams: ZendeskPageParams): List<ZendeskGroup> {
        val response = HttpRequest
            .get("https://${apiCredentials.subdomain}.${HOST}${GROUPS_ENDPOINT}")
            .query("page", zendeskPageParams.page).query("per_page", zendeskPageParams.perPage)
            .basicAuthentication("${apiCredentials.email}/token", apiCredentials.token)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .send()

        return JsonParser()
            .parse(response.bodyText(), ZendeskGroups::class.java)
            .groups
    }

    fun getGroupMemberships(apiCredentials: ZendeskApiCredentials, zendeskPageParams: ZendeskPageParams): List<ZendeskGroupMembership> {
        val response = HttpRequest
            .get("https://${apiCredentials.subdomain}.${HOST}${GROUP_MEMBERSHIP_ENDPOINT}")
            .query("page", zendeskPageParams.page).query("per_page", zendeskPageParams.perPage)
            .basicAuthentication("${apiCredentials.email}/token", apiCredentials.token)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .send()

        return JsonParser()
            .parse(response.bodyText(), ZendeskGroupMemberships::class.java)
            .groups
    }

    fun getGroupCount(apiCredentials: ZendeskApiCredentials): Long {
        return getResourceCount("${GROUPS_ENDPOINT}/count", apiCredentials)
    }

    fun getGroupMembershipCount(apiCredentials: ZendeskApiCredentials): Long {
        val response = get(GROUP_MEMBERSHIP_ENDPOINT, apiCredentials)
        return JsonParser().parseAsJsonObject(response.bodyText()).getValue("count")
    }

    // TICKET FIELDS
    fun getTicketFields(apiCredentials: ZendeskApiCredentials): List<ZendeskTicketField> {
        val response = HttpRequest
            .get("https://${apiCredentials.subdomain}.${HOST}${CUSTOM_FIELD_ENDPOINT}")
            .basicAuthentication("${apiCredentials.email}/token", apiCredentials.token)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .send()

        return JsonParser()
            .parse(response.bodyText(), ZendeskTicketFields::class.java)
            .ticketFields
    }

    fun getTicketFieldCount(apiCredentials: ZendeskApiCredentials): Long {
        return getResourceCount("${CUSTOM_FIELD_ENDPOINT}/count", apiCredentials)
    }

    // USER FIELDS
    fun getUserFields(apiCredentials: ZendeskApiCredentials): List<ZendeskUserField> {
        val response = HttpRequest
            .get("https://${apiCredentials.subdomain}.${HOST}${USER_FIELD_ENDPOINT}")
            .basicAuthentication("${apiCredentials.email}/token", apiCredentials.token)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .send()

        return JsonParser()
            .parse(response.bodyText(), ZendeskUserFields::class.java)
            .userFields
    }

    fun getUserFieldCount(apiCredentials: ZendeskApiCredentials): Long {
        return getResourceCount("${USER_FIELD_ENDPOINT}/count", apiCredentials)
    }

    // TICKET FORMS
    fun getTicketForms(apiCredentials: ZendeskApiCredentials): List<ZendeskTicketForm> {
        val response = HttpRequest
            .get("https://${apiCredentials.subdomain}.${HOST}${TICKET_FORM_ENDPOINT}")
            .basicAuthentication("${apiCredentials.email}/token", apiCredentials.token)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .send()

        return JsonParser()
            .withValueConverter("ticketForms.values.createdAt", zendeskDateConverter)
            .parse(response.bodyText(), ZendeskTicketForms::class.java)
            .ticketForms
    }

    fun getTicketFormsCount(apiCredentials: ZendeskApiCredentials): Long {
        val response = get(TICKET_FORM_ENDPOINT, apiCredentials)
        return JsonParser().parseAsJsonObject(response.bodyText()).getValue("count")
    }

    // USERS
    fun getUsers(
        apiCredentials: ZendeskApiCredentials,
        zendeskPageParams: ZendeskPageParams,
        saveUsers: (List<ZendeskUser>, String) -> List<ZendeskUser>?
    ): CompletableFuture<List<ZendeskUser>> {
        return HttpRequest
            .get("https://${apiCredentials.subdomain}.${HOST}${USERS_ENDPOINT}")
            .query("page", zendeskPageParams.page).query("per_page", zendeskPageParams.perPage)
            .basicAuthentication("${apiCredentials.email}/token", apiCredentials.token)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .sendAsync()
            .thenApply { response ->
                MDC.put("operationId", apiCredentials.operationId)
                val header = response.header("Retry-After")
                if (StringUtils.hasText(header)) {
                    logger.info("Retry after header is $header")
                    apiLimitWatcher.limitExceededFor(apiCredentials.operationId, header.toLong())
                    scheduledExecutorService.schedule(
                        {
                            MDC.put("operationId", apiCredentials.operationId)
                            logger.info("waited for ${header.toLong()} seconds for users... reseting limit")
                            apiLimitWatcher.resetLimitFor(apiCredentials.operationId)
                            getUsers(apiCredentials, zendeskPageParams, saveUsers)
                                .thenApply { users -> saveUsers.invoke(users, apiCredentials.operationId) }
                                .thenApply { users ->
                                    MDC.put("operationId", apiCredentials.operationId)
                                    logger.info("${users?.count()} users saved by SCHEDULED")
                                }
                        }, header.toLong(), TimeUnit.SECONDS)

                    return@thenApply emptyList()
                }

                return@thenApply JsonParser()
                    .parse(response.bodyText(), ZendeskUsers::class.java)
                    .users
            }
    }

    fun getDeletedUsers(
        apiCredentials: ZendeskApiCredentials,
        zendeskPageParams: ZendeskPageParams,
        saveUsers: (List<ZendeskUser>, String) -> List<ZendeskUser>?
    ): CompletableFuture<List<ZendeskUser>> {
        return HttpRequest
            .get("https://${apiCredentials.subdomain}.${HOST}${DELETED_USERS_ENDPOINT}")
            .query("page", zendeskPageParams.page).query("per_page", zendeskPageParams.perPage)
            .basicAuthentication("${apiCredentials.email}/token", apiCredentials.token)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .sendAsync()
            .thenApply { response ->
                MDC.put("operationId", apiCredentials.operationId)
                val header = response.header("Retry-After")
                if (StringUtils.hasText(header)) {
                    logger.info("Retry after header is $header")
                    apiLimitWatcher.limitExceededFor(apiCredentials.operationId, header.toLong())
                    scheduledExecutorService.schedule(
                        {
                            MDC.put("operationId", apiCredentials.operationId)
                            logger.info("waited for ${header.toLong()} seconds for users... reseting limit")
                            apiLimitWatcher.resetLimitFor(apiCredentials.operationId)
                            getUsers(apiCredentials, zendeskPageParams, saveUsers)
                                .thenApply { users -> saveUsers.invoke(users, apiCredentials.operationId) }
                                .thenApply { users ->
                                    MDC.put("operationId", apiCredentials.operationId)
                                    logger.info("${users?.count()} users saved by SCHEDULED")
                                }
                        }, header.toLong(), TimeUnit.SECONDS)

                    return@thenApply emptyList()
                }

                val users = JsonParser()
                    .parse(response.bodyText(), ZendeskDeletedUsers::class.java)
                    .users

                logger.info("fetched user count: ${users.count()}")

                return@thenApply users
            }
    }

    fun getUserCount(apiCredentials: ZendeskApiCredentials): Long {
        return getResourceCount("${USERS_ENDPOINT}/count", apiCredentials)
    }

    fun getDeletedUserCount(apiCredentials: ZendeskApiCredentials): Long {
        val response = get(DELETED_USERS_ENDPOINT, apiCredentials)
        return JsonParser().parseAsJsonObject(response.bodyText()).getValue("count")
    }

    // TICKETS
    fun getTickets(
        apiCredentials: ZendeskApiCredentials,
        zendeskPageParams: ZendeskPageParams,
        saveTickets: (List<ZendeskTicket>, String) -> List<ZendeskTicket>
    ): CompletableFuture<List<ZendeskTicket>> {
        return HttpRequest
            .get("https://${apiCredentials.subdomain}.${HOST}${TICKETS_ENDPOINT}")
            .query("include", "comment_count")
            .query("page", zendeskPageParams.page).query("per_page", zendeskPageParams.perPage)
            .basicAuthentication("${apiCredentials.email}/token", apiCredentials.token)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .sendAsync()
            .thenApply { response ->
                MDC.put("operationId", apiCredentials.operationId)
                val header = response.header("Retry-After")
                if (StringUtils.hasText(header)) {
                    logger.info("Retry after header is $header. waiting $header seconds...")
                    apiLimitWatcher.limitExceededFor(apiCredentials.operationId, header.toLong())
                    scheduledExecutorService.schedule(
                        {
                            MDC.put("operationId", apiCredentials.operationId)
                            logger.info("waited for ${header.toLong()} seconds for tickets... reseting limit")
                            apiLimitWatcher.resetLimitFor(apiCredentials.operationId)
                            getTickets(apiCredentials, zendeskPageParams, saveTickets)
                                .thenApply { tickets -> saveTickets(tickets, apiCredentials.operationId) }
                                .thenApply { tickets ->
                                    MDC.put("operationId", apiCredentials.operationId)
                                    logger.info("${tickets.count()} tickets saved by SCHEDULED")
                                }
                        }, header.toLong(), TimeUnit.SECONDS)

                    return@thenApply emptyList()
                }

                return@thenApply JsonParser()
                    .withValueConverter("tickets.values.createdAt", zendeskDateConverter)
                    .parse(response.bodyText(), ZendeskTickets::class.java)
                    .tickets
            }
    }

    fun getTicketCount(apiCredentials: ZendeskApiCredentials): Long {
        return getResourceCount("${TICKETS_ENDPOINT}/count", apiCredentials)
    }

    fun getTicketComments(
        ticketId: Long,
        apiCredentials: ZendeskApiCredentials,
        kFunction3: KFunction3<List<ZendeskComment>, String, Long, List<ZendeskComment>>
    ): CompletableFuture<List<ZendeskComment>> {
        return HttpRequest
            .get("https://${apiCredentials.subdomain}.${HOST}${TICKETS_ENDPOINT}/${ticketId}${TICKET_COMMENTS_ENDPOINT}")
            .basicAuthentication("${apiCredentials.email}/token", apiCredentials.token)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .sendAsync()
            .thenApply { response ->
                MDC.put("operationId", apiCredentials.operationId)
                val header = response.header("Retry-After")
                if (StringUtils.hasText(header)) {
                    apiLimitWatcher.limitExceededFor(apiCredentials.operationId, header.toLong())
                    scheduledExecutorService.schedule(
                        {
                            MDC.put("operationId", apiCredentials.operationId)
                            logger.info("waited for ${header.toLong()} seconds for ticket comments... reseting limit")
                            apiLimitWatcher.resetLimitFor(apiCredentials.operationId)
                            getTicketComments(ticketId, apiCredentials, kFunction3)
                                .thenApply { comments -> kFunction3.invoke(comments, apiCredentials.operationId, ticketId) }
                                .thenApply { comments ->
                                    MDC.put("operationId", apiCredentials.operationId)
                                    logger.info("${comments.count()} comments saved by SCHEDULED")
                                }
                        }, header.toLong(), TimeUnit.SECONDS)

                    return@thenApply emptyList()
                }

                return@thenApply JsonParser()
                    .withValueConverter("comments.values.createdAt", zendeskDateConverter)
                    .parse(response.bodyText(), ZendeskComments::class.java)
                    .comments
            }
    }

    private fun get(endpoint: String, apiCredentials: ZendeskApiCredentials): HttpResponse {
        return HttpRequest
            .get("https://${apiCredentials.subdomain}.${HOST}${endpoint}")
            .basicAuthentication("${apiCredentials.email}/token", apiCredentials.token)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .send()
    }

    private fun getResourceCount(endpoint: String, apiCredentials: ZendeskApiCredentials): Long {
        val response = get(endpoint, apiCredentials)
        return JsonParser().parseAsJsonObject(response.bodyText())
            .getJsonObject("count")
            .getValue("value")
    }

}
class ZendeskApiException(
    val statusCode: Int? = null,
    val exceptionMessage: String? = null
) : RuntimeException()

data class ZendeskPageParams(val page: Int = 0, val perPage: Int = 100)
