package com.grispi.grispiimport

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory

// TODO: 15.12.2021 RESOURCE_NAMES
@SpringBootApplication
@EnableMongoRepositories
class GrispiImportApplication: CommandLineRunner {

	private val logger = LoggerFactory.getLogger(javaClass)

	@Bean
	fun jedisConnectionFactory(): JedisConnectionFactory? {
		return JedisConnectionFactory()
	}

	override fun run(vararg args: String?) {
		logger.debug("debug log is like that")
		logger.info("info log is like that")
		logger.warn("warn log is like that")
		logger.error("error log is like that")
	}

}

fun main(args: Array<String>) {
	runApplication<GrispiImportApplication>(*args)
}