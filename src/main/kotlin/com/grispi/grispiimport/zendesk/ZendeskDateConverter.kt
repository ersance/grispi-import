package com.grispi.grispiimport.zendesk

import jodd.json.ValueConverter
import org.apache.commons.lang3.time.FastDateFormat
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class ZendeskDateConverter : ValueConverter<String, Instant> {

//    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss")

    private val dateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'hh:mm:ss")

    override fun convert(source: String?): Instant {
        try {
            return dateFormat.parse(source).toInstant()
        } catch (ex: RuntimeException) {
            return Instant.now()
        }
    }

}