package com.grispi.grispiimport.grispi

import java.math.BigDecimal
import java.time.Instant

enum class FieldType(val htmlType: String, val javaType: Class<*>) {

    TEXT("text", String.javaClass),

    HTML("html", String.javaClass),

    NUMBER_INTEGER("number", Int.javaClass),

    NUMBER_DECIMAL("number", BigDecimal::class.java),

    DATE("date", Instant::class.java),

    DATE_TIME("datetime-local", Instant::class.java),

    SELECT("select", String.javaClass),

    MULTI_SELECT("multi-select", LinkedHashSet::class.java),

    TAG("tag", LinkedHashSet::class.java), // LinkedHashSet<String>

    ASSIGNEE("assignee", Assignee::class.java),

    USER("user", User::class.java),

    MULTI_USER("multi-user", LinkedHashSet::class.java)
}