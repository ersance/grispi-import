package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.GrispiImportService
import com.grispi.grispiimport.zendesk.group.GroupImportService
import com.grispi.grispiimport.zendesk.organization.OrganizationImportService
import com.grispi.grispiimport.zendesk.ticket.TicketCommentImportService
import com.grispi.grispiimport.zendesk.ticket.TicketImportService
import com.grispi.grispiimport.zendesk.ticketfield.TicketFieldImportService
import com.grispi.grispiimport.zendesk.user.UserImportService
import com.grispi.grispiimport.zendesk.userfield.UserFieldImportService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.annotation.Id
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
class ZendeskImportService(
    @Autowired val zendeskApi: ZendeskApi,
    @Autowired val zendeskImportRepository: ZendeskImportRepository,
    @Autowired val organizationImportService: OrganizationImportService,
    @Autowired val groupImportService: GroupImportService,
    @Autowired val ticketFieldImportService: TicketFieldImportService,
    @Autowired val userFieldImportService: UserFieldImportService,
    @Autowired val userImportService: UserImportService,
    @Autowired val ticketImportService: TicketImportService,
    @Autowired val ticketCommentImportService: TicketCommentImportService,
    @Autowired val grispiImportService: GrispiImportService,
) {

    fun import(zendeskImportRequest: ZendeskImportRequest): ZendeskTenantImport {
        val zendeskTenantImport =
            zendeskImportRepository.save(ZendeskTenantImport(zendeskImportRequest.grispiApiCredentials.tenantId, zendeskImportRequest.zendeskApiCredentials.subdomain))

        CompletableFuture
            .supplyAsync { import(zendeskTenantImport.id, zendeskImportRequest.zendeskApiCredentials) }
            .thenRun { grispiImportService.import(zendeskTenantImport.id, zendeskImportRequest.grispiApiCredentials) }

        return zendeskTenantImport
    }

    fun fetchResourceCounts(zendeskApiCredentials: ZendeskApiCredentials): MutableMap<String, Int> {
        val countMap: MutableMap<String, Int> = mutableMapOf()

        println("fetching resource counts...")

        val orgCount = CompletableFuture.supplyAsync { zendeskApi.getOrganizationCount(zendeskApiCredentials) }.thenApply { countMap.put("organization", it) }
        val groupCount = CompletableFuture.supplyAsync { zendeskApi.getGroupCount(zendeskApiCredentials) }.thenApply { countMap.put("group", it) }
        val userCount = CompletableFuture.supplyAsync { zendeskApi.getUserCount(zendeskApiCredentials) }.thenApply { countMap.put("user", it) }
        val ticketCount = CompletableFuture.supplyAsync { zendeskApi.getTicketCount(zendeskApiCredentials) }.thenApply { countMap.put("ticket", it) }

        CompletableFuture.allOf(orgCount, groupCount, userCount, ticketCount).get()
        println("resource counts fetched.. $countMap")

        return countMap
    }

    private fun import(operationId: String, zendeskApiCredentials: ZendeskApiCredentials) {
        organizationImportService.import(operationId, zendeskApiCredentials)
        groupImportService.import(operationId, zendeskApiCredentials)
        ticketFieldImportService.import(operationId, zendeskApiCredentials)
        userFieldImportService.import(operationId, zendeskApiCredentials)
        userImportService.import(operationId, zendeskApiCredentials)
        ticketImportService.import(operationId, zendeskApiCredentials)
        ticketCommentImportService.import(operationId, zendeskApiCredentials)

        println("resources imported..")
    }

}

class ZendeskTenantImport(val grispiTenantId: String, val zendeskTenantId: String) {

    @Id
    val id: String = UUID.randomUUID().toString()

    val createdAt: Long = System.currentTimeMillis()

}
