package com.grispi.grispiimport.zendesk

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class ApiLimitWatcher(private val jobMap: MutableMap<String, Long> = ConcurrentHashMap()) {

    fun limitExceededFor(operationId: String, retryAfterInSec: Long) {
        jobMap.putIfAbsent(operationId, retryAfterInSec)
    }

    fun getRetryAfterFor(operationId: String): Long {
        return jobMap.get(operationId)!!
    }

    fun resetLimitFor(operationId: String) {
        jobMap.remove(operationId)
    }

    fun isApiAvailable(operationId: String): Boolean {
        return !isApiUnavailable(operationId)
    }

    fun isApiUnavailable(operationId: String): Boolean {
        return jobMap.containsKey(operationId)
    }

}