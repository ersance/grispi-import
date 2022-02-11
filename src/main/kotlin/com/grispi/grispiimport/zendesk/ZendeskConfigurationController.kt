package com.grispi.grispiimport.zendesk

import com.grispi.grispiimport.grispi.GrispiApiCredentials
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*

@Controller
class ZendeskConfigurationController(
    private val zendeskImportService: ZendeskImportService,
    private val zendeskImportRepository: ZendeskImportRepository
) {

    @GetMapping("/")
    fun blog(model: Model, @RequestParam tenantId: String, @RequestParam token: String): String {
        model["grispiApiCredentials"] = GrispiApiCredentials(tenantId, token)
        return "config"
    }

}
