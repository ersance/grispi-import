package com.grispi.grispiimport

import com.grispi.grispiimport.common.ImportLog
import com.grispi.grispiimport.grispi.*
import com.grispi.grispiimport.zendesk.*
import jodd.http.HttpRequest
import jodd.json.JsonParser
import jodd.json.ValueConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.stream.Collectors


@RestController
class ZendeskApiController(
  @Autowired val zendeskApi: ZendeskApi,
  @Autowired val grispiApi: GrispiApi,
  @Autowired val zendeskMappingDao: ZendeskMappingDao,
  @Autowired val logTemplate: RedisTemplate<String, ImportLog>,
) {

  companion object {
    val apiCredentials =
      ZendeskApiCredentials("dugunbuketi", "bilgehan@dugunbuketi.com", "BApUrJvH7zpa8obvOFRE3LCuRUuVbMRrSHTRrnop")
  }

  @GetMapping("/getir")
  fun getir(@RequestParam("tenant") tenantId: String): List<ZendeskOrganization> {
    val organizations = zendeskApi.getOrganizations(apiCredentials, ZendeskPageParams(1, 100))
    return organizations
  }

//  @PostMapping("/comments")
//  fun getir(@RequestBody zendeskImportRequest: ZendeskImportRequest): List<CommentRequest> {
//    val ticketComments = zendeskApi.getTicketComments(9L, zendeskImportRequest.zendeskApiCredentials)
//    return ticketComments.map { it.toCommentRequest() }
//  }

}