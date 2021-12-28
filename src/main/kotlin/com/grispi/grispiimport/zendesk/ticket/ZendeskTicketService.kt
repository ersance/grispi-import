package com.grispi.grispiimport.zendesk.ticket

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.utils.CalculateTimeSpent
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ZendeskApi.Companion.PAGE_SIZE
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

@Service
class ZendeskTicketService(
    private val zendeskApi: ZendeskApi,
    private val zendeskTicketRepository: ZendeskTicketRepository,
    private val apiLimitWatcher: ApiLimitWatcher,
    private val commentMapRepository: CommentMapRepository
) {

    companion object {
        const val RESOURCE_NAME = "ticket"
    }

    @CalculateTimeSpent
    fun fetch(operationId: String, zendeskApiCredentials: ZendeskApiCredentials, startingFrom: Int? = 1) {
        val ticketCount = zendeskApi.getTicketCount(zendeskApiCredentials)

        println("ticket import process is started for ${ticketCount} tickets at: ${LocalDateTime.now()}")

        val combinedTickets: MutableList<CompletableFuture<List<ZendeskTicket>>> = mutableListOf()
        val to = BigDecimal(ticketCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt()
        for (index in (startingFrom)!!.rangeTo(to)) {
            println("fetching ${index}. page for tickets")

            if (apiLimitWatcher.isApiUnavailable(operationId)) {
                val retryAfterFor = apiLimitWatcher.getRetryAfterFor(operationId)
                println("sleeping user thread for ${retryAfterFor} page ${index}")
                CompletableFuture.supplyAsync(
                    { fetch(operationId, zendeskApiCredentials, index) },
                    CompletableFuture.delayedExecutor(retryAfterFor, TimeUnit.SECONDS)
                )

                break;
            }

            val page = zendeskApi
                .getTickets(zendeskApiCredentials, ZendeskPageParams(index, PAGE_SIZE), this::save)
                .thenApply { tickets ->
                    val commentMaps = tickets.stream().map { t -> CommentMap(t.id) }.collect(Collectors.toList())
                    commentMapRepository.saveAll(commentMaps)
                    return@thenApply tickets
                }
                .thenApply { tickets -> save(tickets, operationId) }

            combinedTickets.add(page)

            page.thenRun { println("tickets imported for page: ${index}") }
        }

        CompletableFuture.allOf(*combinedTickets.toTypedArray()).get(1, TimeUnit.DAYS)

        println("ticket import process is done")
    }

    fun save(tickets: List<ZendeskTicket>, operationId: String): List<ZendeskTicket> {
        tickets.forEach { it.operationId = operationId }

        return zendeskTicketRepository.saveAll(tickets)
    }

}