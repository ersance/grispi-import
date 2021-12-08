package com.grispi.grispiimport

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.grispi.grispiimport.common.ImportLog
import com.grispi.grispiimport.zendesk.TicketImportService
import com.grispi.grispiimport.zendesk.ZendeskApi
import jodd.http.HttpRequest
import jodd.json.JsonParser
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.GenericToStringSerializer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.ReflectionUtils
import java.util.concurrent.CompletableFuture


@SpringBootApplication
class GrispiImportApplication: CommandLineRunner {

	@Bean
	fun jedisConnectionFactory(): JedisConnectionFactory? {
		return JedisConnectionFactory()
	}

	@Bean("zendeskMappingTemplate")
	fun redisTemplate(): RedisTemplate<String, Any> {
		val template = RedisTemplate<String, Any>()
		template.setConnectionFactory(jedisConnectionFactory()!!)
		template.valueSerializer = GenericToStringSerializer(Any::class.java)
		return template
	}

	@Bean("logTemplate")
	fun logTemplate(): RedisTemplate<String, ImportLog> {
		val template = RedisTemplate<String, ImportLog>()
		template.setConnectionFactory(jedisConnectionFactory()!!)
		template.valueSerializer = Jackson2JsonRedisSerializer(ImportLog::class.java)
		val declaredField = Jackson2JsonRedisSerializer::class.java.getDeclaredField("objectMapper")
		declaredField.isAccessible = true
		val objectMapper = declaredField.get(template.valueSerializer) as ObjectMapper
		objectMapper.registerModule(KotlinModule())
		return template
	}

	override fun run(vararg args: String?) {
	}

}

fun main(args: Array<String>) {
	runApplication<GrispiImportApplication>(*args)
}