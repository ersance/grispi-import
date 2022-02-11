package com.grispi.grispiimport.grispi

class GrispiTicketFormRequest(
    val name: String?,
    val description: String?,
    val permission: GrispiTicketFormPermission,
    val fields: List<String>
): GrispiApiRequest()

enum class GrispiTicketFormPermission {
    END_USER_AND_AGENT, END_USER_ONLY, AGENT_ONLY
}