package com.grispi.grispiimport.common

abstract class ImportLog {
    val createdAt: Long = System.currentTimeMillis()
}

// resourceName: (user, organization, customField, group, ticket)
// externalId: (zendeskId, freshDeskId)
class ImportErrorLog(val resourceName: String, val message: String, val externalId: Long? = null): ImportLog()

class ImportSuccessLog(val resourceName: String?, val message: String, val externalId: Long? = null): ImportLog()

class ImportInfoLog(val resourceName: String?, val message: String, val externalId: Long? = null): ImportLog()
