package com.grispi.grispiimport.grispi

class GrispiOrganizationRequest(
    val name: String,
    val details: String?,
    val notes: String?,
    val tags: Set<String>?,
): GrispiApiRequest() {

}
