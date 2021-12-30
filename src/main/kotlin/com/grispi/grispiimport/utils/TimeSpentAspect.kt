package com.grispi.grispiimport.utils

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("dev")
@Component
@Aspect
class TimeSpentAspect {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Around("@annotation(com.grispi.grispiimport.utils.CalculateTimeSpent)")
    fun calculateSpentTime(joinPoint: ProceedingJoinPoint) {
        val started = System.currentTimeMillis()
        joinPoint.proceed()
        val timeSpent = System.currentTimeMillis() - started
        logger.info("$joinPoint method execution took: ${humanize(timeSpent)}")
    }

    private fun humanize(timeSpent: Long): String {
        return when(timeSpent) {
            in 0..1000 -> "${timeSpent}ms"
            in 1000..60000 -> "${timeSpent}sec"
            in 60000..3600000 -> "${timeSpent}min"
            in 3600000..86400000 -> "${timeSpent}h"
            else -> "${timeSpent}day???"
        }
    }

}