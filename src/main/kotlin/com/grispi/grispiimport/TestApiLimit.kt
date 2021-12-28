package com.grispi.grispiimport

import com.grispi.grispiimport.zendesk.*
import jodd.http.HttpRequest
import jodd.json.JsonParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit


@Service
class TestApiLimit(
    @Autowired val apiLimitWatcher: ApiLimitWatcher,
) {

    companion object {
        const val OPERATION_ID = "mock-test"
    }

    fun test() {
//        CompletableFuture.supplyAsync { importTickets() }
        CompletableFuture.supplyAsync { importUsers(0) }
    }

    fun import() {
        importUsers(0)
//        importTickets()
    }

    private fun importUsers(x: Int) {

        for (i in x..1000) {
            if (apiLimitWatcher.isApiUnavailable(OPERATION_ID)) {
                val retryAfterFor = apiLimitWatcher.getRetryAfterFor(OPERATION_ID)
                println("sleeping user thread for ${retryAfterFor} page ${i}")
                CompletableFuture.supplyAsync(
                    { importUsers(i) },
                    CompletableFuture.delayedExecutor(retryAfterFor, TimeUnit.SECONDS)
                )

                break;
            }

            println("importing page ${i}")
            getUsers(ZendeskPageParams(i, 1))
                .thenApply { users -> users.stream().map { it.operationId = OPERATION_ID } }

//                .thenApply { users -> println("users fetched ${users.count()}") }
        }
    }

    private fun importTickets() {
        for (i in 1..4) {
            if (apiLimitWatcher.isApiUnavailable(OPERATION_ID)) {
                val retryAfterFor = apiLimitWatcher.getRetryAfterFor(OPERATION_ID)
                println("sleeping ticket thread for ${retryAfterFor}")
                Thread.sleep(retryAfterFor)
            }

            getTickets(ZendeskPageParams(i, 1))
                .thenApply { tickets -> tickets.stream().map { it.operationId = OPERATION_ID } }
        }
    }

    fun getUsers(zendeskPageParams: ZendeskPageParams): CompletableFuture<List<ZendeskUser>> {
        println("getUser() invoked for page: ${zendeskPageParams.page}... ${LocalDateTime.now()}")
        return HttpRequest
            .get("http://localhost:8086/users")
            .query("page", zendeskPageParams.page).query("per_page", zendeskPageParams.perPage)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .sendAsync()
            .thenApply { response ->
                val header = response.header("Retry-After")
                if (StringUtils.hasText(header)) {
                    println("retry after header is ${header} for users page ${zendeskPageParams.page}")
                    apiLimitWatcher.limitExceededFor(OPERATION_ID, header.toLong())
                    ZendeskApi.scheduledExecutorService.schedule(
                        {
                            println("waited for ${header.toLong()} seconds for users page: {${zendeskPageParams.page}}... reseting limit")
                            apiLimitWatcher.resetLimitFor(OPERATION_ID)
                            getUsers(ZendeskPageParams(zendeskPageParams.page-1, 1))
                        }, header.toLong(), TimeUnit.SECONDS)
                }

                JsonParser()
                    .parse(response.bodyText(), ZendeskUsers::class.java)
                    .users
            }
    }

    fun getTickets(zendeskPageParams: ZendeskPageParams): CompletableFuture<List<ZendeskTicket>> {
        println("getTickets() invoked for page: ${zendeskPageParams.page}... ${LocalDateTime.now()}")
        return HttpRequest
            .get("http://localhost:8086/tickets")
            .query("page", zendeskPageParams.page).query("per_page", zendeskPageParams.perPage)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .sendAsync()
            .thenApply { response ->
                val header = response.header("Retry-After")
                println(header)
                if (StringUtils.hasText(header)) {
                    println("retry after header is ${header} for tickets page ${zendeskPageParams.page} at: ${LocalDateTime.now()}")
                    apiLimitWatcher.limitExceededFor(OPERATION_ID, header.toLong())
                    println(apiLimitWatcher.isApiUnavailable(OPERATION_ID).toString().uppercase())
                    ZendeskApi.scheduledExecutorService.schedule(
                        {
                            println("waited for ${header.toLong()} seconds for tickets... reseting limit")
                            apiLimitWatcher.resetLimitFor(OPERATION_ID)
                            getTickets(ZendeskPageParams(1, 1))
                        }, header.toLong(), TimeUnit.SECONDS)
                }

                JsonParser()
                    .parse(response.bodyText(), ZendeskTickets::class.java)
                    .tickets
            }
    }

}