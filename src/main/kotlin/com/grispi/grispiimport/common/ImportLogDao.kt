package com.grispi.grispiimport.common

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Repository
import org.springframework.web.context.WebApplicationContext

@Repository
class ImportLogDao {

    private val errorLogs: MutableList<ImportErrorLog> = mutableListOf()
    private val successLogs: MutableList<ImportSuccessLog> = mutableListOf()
    private val infoLogs: MutableList<ImportInfoLog> = mutableListOf()

    fun errorLog(resourceName: String, message: String, externalId: Long?) {
        errorLogs.add(ImportErrorLog(resourceName, message, externalId))
    }

    fun successLog(resourceName: String?, message: String, externalId: Long?) {
        successLogs.add(ImportSuccessLog(resourceName, message, externalId))
    }

    fun infoLog(resourceName: String?, message: String, externalId: Long?) {
        infoLogs.add(ImportInfoLog(resourceName, message, externalId))
    }

    fun getAllLogs(): ImportLogContainer {
        return ImportLogContainer(successLogs, errorLogs, infoLogs)
    }

}

class ImportLogContainer(val successLogs: List<ImportLog>, val errorLogs: List<ImportLog>, val infoLogs: List<ImportLog>) {
}

