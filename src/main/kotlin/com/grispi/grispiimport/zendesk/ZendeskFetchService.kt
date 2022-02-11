package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.utils.CalculateTimeSpent
import com.grispi.grispiimport.zendesk.group.ZendeskGroupService
import com.grispi.grispiimport.zendesk.organization.ResourceCount
import com.grispi.grispiimport.zendesk.organization.ZendeskOrganizationService
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketCommentService
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketService
import com.grispi.grispiimport.zendesk.ticketfield.ZendeskTicketFieldService
import com.grispi.grispiimport.zendesk.ticketform.ZendeskTicketFormService
import com.grispi.grispiimport.zendesk.user.ZendeskUserService
import com.grispi.grispiimport.zendesk.userfield.ZendeskUserFieldService
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.stream.Stream
import kotlin.reflect.KFunction1

@Service
class ZendeskFetchService(
    private val zendeskApi: ZendeskApi,
    private val zendeskOrganizationService: ZendeskOrganizationService,
    private val zendeskGroupService: ZendeskGroupService,
    private val zendeskTicketFieldService: ZendeskTicketFieldService,
    private val zendeskTicketFormService: ZendeskTicketFormService,
    private val zendeskUserFieldService: ZendeskUserFieldService,
    private val zendeskUserService: ZendeskUserService,
    private val zendeskTicketService: ZendeskTicketService,
    private val zendeskTicketCommentService: ZendeskTicketCommentService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        val AVAILABLE_RESOURCES = setOf(
            ZendeskOrganizationService.RESOURCE_NAME,
            ZendeskGroupService.RESOURCE_NAME,
            ZendeskTicketFieldService.RESOURCE_NAME,
            ZendeskTicketFormService.RESOURCE_NAME,
            ZendeskUserFieldService.RESOURCE_NAME,
            ZendeskUserService.RESOURCE_NAME,
            ZendeskTicketService.RESOURCE_NAME,
            ZendeskTicketCommentService.RESOURCE_NAME
        )
    }

    @CalculateTimeSpent
    fun fetchResources(operationId: String, zendeskApiCredentials: ZendeskApiCredentials) {
        MDC.put("operationId", operationId)
        zendeskOrganizationService.fetch(operationId, zendeskApiCredentials)
        zendeskGroupService.fetch(operationId, zendeskApiCredentials)
        zendeskGroupService.fetchGroupMemberships(operationId, zendeskApiCredentials)
        zendeskTicketFieldService.fetch(operationId, zendeskApiCredentials)
        zendeskTicketFormService.fetch(operationId, zendeskApiCredentials)
        zendeskUserFieldService.fetch(operationId, zendeskApiCredentials)
        zendeskUserService.fetch(operationId, zendeskApiCredentials)
        zendeskTicketService.fetch(operationId, zendeskApiCredentials)
        zendeskTicketCommentService.fetch(operationId, zendeskApiCredentials)
    }

    fun fetchResourceCounts(zendeskApiCredentials: ZendeskApiCredentials): MutableMap<String, Long> {
        val countMap: MutableMap<String, Long> = mutableMapOf()

        logger.info("fetching resource counts...")

        val orgCount = CompletableFuture.supplyAsync { zendeskApi.getOrganizationCount(zendeskApiCredentials) }.thenApply { countMap.put("organizations", it) }
        val groupCount = CompletableFuture.supplyAsync { zendeskApi.getGroupCount(zendeskApiCredentials) }.thenApply { countMap.put("groups", it) }
        val ticketFieldCount = CompletableFuture.supplyAsync { zendeskApi.getTicketFieldCount(zendeskApiCredentials) }.thenApply { countMap.put("ticketFields", it) }
        val userFieldCount = CompletableFuture.supplyAsync { zendeskApi.getUserFieldCount(zendeskApiCredentials) }.thenApply { countMap.put("userFields", it) }
        val userCount = CompletableFuture.supplyAsync { zendeskApi.getUserCount(zendeskApiCredentials) }.thenApply { countMap.put("users", it) }
        val deletedUserCount = CompletableFuture.supplyAsync { zendeskApi.getDeletedUserCount(zendeskApiCredentials) }.thenApply { countMap.put("deletedUsers", it) }
        val ticketCount = CompletableFuture.supplyAsync { zendeskApi.getTicketCount(zendeskApiCredentials) }.thenApply { countMap.put("tickets", it) }

        CompletableFuture.allOf(orgCount, groupCount, ticketFieldCount, userFieldCount, userCount, deletedUserCount, ticketCount).get()
        logger.info("resource counts fetched.. $countMap")

        return countMap
    }

    fun fetchedResourcesCounts(operationId: String, zendeskApiCredentials: ZendeskApiCredentials): List<ResourceCount> {
        val counts: MutableList<ResourceCount> = mutableListOf()
        val orgCount = CompletableFuture.supplyAsync { zendeskOrganizationService.counts(operationId, zendeskApiCredentials) }.thenApply { counts.add(it) }
        val groupCount = CompletableFuture.supplyAsync { zendeskGroupService.counts(operationId, zendeskApiCredentials) }.thenApply { counts.add(it) }
        val ticketFieldCount = CompletableFuture.supplyAsync { zendeskTicketFieldService.counts(operationId, zendeskApiCredentials) }.thenApply { counts.add(it) }
        val ticketFormCount = CompletableFuture.supplyAsync { zendeskTicketFormService.counts(operationId, zendeskApiCredentials) }.thenApply { counts.add(it) }
        val userFieldCount = CompletableFuture.supplyAsync { zendeskUserFieldService.counts(operationId, zendeskApiCredentials) }.thenApply { counts.add(it) }
        val userCount = CompletableFuture.supplyAsync { zendeskUserService.usersCount(operationId, zendeskApiCredentials) }.thenApply { counts.add(it) }
        val deletedUserCount = CompletableFuture.supplyAsync { zendeskUserService.deletedUsersCount(operationId, zendeskApiCredentials) }.thenApply { counts.add(it) }
        val ticketCount = CompletableFuture.supplyAsync { zendeskTicketService.counts(operationId, zendeskApiCredentials) }.thenApply { counts.add(it) }
        // todo: val ticketCommentCount = CompletableFuture.supplyAsync { zendeskTicketCommentService.counts(operationId) }.thenApply { counts.add(it) }

        CompletableFuture.allOf(orgCount, groupCount, ticketFieldCount, ticketFormCount, userFieldCount, userCount, deletedUserCount, ticketCount).get()

        return counts
    }

}