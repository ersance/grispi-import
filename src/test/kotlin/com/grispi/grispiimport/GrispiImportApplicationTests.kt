package com.grispi.grispiimport

import com.grispi.grispiimport.grispi.*
import jodd.http.HttpRequest
import jodd.json.JsonSerializer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import java.util.List

@SpringBootTest
class GrispiImportApplicationTests(@Autowired val testBuddy: TestBuddy) {

	@Test
	fun createCustomFields() {
		val grispiTicketField = GrispiTicketField.Builder()
			.key("ticket.custom.a_multi_sel")
			.name("A multi sel")
			.type(FieldType.MULTI_SELECT)
			.descriptionForAgents("desc for agents")
			.descriptionForEndUsers("desc for end user")
			.titleForAgents("title agent")
			.titleForEndUsers("title user")
			.required(RequiredStatus.REQUIRED_TO_SUBMIT)
			.permission(FieldPermissions.EDITABLE_BY_END_USERS)
			.attributes(List.of("ALLOW_NEW_VALUES"))
			.options(List.of(GrispiTicketFieldOption(1, "op_1", "zaa"), GrispiTicketFieldOption(2, "op_2", "aaa")))
			.build()

		val httpRequest = HttpRequest
			.post("http://grispi.com:8080/fields")
			.body(grispiTicketField.toJson())
			.defaultHeaders(testBuddy)

		val httpResponse = httpRequest.send()

		assertEquals(201, httpResponse.statusCode())
	}

	@Test
	fun createAdmin() {
		val adminRequest = UserRequest.Builder()
			.email("erso@grr.la")
			.password("S3cret!!")
			.phone("+905556668811")
			.fullName("Ersan")
			.role(Role.ADMIN)
			.build()

		val httpRequest = HttpRequest.post("http://grispi.com:8080/admins")
			.body(adminRequest.toJson())
			.defaultHeaders(testBuddy)

		val httpResponse = httpRequest.send()

		assertEquals(201, httpResponse.statusCode())
	}

	@Test
	fun createTicket() {
		val userId = "1"

		val request = TicketRequest.Builder()
			.assignee(TestBuddy.DEFAULT_GROUP_ID.toString(), userId)
			.subject("Yalandan ticket")
			.comment(TicketRequest.Comment_("verin parami diyor", true, TicketRequest.User_(userId, "tokatci@tokat.com", "+905551231212")))
			.status(TicketStatus.NEW)
			.channel(TicketChannel.WEB)
			.customField("ticket.custom.a_multi_select", "select yaziyor ama text")
			.build()

		assertEquals(TicketChannel.WEB, request.channel)

		val httpRequest = HttpRequest.post("http://grispi.com:8080/tickets?ui=true")
			.bodyText(request.toJson(), MediaType.APPLICATION_JSON_VALUE)
			.defaultHeaders(testBuddy)

		val httpResponse = httpRequest.send()

		assertEquals(201, httpResponse.statusCode())
	}

}
