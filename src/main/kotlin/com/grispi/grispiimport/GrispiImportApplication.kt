package com.grispi.grispiimport

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory

// TODO: 15.12.2021 RESOURCE_NAMES
// TODO: 15.12.2021 INFO AND WARNING LOGS
@SpringBootApplication
@EnableMongoRepositories
class GrispiImportApplication: CommandLineRunner {

	@Bean
	fun jedisConnectionFactory(): JedisConnectionFactory? {
		return JedisConnectionFactory()
	}

	override fun run(vararg args: String?) {
	}

}

fun main(args: Array<String>) {
	runApplication<GrispiImportApplication>(*args)
}