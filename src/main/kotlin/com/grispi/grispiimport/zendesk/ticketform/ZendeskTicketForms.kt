package com.grispi.grispiimport.zendesk.ticketform

import com.grispi.grispiimport.grispi.GrispiTicketFormPermission
import com.grispi.grispiimport.grispi.GrispiTicketFormRequest
import com.grispi.grispiimport.zendesk.ZendeskEntity
import jodd.json.meta.JSON
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.stream.Collectors
import kotlin.reflect.KFunction1

class ZendeskTicketForms {

    @JSON(name = "ticket_forms")
    var ticketForms: List<ZendeskTicketForm> = emptyList()

}

@Document
class ZendeskTicketForm: ZendeskEntity() {

    @Id
    @JSON(name = "id")
    var id: Long = -1

    @JSON(name = "active")
    var active: Boolean = false

    @JSON(name = "created_at")
    var createdAt: Instant = Instant.now()

    @JSON(name = "default")
    var isDefault: Boolean = false

    @JSON(name = "display_name")
    var displayName: String = ""

    @JSON(name = "end_user_conditions")
    var endUserConditions: List<EndUserConditions> = mutableListOf()

    @JSON(name = "agent_conditions")
    var agentConditions: List<AgentConditions> = mutableListOf()

    @JSON(name = "end_user_visible")
    var endUserVisible: Boolean = false

    @JSON(name = "in_all_brands")
    var inAllBrands: Boolean = false

    @JSON(name = "name")
    var name: String = ""

    @JSON(name = "position")
    var position: Int? = 0

    @JSON(name = "raw_display_name")
    var rawDisplayName: String = ""

    @JSON(name = "raw_name")
    var rawName: String = ""

    @JSON(name = "restricted_brand_ids")
    var restrictedBrandIds: Set<Long> = mutableSetOf()

    @JSON(name = "ticket_field_ids")
    var ticketFieldIds: List<Long> = mutableListOf()

    fun toGrispiTicketFormRequest(getGrispiTicketFieldKey: (Long) -> String?): GrispiTicketFormRequest {
        val ticketFormPermission = mapPermission()
        val grispiTicketFieldIds = ticketFieldIds.stream().map {
            getGrispiTicketFieldKey.invoke(it) ?: "tiz.${it}"
        }.toList()

        return GrispiTicketFormRequest(name, displayName, ticketFormPermission, grispiTicketFieldIds)
    }

    private fun mapPermission(): GrispiTicketFormPermission {
        if (endUserVisible) {
            return GrispiTicketFormPermission.END_USER_AND_AGENT
        }
        else {
            return GrispiTicketFormPermission.AGENT_ONLY
        }
    }

}

class AgentConditions {

    @JSON(name = "parent_field_id")
    var parentFieldId: Long? = -1

    @JSON(name = "parent_field_id")
    var value: String? = ""

    @JSON(name = "child_fields")
    var childFields: List<ConditionChildField> = mutableListOf()

    class ConditionChildField {
        @JSON(name = "id")
        var id: Long? = -1

        @JSON(name = "is_required")
        var isRequired: Boolean? = false

        @JSON(name = "required_on_statuses")
        var requiredOnStatuses: RequiredOnStatus? = null

        class RequiredOnStatus {
            @JSON(name = "statuses")
            var statuses: Set<String> = mutableSetOf()

            @JSON(name = "type")
            var type: String? = ""
        }
    }

}

class EndUserConditions {

    @JSON(name = "parent_field_id")
    var parentFieldId: Long? = -1

    @JSON(name = "parent_field_id")
    var value: String? = ""

    @JSON(name = "child_fields")
    var childFields: List<ConditionChildField> = mutableListOf()

    class ConditionChildField {
        @JSON(name = "id")
        var id: Long? = -1

        @JSON(name = "is_required")
        var isRequired: Boolean? = false
    }

}