package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.utils.CalculateTimeSpent
import com.grispi.grispiimport.zendesk.group.ZendeskGroupService
import com.grispi.grispiimport.zendesk.organization.ZendeskOrganizationService
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketCommentService
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketService
import com.grispi.grispiimport.zendesk.ticketfield.ZendeskTicketFieldService
import com.grispi.grispiimport.zendesk.ticketform.ZendeskTicketFormService
import com.grispi.grispiimport.zendesk.user.ZendeskUserService
import com.grispi.grispiimport.zendesk.userfield.ZendeskUserFieldService
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

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

    @CalculateTimeSpent
    fun fetchResources(operationId: String, zendeskApiCredentials: ZendeskApiCredentials) {
//        zendeskOrganizationService.fetch(operationId, zendeskApiCredentials)
//        zendeskGroupService.fetch(operationId, zendeskApiCredentials)
        zendeskTicketFieldService.fetch(operationId, zendeskApiCredentials)
//        zendeskTicketFormService.fetch(operationId, zendeskApiCredentials)
//        zendeskUserFieldService.fetch(operationId, zendeskApiCredentials)
//        zendeskUserService.fetch(operationId, zendeskApiCredentials)
//        zendeskTicketService.fetch(operationId, zendeskApiCredentials)
//        zendeskTicketCommentService.fetch(operationId, zendeskApiCredentials)
    }

    fun fetchResourceCounts(zendeskApiCredentials: ZendeskApiCredentials): MutableMap<String, Int> {
        val countMap: MutableMap<String, Int> = mutableMapOf()

        println("fetching resource counts...")

        val orgCount = CompletableFuture.supplyAsync { zendeskApi.getOrganizationCount(zendeskApiCredentials) }.thenApply { countMap.put("organizations", it) }
        val groupCount = CompletableFuture.supplyAsync { zendeskApi.getGroupCount(zendeskApiCredentials) }.thenApply { countMap.put("groups", it) }
        val ticketFieldCount = CompletableFuture.supplyAsync { zendeskApi.getTicketFieldCount(zendeskApiCredentials) }.thenApply { countMap.put("ticketFields", it) }
        val userFieldCount = CompletableFuture.supplyAsync { zendeskApi.getUserFieldCount(zendeskApiCredentials) }.thenApply { countMap.put("userFields", it) }
        val userCount = CompletableFuture.supplyAsync { zendeskApi.getUserCount(zendeskApiCredentials) }.thenApply { countMap.put("users", it) }
        val deletedUserCount = CompletableFuture.supplyAsync { zendeskApi.getDeletedUserCount(zendeskApiCredentials) }.thenApply { countMap.put("deletedUsers", it) }
        val ticketCount = CompletableFuture.supplyAsync { zendeskApi.getTicketCount(zendeskApiCredentials) }.thenApply { countMap.put("tickets", it) }

        CompletableFuture.allOf(orgCount, groupCount, ticketFieldCount, userFieldCount, userCount, deletedUserCount, ticketCount).get()
        println("resource counts fetched.. $countMap")

        return countMap
    }

}