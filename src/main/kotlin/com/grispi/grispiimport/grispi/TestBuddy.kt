package com.grispi.grispiimport.grispi

import com.grispi.grispiimport.grispi.TestBuddy.Companion.USER_AGENT
import jodd.http.HttpRequest
import jodd.http.HttpResponse
import jodd.http.HttpStatus
import jodd.json.JsonArray
import jodd.json.JsonObject
import jodd.json.JsonParser
import jodd.net.HttpMethod
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component

@Component
class TestBuddy {

    private val tenantId: String = DEFAULT_TEST_TENANT_ID

    fun tenantId(): String {
        return tenantId
    }
//
//    fun tenantIdAsObject(): TenantId {
//        return TenantId(tenantId())
//    }

    /**
     * if tenantId is blank than schema is TenantId.DEFAULT_TENANT_ID.schema
     */
//    private fun schemaName(): String {
//        return if (tenantId().isBlank()) TenantId.DEFAULT_TENANT_ID.schema else TenantId(
//            tenantId()
//        ).schema
//    }

    /**
     * ROLE_AGENT
     */
    fun token(): String {
        return "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJpbXBvcnQtdGVzdCIsInN1YiI6ImVyc2FuQGdyci5sYSIsImRldiI6dHJ1ZSwicm9sZXMiOlsiUk9MRV9BR0VOVCIsIlJPTEVfQURNSU4iXSwiaXNzIjoiZ3Jpc3BpLWFwaTotMTk2NzIxMzU5OSIsImV4cCI6MTYzNzg4NTAxMCwiaWF0IjoxNjM3ODgxNDEwLCJqdGkiOiIwN2QzYzIxYS02NzQwLTQzNjMtOTQ3ZC1mZDM3OGQ2MGYyNDQifQ.dQLHcGJNmtDc-4XDtumcfl1iJ0ZwhCtvCMaw0PA-AMY"
    }

    fun adminToken(): String {
        return "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJkZW1vLWFjY291bnQiLCJzdWIiOiJkZW5lbWVAZ3JyLmxhIiwibmJmIjoxNjE5MTI0MDQwLCJkZXYiOnRydWUsInJvbGVzIjpbIlJPTEVfQURNSU4iXSwiaXNzIjoicGFsbWRhLWFwaS0tMTg4NjEyNDYyNiIsImV4cCI6MTcyOTEyNzY0MCwiaWF0IjoxNjE5MTI0MDQwLCJqdGkiOiIzYzY0MTJkMi1lMjZjLTQwZWUtYTkyMC05YWNmMDRlZGM2OGQifQ.B8pDFLUVsgocCSMyzcvdy5d7B2Luil8If0lY0jhotfs"
    }

//    fun callWithoutTenantIdHeader(url: String, method: HttpMethod = HttpMethod.GET, token: String = token()) {
//
//        val response = HttpRequest()
//            .method(method)
//            .set(url)
//            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
//            .header(HttpHeaders.USER_AGENT, USER_AGENT)
//            .send()
//
//        response.statusCode() shouldBe HttpStatus.HTTP_BAD_REQUEST
//        response.statusPhrase() shouldBe ExceptionCodes.TENANT_ID_HEADER_MISSING
//    }

//    fun callWithoutAuthorizationHeader(url: String, method: HttpMethod = HttpMethod.POST) {
//        val response = HttpRequest()
//            .method(method)
//            .set(url)
//            .header(tenantIdHeaderName(), tenantId())
//            .header(HttpHeaders.USER_AGENT, USER_AGENT)
//            .send()
//
//        response.statusCode() shouldBe HttpStatus.HTTP_UNAUTHORIZED
//        response.statusPhrase() shouldBe "Unauthorized"
//    }

    companion object {
        const val DEFAULT_TEST_TENANT_ID = "import-test"
        const val USER_AGENT = "import-test-agent"
        const val DEFAULT_GROUP_ID = 1L
        const val TEST_USER_EMAIL = "deneme@grr.la"
        const val DEFAULT_GROUP_NAME = "Default Group"
        const val DEFAULT_TEST_PASSWORD = "S3cret!!"

        val JSON_PARSER = JsonParser()
    }

}

// Extension function for Jodd's HttpRequest
//fun jodd.http.HttpRequest.defaultHeaders(testBuddy: TestBuddy, isAdmin: Boolean = false): jodd.http.HttpRequest {
//    val token = if (isAdmin) testBuddy.adminToken() else testBuddy.token()
//    return this.defaultHeaders(testBuddy, token)
//}

fun jodd.http.HttpRequest.defaultHeaders(testBuddy: TestBuddy): jodd.http.HttpRequest {
    return this
        .header(HttpHeaders.AUTHORIZATION, "Bearer ${testBuddy.token()}")
        .header("tenantId", testBuddy.tenantId())
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
}

// Extension function for Jodd's JsonArray
fun jodd.json.JsonArray.extractTicketKeys(): List<String> {
    val keys = mutableListOf<String>()
    for ((index, value) in this.withIndex()) {
        keys.add(this.getJsonObject(index).getString("key"))
    }
    return keys
}

fun jodd.json.JsonArray.filterForObject(key: String, value: String?): Map<String, Any>? {
    return this.stream().filter { (it as Map<String, Any>)[key] == value }.findFirst().orElse(null) as Map<String, Any>?
}

// Extension function for Jodd's HttpResponse
fun jodd.http.HttpResponse.errorMessageInBody(): String? {
    return this.toJsonObject().getString("message")
}

fun jodd.http.HttpResponse.toJsonObject(): JsonObject {
    return TestBuddy.JSON_PARSER.parseAsJsonObject(this.bodyRaw())
}

fun jodd.http.HttpResponse.toJsonArray(): JsonArray {
    return TestBuddy.JSON_PARSER.parseAsJsonArray(this.bodyRaw())
}