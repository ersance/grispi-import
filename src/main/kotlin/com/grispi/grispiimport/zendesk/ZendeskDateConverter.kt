package com.grispi.grispiimport.zendesk

import jodd.json.ValueConverter
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import java.time.Instant

@Component
class ZendeskDateConverter : ValueConverter<String, Instant> {

    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");

    override fun convert(source: String?): Instant {
        try {
            return dateFormat.parse(source).toInstant()
        } catch (ex: RuntimeException) {
            return Instant.now()
        }
    }

}