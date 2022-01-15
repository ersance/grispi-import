package com.grispi.grispiimport

import com.grispi.grispiimport.grispi.GrispiApi
import com.grispi.grispiimport.grispi.GrispiApiCredentials
import com.grispi.grispiimport.grispi.GrispiApiException
import com.grispi.grispiimport.grispi.GrispiTicketCommentImportService
import com.grispi.grispiimport.zendesk.*
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketCommentRepository
import com.grispi.grispiimport.zendesk.ticket.ZendeskTicketRepository
import jodd.http.HttpRequest
import jodd.http.HttpStatus
import jodd.json.JsonParser
import jodd.json.JsonSerializer
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/async-test")
class AsyncTestController(
    private val grispiApi: GrispiApi,
    private val zendeskTicketRepository: ZendeskTicketRepository,
    private val zendeskTicketCommentRepository: ZendeskTicketCommentRepository,
    private val zendeskMappingQueryRepository: ZendeskMappingQueryRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        val grispiApiCredentials = GrispiApiCredentials("bugfix-db2", "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJidWdmaXgtZGIyIiwic3ViIjoiZXJzYW5AZ3JyLmxhIiwiZGV2Ijp0cnVlLCJyb2xlcyI6WyJST0xFX0FETUlOIiwiUk9MRV9QTFVHSU4iXSwiaXNzIjoiZ3Jpc3BpLWFwaTo3MzcyNTU0OTYiLCJwbGciOiJjb20uZ3Jpc3BpLmNhbGwiLCJleHAiOjE3NDIwMjIxNzUsImlhdCI6MTY0MjAxODU3NSwianRpIjoiOTQ1NzhmZTAtN2EyMC00YmVhLWI3N2MtNzE5OTJiYjQwNDQ3In0.NeY5E8TJMMRQtvvdH60srHok53iRrm3wHYHyRoV5swU")
    }

    @GetMapping("/try-get")
    fun testGetAsync() {
        HttpRequest.get("http://localhost:8088/async-test/get")
            .query("param", "request_1")
            .sendAsync().thenApply { res -> logger.info(res.bodyText()) }
        logger.info("request 1 sent")

        HttpRequest.get("http://localhost:8088/async-test/get")
            .query("param", "request_2")
            .sendAsync().thenApply { res -> logger.info(res.bodyText()) }
        logger.info("request 2 sent")

        HttpRequest.get("http://localhost:8088/async-test/get")
            .query("param", "request_3")
            .sendAsync().thenApply { res -> logger.info(res.bodyText()) }
        logger.info("request 3 sent")
    }

    @GetMapping("/try-grispi")
    fun testPostGrispiAsync() {

        val operationId = "45aa9bfa-de2a-47e0-a2f9-0dea0b14f63d"

        val commentedTicketIds = zendeskTicketRepository.findCommentedTicketIds(operationId)

        val groupedComments = zendeskTicketCommentRepository
            .findAllByOperationIdAndTicketIdIsIn(operationId, commentedTicketIds.stream().map { it.id }.toList())
            .groupBy { it.ticketId }

        val requests: MutableList<CompletableFuture<Unit>> = mutableListOf()

        for (ticket in groupedComments.entries) {

            val toList = ticket.value.stream()
                .map {
                    it.toCommentRequest(zendeskMappingQueryRepository::findGrispiTicketKey,
                        zendeskMappingQueryRepository::findGrispiUserId)
                }
                .toList()

            val thenApply = HttpRequest
                .post("${GrispiApi.HOST}${GrispiApi.TICKET_ENDPOINT}/TICKET-45471${GrispiApi.TICKET_COMMENT_ENDPOINT}")
                .bodyText("", "")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${grispiApiCredentials.token}")
                .header(GrispiApi.TENANT_ID_HEADER_NAME, grispiApiCredentials.tenantId)
                .timeout(10000)
                .sendAsync()
                .thenApply { response -> logger.info("response: ${response.bodyText()} operation: $operationId") }

            logger.info("${ticket.key} is requested")

            requests.add(thenApply)
        }

        CompletableFuture.allOf(*requests.toTypedArray()).get()
    }

    @GetMapping("/try-post")
    fun testPostAsync() {
        val bodyText = JsonSerializer().deep(true).serialize(AsyncTestForm("request_post_1"))
        val bodyText2 = JsonSerializer().deep(true).serialize(AsyncTestForm("request_post_2"))
        val bodyText3 = JsonSerializer().deep(true).serialize(AsyncTestForm("request_post_3"))

        val requests: MutableList<CompletableFuture<Unit>> = mutableListOf()

        for (i in 1..300) {
            val req1 = HttpRequest.post("http://localhost:8088/async-test/post")
                .bodyText(bodyText, "")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .sendAsync()
                .thenApply { res -> logger.info(res.bodyText()) }
            logger.info("post request $i sent")

            requests.add(req1)
        }

        CompletableFuture.allOf(*requests.toTypedArray()).get()
    }

    @GetMapping("/get")
    fun get(@RequestParam param: String?): String {
        Thread.sleep(1000)
        return param.toString()
    }

    @PostMapping("/post")
    fun post(@RequestBody asyncTestForm: AsyncTestForm): String {
        Thread.sleep(3000)
        return asyncTestForm.value
    }

}

data class AsyncTestForm(val value: String)

