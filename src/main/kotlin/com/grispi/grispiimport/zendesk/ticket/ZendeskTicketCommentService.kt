package com.grispi.grispiimport.zendesk.ticket

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.zendesk.*
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

@Service
class ZendeskTicketCommentService(
    private val zendeskApi: ZendeskApi,
    private val ticketRepository: ZendeskTicketRepository,
    private val ticketCommentRepository: ZendeskTicketCommentRepository,
    private val apiLimitWatcher: ApiLimitWatcher,
    private val commentMapRepository: CommentMapRepository,
) {

    companion object {
        const val RESOURCE_NAME = "comment"
        const val PAGE_SIZE = 1000
    }

    fun fetch(operationId: String, zendeskApiCredentials: ZendeskApiCredentials, startingFrom: Int? = 0) {
        val commentedTicketsCount = ticketRepository.getCommentedTicketsCount()

        println("ticket comment fetch process is started for ${commentedTicketsCount} tickets at: ${LocalDateTime.now()}")

        val to = BigDecimal(commentedTicketsCount).divide(BigDecimal(PAGE_SIZE), RoundingMode.UP).toInt()
        for (index in (startingFrom)!!.rangeTo(to)) {
            val commentedTickets = ticketRepository.findCommentedTickets(PageRequest.of(index, PAGE_SIZE))

            for (ticket in commentedTickets) {
                if (apiLimitWatcher.isApiUnavailable(operationId)) {
                    commentMapRepository.save(CommentMap(ticket.id, apiAvailable = false))
                    val retryAfterFor = apiLimitWatcher.getRetryAfterFor(operationId)
                    println("sleeping user thread for ${retryAfterFor} page ${index}")
                    CompletableFuture.supplyAsync(
                        { fetch(operationId, zendeskApiCredentials, index) },
                        CompletableFuture.delayedExecutor(retryAfterFor, TimeUnit.SECONDS)
                    )

                    break;
                }

                commentMapRepository.save(CommentMap(ticket.id, callingZendesk = true))
                zendeskApi
                    .getTicketComments(ticket.id, zendeskApiCredentials, this::save)
                    .thenApply { comments -> save(comments, operationId, ticket.id) }
                    .handle { t, exception ->
                        exception.printStackTrace()
                        commentMapRepository.save(CommentMap(ticket.id, exception = exception.stackTraceToString()))
                    }

            }

            println("ticket comments fetched for page: ${index}")
        }
    }

    fun save(comments: List<ZendeskComment>, operationId: String, ticketId: Long): List<ZendeskComment> {

        if (comments.isEmpty()) {
            println("no comment found. ")
            return emptyList()
        }
        else {
            println("${comments.count()} comments fetched")
        }

        comments.forEach {
            it.operationId = operationId
            it.ticketId = ticketId
        }

        return ticketCommentRepository.saveAll(comments)
    }

}